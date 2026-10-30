<template>
  <el-card>
    <div class="toolbar">
      <el-button v-if="user.isAdmin" type="primary" @click="openCreate">发起专项补录任务</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="taskCode" label="任务编号" width="130" />
      <el-table-column prop="taskName" label="任务名称" min-width="200" show-overflow-tooltip />
      <el-table-column prop="majorCodes" label="涉及大类" width="110" />
            <el-table-column label="目标范围" min-width="200" show-overflow-tooltip>
        <template #default="{ row }">{{ scopeText(row) }}</template>
      </el-table-column>
      <el-table-column prop="startTime" label="开始时间" width="160" />
      <el-table-column prop="endTime" label="截止时间" width="160" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="{ 1: 'primary', 2: 'info', 0: 'danger' }[row.status]">
            {{ { 1: '进行中', 2: '已结束', 0: '已撤回' }[row.status] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="150" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="goDetail(row)">查看</el-button>
          <el-button v-if="user.isAdmin && row.status === 1" link type="danger"
            @click="withdraw(row)">撤回</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 12px" layout="total, prev, pager, next"
      :total="total" :page-size="q.size" v-model:current-page="q.page" @current-change="load" />

    <el-dialog v-model="dialog" title="发起专项补录任务（SRS UC-05）" width="560px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="任务名称"><el-input v-model="form.taskName" /></el-form-item>
        <el-form-item label="涉及大类">
          <el-select v-model="form.majorCodes" multiple placeholder="留空表示全部大类">
            <el-option v-for="m in majors" :key="m.code" :label="m.name" :value="m.code" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标范围">
          <el-select v-model="form.targetScope" @change="onScopeChange">
            <el-option label="全部协会" value="ALL" />
            <el-option label="按行业" value="INDUSTRY" />
            <el-option label="指定协会" value="LIST" />
          </el-select>
        </el-form-item>

        <el-form-item v-if="form.targetScope === 'LIST'" label="选择协会" required>
          <el-select v-model="form.targets" multiple filterable collapse-tags collapse-tags-tooltip
            placeholder="勾选需要补录的协会，可多选" style="width: 100%">
            <el-option v-for="a in associations" :key="a.assocCode"
              :label="`${a.assocName}（${a.assocCode}）`" :value="a.assocCode" />
          </el-select>
          <div class="tip">已选 {{ form.targets.length }} 家</div>
        </el-form-item>

        <el-form-item v-else-if="form.targetScope === 'INDUSTRY'" label="行业类别" required>
          <el-select v-model="form.targets" multiple filterable allow-create default-first-option
            placeholder="选择或输入行业类别，如 灯饰" style="width: 100%">
            <el-option v-for="t in industries" :key="t" :label="t" :value="t" />
          </el-select>
          <div class="tip">该行业类别下的协会都会收到任务，可多选</div>
        </el-form-item>
        <el-form-item label="起止时间">
          <el-date-picker v-model="range" type="datetimerange" value-format="YYYY-MM-DDTHH:mm:ss" />
        </el-form-item>
        <el-form-item label="任务说明"><el-input v-model="form.taskDesc" type="textarea" :rows="3" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" @click="save">发布任务</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pageTask, createTask, allAssociation } from '../../api'
import request from '../../utils/request'
import { useUserStore } from '../../store/user'

const router = useRouter()
const user = useUserStore()
const rows = ref([])
const total = ref(0)
const dialog = ref(false)
const range = ref([])
const q = reactive({ page: 1, size: 20 })
const form = reactive({ taskName: '', taskDesc: '', majorCodes: [], targetScope: 'ALL', targets: [] })
const associations = ref([])
// 行业类别取自协会主档，支持手动输入新类别
const industries = computed(() => [...new Set(associations.value.map((a) => a.industryType).filter(Boolean))])

const majors = [
  { code: 'A', name: '登记类' }, { code: 'B', name: '治理类' }, { code: 'C', name: '会员类' },
  { code: 'D', name: '活动类' }, { code: 'E', name: '财务类' }, { code: 'F', name: '党建类' }
]

const scopeText = (row) => {
  let targets = []
  try {
    const arr = JSON.parse(row.targetValue || '[]')
    targets = Array.isArray(arr) ? arr : []
  } catch (e) {
    targets = []
  }
  if (row.targetScope === 'ALL') return '全部协会'
  if (row.targetScope === 'INDUSTRY') return `按行业：${targets.join('、') || '—'}`
  if (row.targetScope === 'LIST') return `指定协会 ${targets.length} 家：${targets.join('、')}`
  return row.targetScope || '—'
}

async function load() {
  const data = await pageTask(q)
  rows.value = data.records
  total.value = Number(data.total)
}

function openCreate() {
  form.taskName = ''
  form.taskDesc = ''
  form.majorCodes = []
  form.targetScope = 'ALL'
  form.targets = []
  range.value = []
  dialog.value = true
}

// 切换范围时清空已选目标，避免残留上一次的选择
function onScopeChange() {
  form.targets = []
}

async function save() {
  if (!form.taskName.trim()) {
    ElMessage.warning('请填写任务名称')
    return
  }
  if (form.targetScope !== 'ALL' && form.targets.length === 0) {
    ElMessage.warning('请选择目标协会或行业类别')
    return
  }
  await createTask({
    ...form,
    taskName: form.taskName.trim(),
    majorCodes: form.majorCodes.join(','),
    targetValue: form.targetScope === 'ALL' ? null : JSON.stringify(form.targets),
    startTime: range.value?.[0],
    endTime: range.value?.[1]
  })
  ElMessage.success('任务已发布，待接入订阅消息通道')
  dialog.value = false
  load()
}

function goDetail(row) {
  router.push({ path: '/task/detail', query: { code: row.taskCode } })
}

async function withdraw(row) {
  await request.post(`/task/${row.taskCode}/withdraw`)
  ElMessage.success('已撤回')
  load()
}

onMounted(async () => {
  load()
  associations.value = await allAssociation()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
</style>
