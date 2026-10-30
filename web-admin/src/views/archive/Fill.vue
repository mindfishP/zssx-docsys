<template>
  <div v-loading="loading">
    <el-page-header @back="goBack" :content="editing ? '修改档案' : '填报档案'" />

    <el-card class="box">
      <el-alert v-if="rejectReason" type="warning" :closable="false" class="mb"
        :title="'退回意见：' + rejectReason" />
      <el-alert v-if="archiveId" type="success" :closable="false" class="mb"
        :title="'档案编号：' + archiveId + '（已归档）'" />

      <el-form :model="form" label-width="110px" class="form">
        <el-form-item label="所属协会">
          <el-tag>{{ user.assocCode || '—' }}</el-tag>
        </el-form-item>

        <el-form-item label="档案分类" required>
          <el-select v-model="majorCode" placeholder="选择大类" style="width: 170px"
            @change="onMajorChange">
            <el-option v-for="m in majors" :key="m.code" :label="m.name" :value="m.code" />
          </el-select>
          <el-select v-model="form.categoryCode" placeholder="选择中类" filterable
            style="width: 330px; margin-left: 8px" @change="onCategoryChange">
            <el-option v-for="c in categoryOptions" :key="c.categoryCode"
              :label="`${c.majorName} / ${c.subName}（${c.categoryCode}）`" :value="c.categoryCode" />
          </el-select>

        </el-form-item>

        <el-form-item label="题名" :required="isRequired('title')">
          <el-input v-model="form.title" maxlength="200" show-word-limit
            placeholder="如 2026 年度会员名册" />
        </el-form-item>

        <el-form-item label="年度" :required="isRequired('archiveYear')">
          <el-input v-model="form.archiveYear" maxlength="4" style="width: 140px" placeholder="如 2026" />
          <span class="tip">4 位年份，默认当前年</span>
        </el-form-item>

        <el-form-item label="责任者" :required="isRequired('responsibility')">
          <el-input v-model="form.responsibility" maxlength="64" placeholder="形成该材料的部门或个人" />
        </el-form-item>

        <el-form-item label="密级">
          <el-radio-group v-model="form.securityLevel">
            <el-radio :value="1">公开</el-radio>
            <el-radio :value="2">内部</el-radio>
          </el-radio-group>
          <span class="tip">默认取所选分类；“内部”档案不对其他协会开放借阅</span>
        </el-form-item>

        <el-form-item label="保管期限">
          <el-select v-model="form.retention" clearable placeholder="默认取所选分类" style="width: 200px">
            <el-option label="永久" value="永久" />
            <el-option label="定期30年" value="定期30年" />
            <el-option label="定期10年" value="定期10年" />
          </el-select>
        </el-form-item>

        <el-form-item label="附件">
          <div class="attach-box">
            <input type="file" multiple @change="onFiles" />
            <div class="tip">
              支持 PDF、Office 文档、图片与压缩包，单文件不超过 60MB；
            </div>
            <el-table v-if="attachments.length" :data="attachments" border size="small" class="attach-table">
              <el-table-column prop="fileName" label="文件名" min-width="240" show-overflow-tooltip />
              <el-table-column label="大小" width="100">
                <template #default="{ row }">{{ formatSize(row.fileSize) }}</template>
              </el-table-column>
              <el-table-column label="操作" width="140">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openAttachment(row)">查看</el-button>
                  <el-button link type="danger" @click="removeAttachment(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
        </el-form-item>
      </el-form>

      <div class="actions">
        <el-button @click="goBack">取消</el-button>
        <el-button :loading="saving" @click="save(false)">存草稿</el-button>
        <el-button type="primary" :loading="saving" @click="save(true)">提交送审</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  listCategory, getArchiveDetail, createArchive, updateArchive,
  uploadFile, deleteAttachment, getSignedUrl, fetchAttachment
} from '../../api'
import { useUserStore } from '../../store/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const bizId = computed(() => (route.query.bizId ? Number(route.query.bizId) : null))
const editing = computed(() => !!bizId.value)

const loading = ref(false)
const saving = ref(false)
const categories = ref([])
const attachments = ref([])
const rejectReason = ref('')
const archiveId = ref('')
const majorCode = ref('')

const majors = [
  { code: 'A', name: '登记类' }, { code: 'B', name: '治理类' }, { code: 'C', name: '会员类' },
  { code: 'D', name: '活动类' }, { code: 'E', name: '财务类' }, { code: 'F', name: '党建类' }
]

const form = reactive({
  categoryCode: '',
  title: '',
  archiveYear: String(new Date().getFullYear()),
  responsibility: '',
  securityLevel: 1,
  retention: ''
})

// 只在选定大类后展示该大类下的中类
const categoryOptions = computed(() => majorCode.value
  ? categories.value.filter((c) => c.majorCode === majorCode.value)
  : categories.value)

const selectedCategory = computed(() =>
  categories.value.find((c) => c.categoryCode === form.categoryCode) || null)

