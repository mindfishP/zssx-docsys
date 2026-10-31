<template>
  <div v-loading="loading">
    <el-row :gutter="16">
      <el-col :span="6"><el-card><el-statistic title="商协会总数" :value="d.assocTotal || 0" /></el-card></el-col>
      <el-col :span="6"><el-card><el-statistic title="归档档案总数" :value="d.archiveTotal || 0" /></el-card></el-col>
      <el-col :span="6"><el-card><el-statistic title="示范型协会" :value="(d.activityLevels && d.activityLevels['示范型']) || 0" /></el-card></el-col>
      <el-col :span="6"><el-card><el-statistic title="休眠型协会" :value="(d.activityLevels && d.activityLevels['休眠型']) || 0" /></el-card></el-col>
    </el-row>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :span="12">
        <el-card>
          <template #header>行业分布</template>
          <div ref="industryRef" style="height: 320px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card>
          <template #header>六大类归档数量（大类码 A-F）</template>
          <div ref="majorRef" style="height: 320px" />
        </el-card>
      </el-col>
    </el-row>

    <!-- 活跃度评估（SRS 表 3-11） -->
    <el-card style="margin-top: 16px">
      <template #header>
        <div class="card-head">
          <span>活跃度评估（会员规模 25% · 活动频次 25% · 完整率 20% · 年检 20% · 一次通过率 10%）</span>
          <div>
            <el-button size="small" @click="loadActivity">刷新</el-button>
            <el-button size="small" :loading="exporting" @click="exportReport">导出报表</el-button>
            <el-button v-if="user.isAdmin" size="small" type="primary" :loading="evaluating"
              @click="recalculate">重新计算</el-button>
          </div>
        </div>
      </template>
      <el-table :data="activity" border stripe size="small">
        <el-table-column prop="assocCode" label="编号" width="90" />
        <el-table-column prop="assocName" label="协会名称" min-width="180" show-overflow-tooltip />
        <el-table-column prop="industryType" label="行业" width="90" />
        <el-table-column prop="score" label="综合得分" width="100" />
        <el-table-column label="分档" width="100">
          <template #default="{ row }">
            <el-tag :type="{ 1: 'success', 2: 'warning', 3: 'info' }[row.level]" size="small">
              {{ row.levelText }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="会员规模" width="100">
          <template #default="{ row }">{{ Math.round(row.memberIndex) }}</template>
        </el-table-column>
        <el-table-column label="活动频次" width="100">
          <template #default="{ row }">{{ Math.round(row.activityIndex) }}</template>
        </el-table-column>
        <el-table-column label="完整率" width="90">
          <template #default="{ row }">{{ Math.round(row.coverageRate) }}</template>
        </el-table-column>
        <el-table-column label="年检达标" width="100">
          <template #default="{ row }">{{ Math.round(row.annualIndex) }}</template>
        </el-table-column>
        <el-table-column label="一次通过率" width="110">
          <template #default="{ row }">{{ Math.round(row.passRate) }}</template>
        </el-table-column>
      </el-table>

    </el-card>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import * as echarts from 'echarts'
import { ElMessage } from 'element-plus'
import { dashboard, activityList, evaluateActivity, exportActivity } from '../../api'
import { useUserStore } from '../../store/user'

const user = useUserStore()
const d = reactive({})
const activity = ref([])
const loading = ref(true)
const evaluating = ref(false)
const exporting = ref(false)
const industryRef = ref()
const majorRef = ref()

async function loadActivity() {
  activity.value = await activityList()
}

async function exportReport() {
  exporting.value = true
  try {
    const blob = await exportActivity()
    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = '协会活跃度报表.xlsx'
    a.click()
    setTimeout(() => URL.revokeObjectURL(url), 30000)
  } finally {
    exporting.value = false
  }
}

async function recalculate() {
  evaluating.value = true
  try {
    activity.value = await evaluateActivity()
    ElMessage.success('已重新计算并写回各协会活跃度')
    loadDashboard()
  } finally {
    evaluating.value = false
  }
}

async function loadDashboard() {
  Object.assign(d, await dashboard())
  renderCharts()
}

function renderCharts() {
  const ind = echarts.init(industryRef.value)
  ind.setOption({
    tooltip: {},
    xAxis: { type: 'category', data: Object.keys(d.industryDistribution || {}) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', itemStyle: { color: '#d4875a' }, data: Object.values(d.industryDistribution || {}) }]
  })
  const maj = echarts.init(majorRef.value)
  maj.setOption({
    tooltip: {},
    xAxis: { type: 'category', data: Object.keys(d.majorDistribution || {}) },
    yAxis: { type: 'value' },
    series: [{ type: 'bar', itemStyle: { color: '#2c3e50' }, data: Object.values(d.majorDistribution || {}) }]
  })
}

onMounted(async () => {
  try {
    await loadDashboard()
    await loadActivity()
  } finally {
    loading.value = false
  }
})
</script>

<style scoped>
.card-head { display: flex; align-items: center; justify-content: space-between; }
.hint { color: #909399; font-size: 12px; margin-top: 8px; }
</style>
