<template>
  <el-card>
    <div class="toolbar">
      <el-select v-model="q.status" placeholder="状态" clearable style="width: 140px">
        <el-option label="草稿" :value="0" /><el-option label="待审核" :value="1" />
        <el-option label="审核退回" :value="2" /><el-option label="已归档" :value="3" />
      </el-select>
      <el-select v-model="q.majorCode" placeholder="六大类" clearable style="width: 140px"
        @change="q.categoryCode = ''">
        <el-option v-for="m in majors" :key="m.code" :label="m.name" :value="m.code" />
      </el-select>
      <el-select v-model="q.categoryCode" placeholder="中类" clearable filterable style="width: 190px">
        <el-option v-for="c in categoryOptions" :key="c.categoryCode"
          :label="`${c.majorName}/${c.subName}`" :value="c.categoryCode" />
      </el-select>
      <el-input v-model="q.archiveYear" placeholder="年度" clearable style="width: 100px" />
      <el-select v-model="q.securityLevel" placeholder="密级" clearable style="width: 110px">
        <el-option label="公开" :value="1" /><el-option label="内部" :value="2" />
      </el-select>
      <el-input v-model="q.keyword" placeholder="题名关键词" clearable style="width: 200px" @keyup.enter="load" />
      <el-button type="primary" @click="load">查询</el-button>
      <el-button v-if="user.isAssoc" type="success" @click="goFill">填报档案</el-button>
      <el-button v-if="user.isAssoc" @click="openImport">批量导入</el-button>
      <el-button v-if="user.isAssoc" @click="downloadTemplate">下载模板</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="archiveId" label="档案编号" width="220">
        <template #default="{ row }">{{ row.archiveId || '—（未归档）' }}</template>
      </el-table-column>
      <el-table-column prop="title" label="题名" min-width="200" show-overflow-tooltip />
      <el-table-column prop="assocCode" label="协会" width="90" />
      <el-table-column prop="majorCode" label="大类" width="70" />
      <el-table-column prop="archiveYear" label="年度" width="80" />
      <el-table-column prop="retention" label="保管期限" width="100" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="statusType(row.status)">{{ statusText(row.status) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="submitAt" label="提交时间" width="160" />
      <el-table-column label="操作" width="160" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" @click="openDetail(row)">查看</el-button>
          <el-button v-if="user.isAssoc && editable(row)" link type="primary" @click="goEdit(row)">修改</el-button>
          <el-button v-if="user.isAdmin && row.status === 3" link type="danger"
            @click="removeArchive(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <el-pagination style="margin-top: 12px" layout="total, prev, pager, next"
      :total="total" :page-size="q.size" v-model:current-page="q.page" @current-change="load" />

    <!-- 批量导入（SRS FR-04-04）：模板下载 + 逐行校验回执 -->
    <el-dialog v-model="importDialog" title="批量导入档案" width="620px">
      <el-alert type="info" :closable="false" style="margin-bottom: 12px"
        title="请使用「下载模板」得到的 xlsx：分类编码需存在于六大类分类（如 C01）；密级填 公开/内部；保管期限填 永久/定期30年/定期10年。" />
      <el-radio-group v-model="importSubmit" style="margin-bottom: 12px">
        <el-radio-button :value="false">存草稿</el-radio-button>
        <el-radio-button :value="true">提交送审</el-radio-button>
      </el-radio-group>
      <div class="import-file">
        <input type="file" accept=".xlsx" @change="onImportFile" />
        <span class="tip">选择填写好的 .xlsx 文件</span>
      </div>
      <div v-if="importResult" class="receipt">
        <el-alert :type="importResult.failed ? 'warning' : 'success'" :closable="false"
          :title="`共 ${importResult.total} 行，成功 ${importResult.success} 条，失败 ${importResult.failed} 条`" />
        <el-table v-if="importResult.errors && importResult.errors.length" :data="importResult.errors"
          border size="small" style="margin-top: 8px" max-height="220">
          <el-table-column prop="row" label="行号" width="80" />
          <el-table-column prop="message" label="失败原因" min-width="280" show-overflow-tooltip />
        </el-table>
      </div>
      <template #footer>
        <el-button @click="importDialog = false">关闭</el-button>
        <el-button type="primary" :loading="importing" :disabled="!importFile" @click="doImport">开始导入</el-button>
      </template>
    </el-dialog>


  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { pageArchive, listCategory, deleteArchive, importArchives, downloadImportTemplate } from '../../api'
import { useUserStore } from '../../store/user'

const router = useRouter()
const user = useUserStore()

const rows = ref([])
const total = ref(0)
const categories = ref([])


const importDialog = ref(false)
const importing = ref(false)
const importSubmit = ref(false)
const importFile = ref(null)
const importResult = ref(null)

const majors = [
  { code: 'A', name: '登记类' }, { code: 'B', name: '治理类' }, { code: 'C', name: '会员类' },
  { code: 'D', name: '活动类' }, { code: 'E', name: '财务类' }, { code: 'F', name: '党建类' }
]
const q = reactive({
  page: 1, size: 20, status: null, majorCode: '', categoryCode: '',
  archiveYear: '', securityLevel: null, keyword: ''
})
const categoryOptions = computed(() => q.majorCode
  ? categories.value.filter((c) => c.majorCode === q.majorCode)
  : categories.value)

const statusText = (s) => ({ 0: '草稿', 1: '待审核', 2: '审核退回', 3: '已归档' }[s] || s)
const statusType = (s) => ({ 0: 'info', 1: 'warning', 2: 'danger', 3: 'success' }[s] || 'info')
const editable = (row) => row.status === 0 || row.status === 2

async function load() {
  const data = await pageArchive(q)
  rows.value = data.records
  total.value = Number(data.total)
}

// 填报与修改都进入档案管理子页面，不再使用悬浮窗
function goFill() {
  router.push('/archive/fill')
}

function goEdit(row) {
  router.push({ path: '/archive/fill', query: { bizId: row.bizId } })
}

// 查看改为独立详情页，页面内直接展示元数据与附件内容
function openDetail(row) {
  router.push({ path: '/archive/detail', query: { bizId: row.bizId } })
}

async function removeArchive(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除已归档档案「${row.title}」？档案编号 ${row.archiveId || '—'} 删除后不可恢复，操作将记入审计日志。`,
      '删除确认',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  await deleteArchive(row.bizId)
  ElMessage.success('档案已删除')
  load()
}

function openImport() {
  importDialog.value = true
  importFile.value = null
  importResult.value = null
  importSubmit.value = false
}

function onImportFile(e) {
  importFile.value = e.target.files[0] || null
  importResult.value = null
}

async function doImport() {
  if (!importFile.value) {
    ElMessage.warning('请选择 .xlsx 文件')
    return
  }
  importing.value = true
  try {
    const fd = new FormData()
    fd.append('file', importFile.value)
    fd.append('submit', String(importSubmit.value))
    importResult.value = await importArchives(fd)
    if (importResult.value.success) load()
    ElMessage.success(`导入完成：成功 ${importResult.value.success} 条，失败 ${importResult.value.failed} 条`)
  } finally {
    importing.value = false
  }
}

async function downloadTemplate() {
  const blob = await downloadImportTemplate()
  const url = URL.createObjectURL(blob)
  const a = document.createElement('a')
  a.href = url
  a.download = '档案导入模板.xlsx'
  a.click()
  setTimeout(() => URL.revokeObjectURL(url), 30000)
}

onMounted(async () => {
  load()
  categories.value = await listCategory()
})
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; flex-wrap: wrap; }
.tip { color: #909399; font-size: 12px; line-height: 1.5; margin-top: 4px; }
.import-file { display: flex; align-items: center; gap: 10px; }
.receipt { margin-top: 12px; }
</style>