// 分类元数据字典声明的必填项（SRS FR-03-02）
const requiredKeys = computed(() => {
  const raw = selectedCategory.value && selectedCategory.value.requiredMetaKeys
  if (!raw) return []
  try {
    const arr = JSON.parse(raw)
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
})
const isRequired = (key) => requiredKeys.value.includes(key)

const formatSize = (n) => (n == null ? '—'
  : n > 1048576 ? (n / 1048576).toFixed(1) + ' MB' : Math.max(1, Math.round(n / 1024)) + ' KB')

function goBack() {
  router.push('/archive')
}

function onMajorChange() {
  form.categoryCode = ''
}

// 切换分类时带出默认密级与保管期限
function onCategoryChange() {
  const cat = selectedCategory.value
  if (!cat) return
  if (cat.defaultSecurityLevel != null) form.securityLevel = cat.defaultSecurityLevel
  if (cat.defaultRetention) form.retention = cat.defaultRetention
}

async function load() {
  loading.value = true
  try {
    categories.value = await listCategory()
    if (!bizId.value) return
    const d = await getArchiveDetail(bizId.value)
    const a = d.archive
    if (a.status !== 0 && a.status !== 2) {
      ElMessage.warning('仅草稿或审核退回的档案可修改')
      router.replace('/archive')
      return
    }
    Object.assign(form, {
      categoryCode: a.categoryCode,
      title: a.title,
      archiveYear: a.archiveYear,
      responsibility: a.responsibility || '',
      securityLevel: a.securityLevel ?? 1,
      retention: a.retention || ''
    })
    majorCode.value = a.majorCode || (a.categoryCode || '').substring(0, 1)
    rejectReason.value = a.status === 2 ? (a.rejectReason || '') : ''
    archiveId.value = a.archiveId || ''
    attachments.value = d.attachments || []
  } finally {
    loading.value = false
  }
}

async function onFiles(e) {
  const files = Array.from(e.target.files || [])
  if (!files.length) return
  if (!form.categoryCode) {
    ElMessage.warning('请先选择档案分类，附件按大类分档存储')
    e.target.value = ''
    return
  }
  const major = selectedCategory.value ? selectedCategory.value.majorCode : majorCode.value
  for (const f of files) {
    const fd = new FormData()
    fd.append('file', f)
    fd.append('majorCode', major || '')
    if (editing.value) fd.append('bizId', bizId.value)
    const att = await uploadFile(fd)
    attachments.value.push(att)
  }
  e.target.value = ''
  ElMessage.success('附件上传完成并登记哈希')
}

async function removeAttachment(att) {
  try {
    await ElMessageBox.confirm(
      `确认删除附件「${att.fileName}」？删除后不可恢复，操作将记入审计日志。`,
      '删除附件',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  await deleteAttachment(att.attachId)
  attachments.value = attachments.value.filter((a) => a.attachId !== att.attachId)
  ElMessage.success('附件已删除')
}

async function openAttachment(row) {
  try {
    const path = await getSignedUrl(row.attachId)
    const blob = await fetchAttachment(path)
    if (!blob || !blob.size) throw new Error('附件内容为空')
    if (blob.type && blob.type.includes('json')) {
      let msg = '附件打开失败'
      try { msg = JSON.parse(await blob.text()).message || msg } catch (e) { /* ignore */ }
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
  }
}

async function save(submit) {
  if (!form.categoryCode) {
    ElMessage.warning('请选择档案分类')
    return
  }
  if (!form.title || !form.title.trim()) {
    ElMessage.warning('请填写题名')
    return
  }
  if (!/^\d{4}$/.test(form.archiveYear || '')) {
    ElMessage.warning('年度应为 4 位年份')
    return
  }
  if (isRequired('responsibility') && !(form.responsibility || '').trim()) {
    ElMessage.warning('该分类要求填写责任者')
    return
  }
  saving.value = true
  try {
    const payload = {
      categoryCode: form.categoryCode,
      title: form.title.trim(),
      archiveYear: form.archiveYear,
      responsibility: form.responsibility,
      securityLevel: form.securityLevel,
      retention: form.retention || null,
      attachIds: editing.value ? [] : attachments.value.map((a) => a.attachId),
      submit
    }
    if (editing.value) {
      await updateArchive(bizId.value, payload)
    } else {
      await createArchive(payload)
    }
    ElMessage.success(submit ? '已提交送审' : '已保存草稿')
    router.push('/archive')
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.box { margin-top: 16px; }
.mb { margin-bottom: 12px; }
.form { max-width: 880px; }
.tip { color: #909399; font-size: 12px; line-height: 1.6; margin-left: 8px; }
.attach-box { width: 100%; }
.attach-box .tip { margin-left: 0; }
.attach-table { margin-top: 8px; }
.actions { margin-top: 8px; padding-left: 110px; display: flex; gap: 10px; }
</style>
