<template>
  <el-dialog :model-value="modelValue" title="档案详情" width="760px"
    @update:model-value="$emit('update:modelValue', $event)">
    <div v-loading="loading">
      <el-descriptions v-if="detail" :column="2" border size="small">
        <el-descriptions-item label="档案编号">{{ detail.archive.archiveId || '—（未归档）' }}</el-descriptions-item>
        <el-descriptions-item label="状态">
          <el-tag :type="statusType(detail.archive.status)">{{ statusText(detail.archive.status) }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="协会">{{ detail.archive.assocCode }}</el-descriptions-item>
        <el-descriptions-item label="分类">{{ detail.archive.categoryCode }}（{{ detail.archive.majorCode }} 类）</el-descriptions-item>
        <el-descriptions-item label="题名" :span="2">{{ detail.archive.title }}</el-descriptions-item>
        <el-descriptions-item label="年度">{{ detail.archive.archiveYear }}</el-descriptions-item>
        <el-descriptions-item label="责任者">{{ detail.archive.responsibility || '—' }}</el-descriptions-item>
        <el-descriptions-item label="密级">{{ detail.archive.securityLevel === 2 ? '内部' : '公开' }}</el-descriptions-item>
        <el-descriptions-item label="保管期限">{{ detail.archive.retention || '—' }}</el-descriptions-item>
        <el-descriptions-item label="提交时间">{{ detail.archive.submitAt || '—' }}</el-descriptions-item>
        <el-descriptions-item label="归档时间">{{ detail.archive.archivedAt || '—' }}</el-descriptions-item>
        <el-descriptions-item v-if="detail.archive.rejectReason" label="退回意见" :span="2">
          <span class="danger">{{ detail.archive.rejectReason }}</span>
        </el-descriptions-item>
      </el-descriptions>

      <h4 class="section">附件（{{ detail ? detail.attachments.length : 0 }}）</h4>

      <el-table :data="detail ? detail.attachments : []" border size="small" empty-text="暂无附件">
        <el-table-column prop="fileName" label="文件名" min-width="200" show-overflow-tooltip />
        <el-table-column label="大小" width="100">
          <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
        </el-table-column>
        <el-table-column label="SHA-256（前 16 位）" width="180">
          <template #default="{ row }"><span class="hash">{{ (row.fileHash || '').slice(0, 16) }}</span></template>
        </el-table-column>
        <el-table-column prop="uploadAt" label="上传时间" width="160" />
        <el-table-column label="操作" width="130">
          <template #default="{ row }">
            <el-button link type="primary" :loading="openingId === row.attachId"
              @click="openAttachment(row)">查看</el-button>
            <el-button v-if="deletable" link type="danger" @click="removeAttachment(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <h4 class="section">审核记录（{{ detail ? detail.reviews.length : 0 }}）</h4>
      <el-table :data="detail ? detail.reviews : []" border size="small" empty-text="暂无审核记录">
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
        <el-table-column prop="reviewOpinion" label="意见" min-width="200" show-overflow-tooltip />
        <el-table-column prop="reviewAt" label="时间" width="160" />
      </el-table>
    </div>
    <template #footer>
      <el-button @click="$emit('update:modelValue', false)">关闭</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { getArchiveDetail, getSignedUrl, fetchAttachment, deleteAttachment } from '../api'

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  bizId: { type: [Number, String], default: null },
  // 是否允许删除附件：商协会管理员在档案归档前（草稿/待审核/审核退回）可删
  deletable: { type: Boolean, default: false }
})
const emit = defineEmits(['update:modelValue', 'deleted'])

const loading = ref(false)
const detail = ref(null)
const openingId = ref(null)

const statusText = (s) => ({ 0: '草稿', 1: '待审核', 2: '审核退回', 3: '已归档' }[s] || s)
const statusType = (s) => ({ 0: 'info', 1: 'warning', 2: 'danger', 3: 'success' }[s] || 'info')
const formatSize = (n) => (n == null ? '—' : n > 1048576 ? (n / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(n / 1024)) + ' KB')

async function load() {
  if (!props.bizId) return
  loading.value = true
  try {
    detail.value = await getArchiveDetail(props.bizId)
  } catch (e) {
    ElMessage.error('档案详情加载失败')
  } finally {
    loading.value = false
  }
}

// 打开附件：先取时效签名地址，再带 JWT 拉取二进制，用 blob 地址在新窗口打开
async function openAttachment(row) {
  openingId.value = row.attachId
  try {
    const path = await getSignedUrl(row.attachId)
    const blob = await fetchAttachment(path)
    if (!blob || !blob.size) throw new Error('附件内容为空')
    // 服务端拒绝时可能返回 JSON 文本，避免把错误信息当成文件打开
    if (blob.type && blob.type.includes('json')) {
      let msg = '附件打开失败'
      try {
        msg = JSON.parse(await blob.text()).message || msg
      } catch (e) { /* 忽略解析失败 */ }
      throw new Error(msg)
    }
    const url = URL.createObjectURL(blob)
    const win = window.open(url, '_blank')
    if (!win) {
      const a = document.createElement('a')
      a.href = url
      a.download = row.fileName || 'attachment'
      a.click()
    }
    setTimeout(() => URL.revokeObjectURL(url), 60000)
  } catch (e) {
    ElMessage.error(e.message || '附件打开失败，请重试')
  } finally {
    openingId.value = null
  }
}

// 删除附件：二次确认后逻辑删除，成功后刷新详情并通知父页面刷新列表
async function removeAttachment(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除附件「${row.fileName}」？删除后不可恢复，操作将记入审计日志。`,
      '删除附件',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  await deleteAttachment(row.attachId)
  ElMessage.success('附件已删除')
  await load()
  emit('deleted')
}

// 打开弹窗时加载，关闭保留数据便于下次快速展示
watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      detail.value = null
      load()
    }
  }
)
</script>

<style scoped>
.section { margin: 16px 0 8px; color: #1a2330; }
.danger { color: #f56c6c; }
.hash { font-family: Consolas, monospace; color: #909399; }
.tip { color: #e6a23c; font-size: 12px; line-height: 1.6; margin-bottom: 8px; }
</style>
