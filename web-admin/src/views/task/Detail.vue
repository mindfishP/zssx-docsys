<template>
  <div v-loading="loading">
    <el-page-header @back="goBack" content="专项补录详情" />

    <template v-if="detail">
      <el-card class="box">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="任务编号">{{ detail.taskCode }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="{ 1: 'primary', 2: 'info', 0: 'danger' }[detail.status]" size="small">
              {{ { 1: '进行中', 2: '已结束', 0: '已撤回' }[detail.status] || '未知' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="任务名称" :span="2">{{ detail.taskName }}</el-descriptions-item>
          <el-descriptions-item label="涉及大类">{{ majorText }}</el-descriptions-item>
          <el-descriptions-item label="目标范围">{{ scopeText }}</el-descriptions-item>
          <el-descriptions-item label="开始时间">{{ detail.startTime || '—' }}</el-descriptions-item>
          <el-descriptions-item label="截止时间">{{ detail.endTime || '—' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="box">
        <template #header><span>任务说明</span></template>
        <div class="content">{{ detail.taskDesc || '（未填写任务说明）' }}</div>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getTask } from '../../api'

const route = useRoute()
const router = useRouter()

const taskCode = computed(() => route.query.code || '')
const loading = ref(false)
const detail = ref(null)

const majorNames = {
  A: '登记类', B: '治理类', C: '会员类',
  D: '活动类', E: '财务类', F: '党建类'
}
const majorText = computed(() => {
  const codes = (detail.value?.majorCodes || '').split(',').map((s) => s.trim()).filter(Boolean)
  if (!codes.length) return '不限（全部大类）'
  return codes.map((c) => `${c} ${majorNames[c] || ''}`.trim()).join('、')
})

const scopeText = computed(() => {
  const d = detail.value
  if (!d) return '—'
  let targets = []
  try {
    const arr = JSON.parse(d.targetValue || '[]')
    targets = Array.isArray(arr) ? arr : []
  } catch (e) {
    targets = []
  }
  if (d.targetScope === 'ALL') return '全部协会'
  if (d.targetScope === 'INDUSTRY') return `按行业：${targets.join('、') || '—'}`
  if (d.targetScope === 'LIST') return `指定协会：${targets.join('、') || '—'}`
  return d.targetScope || '—'
})

function goBack() {
  router.push('/task')
}

async function load() {
  if (!taskCode.value) {
    ElMessage.warning('缺少任务编号')
    router.replace('/task')
    return
  }
  loading.value = true
  try {
    detail.value = await getTask(taskCode.value)
  } catch (e) {
    ElMessage.error('任务详情加载失败')
    router.replace('/task')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.box { margin-top: 16px; }
.content {
  white-space: pre-wrap;
  line-height: 1.9;
  color: #303133;
  font-size: 14px;
  min-height: 60px;
}
</style>
