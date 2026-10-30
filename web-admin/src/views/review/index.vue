<template>
  <el-card>
    <div class="toolbar">
      <el-radio-group v-model="q.status" @change="load">
        <el-radio-button :value="1">待审核</el-radio-button>
        <el-radio-button :value="2">审核退回</el-radio-button>
        <el-radio-button :value="3">已归档</el-radio-button>
      </el-radio-group>
      <el-input v-model="q.keyword" placeholder="题名关键词" clearable style="width: 220px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="archiveId" label="档案编号" width="220">
        <template #default="{ row }">{{ row.archiveId || '—' }}</template>
      </el-table-column>
      <el-table-column prop="assocCode" label="协会" width="90" />
      <el-table-column prop="title" label="题名" min-width="200" show-overflow-tooltip />
      <el-table-column prop="majorCode" label="大类" width="70" />
      <el-table-column prop="archiveYear" label="年度" width="80" />
      <el-table-column prop="submitAt" label="提交时间" width="160" />
      <el-table-column label="操作" :width="q.status === 1 ? 280 : 100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          <template v-if="q.status === 1">
            <el-button type="success" size="small" @click="openReview(row, true)">通过并归档</el-button>
            <el-button type="danger" size="small" @click="openReview(row, false)">退回</el-button>
          </template>
        </template>
      </el-table-column>
      <el-table-column v-if="q.status === 2" label="退回意见" min-width="180">
        <template #default="{ row }">{{ row.rejectReason || '—' }}</template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 12px" layout="total, prev, pager, next"
      :total="total" :page-size="q.size" v-model:current-page="q.page" @current-change="load" />

    <el-dialog v-model="dialog" :title="pass ? '确认通过并归档' : '退回修改'" width="460px">
      <el-input v-model="opinion" type="textarea" :rows="4"
        :placeholder="pass ? '审核意见（可选）' : '退回原因（必填，SRS UC-06）'" />
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button :type="pass ? 'success' : 'danger'" :loading="saving" @click="submitReview">确认</el-button>
      </template>
    </el-dialog>

    <ArchiveDetailDialog v-model="detailVisible" :biz-id="detailId" />
  </el-card>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { pageArchive, reviewArchive } from '../../api'
import { useUserStore } from '../../store/user'
import ArchiveDetailDialog from '../../components/ArchiveDetailDialog.vue'

const user = useUserStore()
if (!user.isAdmin) ElMessage.warning('审核归档功能仅主管部门操作员可用')

const rows = ref([])
const total = ref(0)
const dialog = ref(false)
const saving = ref(false)
const pass = ref(true)
const opinion = ref('')
const current = ref(null)
const detailVisible = ref(false)
const detailId = ref(null)
const q = reactive({ page: 1, size: 20, status: 1, keyword: '' })

function openDetail(row) {
  detailId.value = row.bizId
  detailVisible.value = true
}

async function load() {
  const data = await pageArchive(q)
  rows.value = data.records
  total.value = Number(data.total)
}

function openReview(row, p) {
  current.value = row
  pass.value = p
  opinion.value = ''
  dialog.value = true
}

async function submitReview() {
  if (!pass.value && !opinion.value.trim()) {
    ElMessage.warning('退回必须填写审核意见')
    return
  }
  saving.value = true
  try {
    await reviewArchive({ bizId: current.value.bizId, pass: pass.value, opinion: opinion.value })
    ElMessage.success(pass.value ? '已通过并完成电子归档' : '已退回协会修改')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
</style>
