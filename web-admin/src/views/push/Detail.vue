<template>
  <div v-loading="loading">
    <el-page-header @back="goBack" content="政策推送详情" />

    <template v-if="detail">
      <el-card class="box">
        <h2 class="title">{{ detail.title }}</h2>
        <div class="meta">
          <el-tag size="small" :type="{ 1: 'success', 0: 'info', 2: 'danger' }[detail.status]">
            {{ { 1: '已发布', 0: '草稿', 2: '已撤回' }[detail.status] || '未知' }}
          </el-tag>
          <span>编号 #{{ detail.pushId }}</span>
          <span>发布时间：{{ detail.publishedAt || '—' }}</span>
        </div>
        <el-divider />
        <div class="content">{{ detail.content || '（该推送没有正文）' }}</div>
      </el-card>

      <el-card class="box">
        <template #header><span>推送范围</span></template>
        <el-descriptions :column="1" border size="small">
          <el-descriptions-item label="范围类型">{{ scopeLabel }}</el-descriptions-item>
          <el-descriptions-item label="目标对象">{{ targetsText }}</el-descriptions-item>
          <el-descriptions-item label="发布人">{{ detail.createdBy || '—' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPushDetail } from '../../api'

const route = useRoute()
const router = useRouter()

const pushId = computed(() => (route.query.id ? Number(route.query.id) : null))
const loading = ref(false)
const detail = ref(null)

const levelText = (v) => ({ 1: '示范型', 2: '成长型', 3: '休眠型' }[String(v)] || v)
const scopeLabel = computed(() => ({
  ALL: '全部协会',
  LIST: '指定协会',
  INDUSTRY: '按行业类别',
  LEVEL: '按活跃度分档'
}[detail.value?.targetScope] || detail.value?.targetScope || '—'))

const targetsText = computed(() => {
  const d = detail.value
  if (!d) return '—'
  if (d.targetScope === 'ALL') return '全体商协会'
  let arr = []
  try {
    const parsed = JSON.parse(d.targetValue || '[]')
    arr = Array.isArray(parsed) ? parsed : []
  } catch (e) {
    arr = []
  }
  if (!arr.length) return '—'
  return d.targetScope === 'LEVEL' ? arr.map(levelText).join('、') : arr.join('、')
})

function goBack() {
  router.push('/push')
}

async function load() {
  if (!pushId.value) {
    ElMessage.warning('缺少推送编号')
    router.replace('/push')
    return
  }
  loading.value = true
  try {
    detail.value = await getPushDetail(pushId.value)
  } catch (e) {
    ElMessage.error('推送详情加载失败，可能不在本会接收范围内')
    router.replace('/push')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.box { margin-top: 16px; }
.title { margin: 0 0 12px; font-size: 20px; color: #1a2330; line-height: 1.5; }
.meta { display: flex; align-items: center; gap: 16px; color: #909399; font-size: 13px; }
.content {
  white-space: pre-wrap;
  line-height: 1.9;
  color: #303133;
  font-size: 14px;
  min-height: 80px;
}
</style>
