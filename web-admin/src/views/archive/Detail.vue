<template>
  <div v-loading="loading">
    <el-page-header @back="goBack" :content="'档案详情'">
      <template #extra>
        <el-button v-if="canEdit" size="small" type="primary" @click="goEdit">修改档案</el-button>
      </template>
    </el-page-header>

    <template v-if="detail">
            <el-alert v-if="borrowed" type="warning" :closable="false" class="box"
        title="借阅查阅中：仅可在线查看，不可下载。" />
      <el-card class="box">
        <template #header>
          <div class="card-head">
            <span>基本信息</span>
            <el-tag :type="statusType(detail.archive.status)">{{ statusText(detail.archive.status) }}</el-tag>
          </div>
        </template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="档案编号">{{ detail.archive.archiveId || '—（未归档）' }}</el-descriptions-item>
          <el-descriptions-item label="所属协会">{{ detail.archive.assocCode }}</el-descriptions-item>
          <el-descriptions-item label="分类">{{ detail.archive.categoryCode }}（{{ detail.archive.majorCode }} 类）</el-descriptions-item>
          <el-descriptions-item label="年度">{{ detail.archive.archiveYear }}</el-descriptions-item>
          <el-descriptions-item label="题名" :span="2">{{ detail.archive.title }}</el-descriptions-item>
          <el-descriptions-item label="责任者">{{ detail.archive.responsibility || '—' }}</el-descriptions-item>
          <el-descriptions-item label="密级">{{ detail.archive.securityLevel === 2 ? '内部' : '公开' }}</el-descriptions-item>
          <el-descriptions-item label="保管期限">{{ detail.archive.retention || '—' }}</el-descriptions-item>
          <el-descriptions-item label="提交时间">{{ detail.archive.submitAt || '—' }}</el-descriptions-item>
          <el-descriptions-item label="归档时间">{{ detail.archive.archivedAt || '—' }}</el-descriptions-item>
          <el-descriptions-item v-if="detail.archive.rejectReason" label="退回意见" :span="2">
            <span class="danger">{{ detail.archive.rejectReason }}</span>
          </el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="box">
        <template #header>
          <span>附件（{{ attachments.length }}）</span>
        </template>
        <el-empty v-if="!attachments.length" description="暂无附件" :image-size="80" />
        <div ref="attachArea" class="attach-list">
          <div v-for="(a, idx) in attachments" :key="a.attachId" class="attach-card">
            <div class="attach-head">
              <span class="attach-index">{{ idx + 1 }}</span>
              <span class="attach-name">{{ a.fileName }}</span>
              <span class="attach-meta">
                {{ formatSize(a.fileSize) }} · {{ (a.fileType || '').toUpperCase() }}
                <template v-if="a.pages"> · 共 {{ a.pages }} 页</template>
              </span>
              <span class="attach-hash">SHA-256 {{ (a.fileHash || '').slice(0, 16) }}…</span>
              <el-button v-if="a.previewUrl && canDownload" link type="primary" @click="openInNewTab(a)">新窗口</el-button>
              <el-button v-if="a.previewUrl && canDownload" link type="primary" @click="download(a)">下载</el-button>
              <span v-else-if="a.previewUrl" class="readonly-tag">仅可在线查阅</span>
            </div>
          <div v-if="a.loading" class="attach-state">正在加载附件内容…</div>
          <div v-else-if="a.error" class="attach-state danger">附件加载失败（可能已被移动或无权限）</div>
            <img v-else-if="isImage(a)" :src="a.previewUrl" class="preview-image" :class="{ 'readonly': !canDownload }" @click="canDownload && openInNewTab(a)" />
            <iframe v-else-if="isPdf(a)" :src="a.previewUrl + (canDownload ? '#view=FitH' : '#toolbar=0&view=FitH')"
              class="preview-pdf" :style="{ height: pdfHeight(a) + 'px' }" />
            <div v-else class="preview-none">
              该格式不支持在页面内预览，请点击右上角“下载”后查看。
            </div>
          </div>
        </div>
      </el-card>

      <el-card class="box">
        <template #header><span>审核记录（{{ detail.reviews.length }}）</span></template>
        <el-empty v-if="!detail.reviews.length" description="暂无审核记录" :image-size="80" />
        <el-table v-else :data="detail.reviews" border size="small">
          <el-table-column label="节点" width="90">
            <template #default="{ row }">{{ row.nodeLevel === 2 ? '复审' : '初审' }}</template>
          </el-table-column>
          <el-table-column label="结果" width="90">
            <template #default="{ row }">
              <el-tag :type="row.reviewResult === 1 ? 'success' : 'danger'" size="small">
                {{ row.reviewResult === 1 ? '通过' : '退回' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="reviewOpinion" label="意见" min-width="240" show-overflow-tooltip />
          <el-table-column prop="reviewAt" label="时间" width="170" />
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, nextTick, onMounted, onUnmounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getArchiveDetail, getSignedUrl, getSignedViewUrl, fetchAttachment } from '../../api'
import { useUserStore } from '../../store/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const bizId = computed(() => (route.query.bizId ? Number(route.query.bizId) : null))
const loading = ref(false)
const detail = ref(null)
const attachments = ref([])
// 附件区域宽度：用于按页数推算 PDF 的完整高度
const attachArea = ref(null)
const contentWidth = ref(880)

const statusText = (s) => ({ 0: '草稿', 1: '待审核', 2: '审核退回', 3: '已归档' }[s] || s)
const statusType = (s) => ({ 0: 'info', 1: 'warning', 2: 'danger', 3: 'success' }[s] || 'info')
const formatSize = (n) => (n == null ? '—'
  : n > 1048576 ? (n / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(n / 1024)) + ' KB')
const isImage = (a) => (a.mime || '').startsWith('image/')
const isPdf = (a) => (a.mime || '') === 'application/pdf'

const access = computed(() => detail.value?.access || { owner: true, borrowed: false, canDownload: true })
const canDownload = computed(() => access.value.canDownload !== false)
const borrowed = computed(() => !!access.value.borrowed)

const canEdit = computed(() => {
  if (!detail.value || !user.isAssoc) return false
  const st = detail.value.archive.status
  return st === 0 || st === 2
})

function goBack() {
  router.push('/archive')
}

function goEdit() {
  router.push({ path: '/archive/fill', query: { bizId: bizId.value } })
}

function download(a) {
  const link = document.createElement('a')
  link.href = a.previewUrl
  link.download = a.fileName || 'attachment'
  link.click()
}

function openInNewTab(a) {
  if (a.previewUrl) window.open(a.previewUrl, '_blank')
}

// A4 纸张高宽比约 1.414，Chrome 内嵌阅读器每页之间还有少量间隙
function pdfHeight(a) {
  const pages = Math.min(a.pages || 1, 40)
  return Math.round(contentWidth.value * 1.42 * pages + 96)
}

function syncWidth() {
  if (attachArea.value) {
    contentWidth.value = Math.max(320, attachArea.value.clientWidth - 2)
  }
}

/** 粗略统计 PDF 页数：优先数 /Type /Page（排除 /Pages），失败则读 /Count 最大值 */
async function countPdfPages(blob) {
  try {
    const buf = await blob.arrayBuffer()
    const bytes = new Uint8Array(buf)
    let text = ''
    const CHUNK = 0x8000
    for (let i = 0; i < bytes.length; i += CHUNK) {
      text += String.fromCharCode.apply(null, bytes.subarray(i, i + CHUNK))
    }
    const pages = text.match(/\/Type\s*\/Page[^s]/g)
    if (pages && pages.length) return pages.length
    const counts = [...text.matchAll(/\/Count\s+(\d+)/g)].map((m) => Number(m[1]))
    return counts.length ? Math.max(...counts) : 0
  } catch (e) {
    return 0
  }
}

async function loadAttachments(list) {
  attachments.value = list.map((a) => ({ ...a, previewUrl: '', mime: '', loading: true, error: false }))
  for (const a of attachments.value) {
    try {
      const path = canDownload.value
        ? await getSignedUrl(a.attachId)
        : await getSignedViewUrl(a.attachId)
      const blob = await fetchAttachment(path)
      if (!blob || !blob.size) throw new Error('empty')
      a.mime = blob.type || ''
      a.previewUrl = URL.createObjectURL(blob)
      if (a.mime === 'application/pdf') {
        a.pages = await countPdfPages(blob)
      }
    } catch (e) {
      a.error = true
    } finally {
      a.loading = false
    }
  }
}

async function load() {
  if (!bizId.value) {
    ElMessage.warning('缺少档案参数')
    router.replace('/archive')
    return
  }
  loading.value = true
  try {
    const d = await getArchiveDetail(bizId.value)
    detail.value = d
    await loadAttachments(d.attachments || [])
    await nextTick()
    syncWidth()
  } catch (e) {
    ElMessage.error('档案详情加载失败，可能无权限查看')
    router.replace('/archive')
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  window.addEventListener('resize', syncWidth)
  await load()
  await nextTick()
  syncWidth()
})
onUnmounted(() => {
  window.removeEventListener('resize', syncWidth)
  attachments.value.forEach((a) => {
    if (a.previewUrl) URL.revokeObjectURL(a.previewUrl)
  })
})
</script>

<style scoped>
.box { margin-top: 16px; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.danger { color: #f56c6c; }
/* 多个附件按顺序上下排列，每个占满一行 */
.attach-list { display: flex; flex-direction: column; gap: 14px; }
.attach-card { border: 1px solid #ebeef5; border-radius: 4px; padding: 12px; background: #fff; }
.attach-head { display: flex; align-items: center; gap: 12px; flex-wrap: wrap; }
.attach-index {
  width: 20px; height: 20px; line-height: 20px; text-align: center; border-radius: 50%;
  background: #d4875a; color: #fff; font-size: 12px; flex: none;
}
.attach-name { font-weight: 600; color: #1a2330; word-break: break-all; }
.attach-meta { color: #909399; font-size: 12px; }
.attach-hash { color: #b0b8c0; font-size: 12px; font-family: Consolas, monospace; flex: 1; }
.attach-state { color: #909399; font-size: 13px; padding: 16px 0; }
/* 图片按原始比例完整显示，宽度不超出容器 */
.preview-image { display: block; max-width: 100%; height: auto; margin-top: 10px; cursor: zoom-in; border-radius: 2px; }
/* PDF 高度由页数与容器宽度计算，保证内容完整可见 */
.preview-pdf { width: 100%; border: 1px solid #ebeef5; border-radius: 2px; margin-top: 10px; background: #f5f7fa; display: block; }
.preview-none { color: #909399; font-size: 13px; padding: 16px 0; }
.readonly-tag { color: #e6a23c; font-size: 12px; }
.preview-image.readonly { cursor: default; }
</style>
