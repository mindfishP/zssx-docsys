<template>
  <el-card>
    <el-tabs v-model="tab">
      <el-tab-pane v-if="user.isAssoc" label="可借阅档案" name="available">
        <div class="toolbar">
          <el-input v-model="qa.keyword" placeholder="题名关键词" clearable style="width: 220px"
            @keyup.enter="loadAvailable" />
          <el-button type="primary" @click="loadAvailable">查询</el-button>
        </div>

        <el-table :data="available" border stripe>
          <el-table-column prop="archiveId" label="档案编号" width="230">
            <template #default="{ row }">{{ row.archiveId || '—' }}</template>
          </el-table-column>
          <el-table-column prop="title" label="题名" min-width="220" show-overflow-tooltip />
          <el-table-column label="所属协会" width="140">
            <template #default="{ row }">{{ assocName(row.assocCode) }}</template>
          </el-table-column>
          <el-table-column prop="majorCode" label="大类" width="80" />
          <el-table-column prop="archiveYear" label="年度" width="90" />
          <el-table-column label="密级" width="90">
            <template #default>
              <el-tag type="success" size="small">公开</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" @click="openApply(row)">申请借阅</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination style="margin-top: 12px" layout="total, prev, pager, next"
          :total="availTotal" :page-size="qa.size" v-model:current-page="qa.page"
          @current-change="loadAvailable" />
      </el-tab-pane>

      <el-tab-pane :label="user.isAssoc ? '借阅记录' : '借阅记录'" name="records">
        <div class="toolbar">
          <el-select v-model="q.status" placeholder="借阅状态" clearable style="width: 150px" @change="load">
            <el-option label="待审批" :value="0" /><el-option label="借阅中" :value="1" />
            <el-option label="已归还" :value="2" />
            <el-option label="已拒绝" :value="4" />
          </el-select>
          <el-button type="primary" @click="load">查询</el-button>
        </div>

        <el-table :data="rows" border stripe>
          <el-table-column prop="borrowId" label="编号" width="80" />
          <el-table-column prop="archiveId" label="档案编号" width="220" />
          <el-table-column prop="applicantAssoc" label="申请协会" width="100" />
          <el-table-column prop="reason" label="借阅事由" min-width="180" show-overflow-tooltip />
          <el-table-column prop="dueAt" label="应还时间" width="160" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="statusType(row.borrowStatus)">{{ statusText(row.borrowStatus) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="240" fixed="right">
            <template #default="{ row }">
              <el-button v-if="user.isAssoc && row.borrowStatus === 1" link type="primary"
                @click="goArchive(row)">查看档案</el-button>
              <el-button v-if="user.isAdmin && row.borrowStatus === 0" link type="success"
                @click="doApprove(row, true)">通过</el-button>
              <el-button v-if="user.isAdmin && row.borrowStatus === 0" link type="danger"
                @click="doApprove(row, false)">拒绝</el-button>
              <el-button v-if="user.isAdmin && row.borrowStatus === 1" link type="primary"
                @click="doReturn(row)">登记归还</el-button>
            </template>
          </el-table-column>
        </el-table>

        <el-pagination style="margin-top: 12px" layout="total, prev, pager, next"
          :total="total" :page-size="q.size" v-model:current-page="q.page" @current-change="load" />
      </el-tab-pane>
    </el-tabs>

    <!-- 申请借阅 -->
    <el-dialog v-model="applyDialog" title="申请借阅" width="520px">
      <el-descriptions v-if="current" :column="1" border size="small" style="margin-bottom: 12px">
        <el-descriptions-item label="档案编号">{{ current.archiveId || '—' }}</el-descriptions-item>
        <el-descriptions-item label="题名">{{ current.title }}</el-descriptions-item>
        <el-descriptions-item label="所属协会">{{ assocName(current.assocCode) }}</el-descriptions-item>
      </el-descriptions>
      <el-form :model="form" label-width="90px">
        <el-form-item label="借阅事由" required>
          <el-input v-model="form.reason" type="textarea" :rows="3" maxlength="200" show-word-limit
            placeholder="如：参考兄弟协会活动组织与会员服务材料" />
        </el-form-item>
        <el-form-item label="应还时间" required>
          <el-date-picker v-model="form.dueAt" type="datetime" value-format="YYYY-MM-DDTHH:mm:ss"
            placeholder="选择应归还时间" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="applyDialog = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitApply">提交申请</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  pageBorrow, approveBorrow, returnBorrow, borrowableArchives, applyBorrow, allAssociation
} from '../../api'
import { useUserStore } from '../../store/user'

const router = useRouter()
const user = useUserStore()
const tab = ref(user.isAssoc ? 'available' : 'records')

const rows = ref([])
const total = ref(0)
const q = reactive({ page: 1, size: 20, status: null })

const available = ref([])
const availTotal = ref(0)
const qa = reactive({ page: 1, size: 20, keyword: '' })

const applyDialog = ref(false)
const submitting = ref(false)
const current = ref(null)
const form = reactive({ reason: '', dueAt: '' })
const associations = ref([])

const statusText = (s) => ({ 0: '待审批', 1: '借阅中', 2: '已归还', 4: '已拒绝' }[s] || s)
const statusType = (s) => ({ 0: 'warning', 1: 'primary', 2: 'success', 4: 'info' }[s] || 'info')
const assocName = (code) => {
  const a = associations.value.find((x) => x.assocCode === code)
  return a ? a.assocName : code
}

async function load() {
  const data = await pageBorrow(q)
  rows.value = data.records
  total.value = Number(data.total)
}

async function loadAvailable() {
  const data = await borrowableArchives(qa)
  available.value = data.records
  availTotal.value = Number(data.total)
}

function openApply(row) {
  current.value = row
  form.reason = ''
  const due = new Date()
  due.setDate(due.getDate() + 7)
  const pad = (n) => String(n).padStart(2, '0')
  form.dueAt = `${due.getFullYear()}-${pad(due.getMonth() + 1)}-${pad(due.getDate())}T${pad(due.getHours())}:${pad(due.getMinutes())}:00`
  applyDialog.value = true
}

async function submitApply() {
  if (!form.reason.trim()) {
    ElMessage.warning('请填写借阅事由')
    return
  }
  if (!form.dueAt) {
    ElMessage.warning('请选择应还时间')
    return
  }
  submitting.value = true
  try {
    await applyBorrow({ bizId: current.value.bizId, reason: form.reason.trim(), dueAt: form.dueAt })
    ElMessage.success('借阅申请已提交，等待主管部门审批')
    applyDialog.value = false
    loadAvailable()
    load()
  } finally {
    submitting.value = false
  }
}

function goArchive(row) {
  router.push({ path: '/archive/detail', query: { bizId: row.bizId } })
}

async function doApprove(row, pass) {
  await approveBorrow({ borrowId: row.borrowId, pass })
  ElMessage.success(pass ? '已通过，生成限时查阅授权' : '已拒绝')
  load()
}

async function doReturn(row) {
  await returnBorrow(row.borrowId)
  ElMessage.success('已登记归还，留痕封存')
  load()
}

onMounted(async () => {
  associations.value = await allAssociation()
  load()
  if (user.isAssoc) loadAvailable()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; align-items: center; flex-wrap: wrap; }
.tip { color: #909399; font-size: 12px; }
</style>
