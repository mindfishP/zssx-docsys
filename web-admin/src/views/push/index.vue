<template>
  <el-card>
    <div class="toolbar">
      <el-button v-if="user.isAdmin" type="primary" @click="openPublish">发布政策推送</el-button>
      <el-button @click="load">刷新</el-button>
    </div>

    <el-table :data="rows" border stripe>
      <el-table-column prop="pushId" label="编号" width="80" />
      <el-table-column prop="title" label="标题" min-width="200" show-overflow-tooltip />
      <el-table-column label="推送范围" min-width="240" show-overflow-tooltip>
        <template #default="{ row }">{{ scopeText(row) }}</template>
      </el-table-column>
      <el-table-column prop="publishedAt" label="发布时间" width="160" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="{ 1: 'success', 0: 'info', 2: 'danger' }[row.status]">
            {{ { 1: '已发布', 0: '草稿', 2: '已撤回' }[row.status] }}
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

    <el-dialog v-model="dialog" title="发布政策推送（SRS FR-09）" width="640px">
      <el-form :model="form" label-width="100px">
        <el-form-item label="标题">
          <el-input v-model="form.title" maxlength="200" show-word-limit />
        </el-form-item>

        <el-form-item label="推送范围">
          <el-radio-group v-model="form.targetScope" @change="onScopeChange">
            <el-radio-button value="ALL">全部协会</el-radio-button>
            <el-radio-button value="LIST">指定协会</el-radio-button>
            <el-radio-button value="INDUSTRY">按行业类别</el-radio-button>
            <el-radio-button value="LEVEL">按活跃度分档</el-radio-button>
          </el-radio-group>
        </el-form-item>

        <el-form-item v-if="form.targetScope === 'LIST'" label="选择协会">
          <el-select v-model="form.targets" multiple filterable collapse-tags collapse-tags-tooltip
            placeholder="勾选需要接收的协会，数量不限" style="width: 100%">
            <el-option v-for="a in associations" :key="a.assocCode"
              :label="`${a.assocName}（${a.assocCode}）`" :value="a.assocCode" />
          </el-select>
          <div class="tip">已选 {{ form.targets.length }} 家，数量不设上限</div>
        </el-form-item>

        <el-form-item v-else-if="form.targetScope === 'INDUSTRY'" label="行业类别">
          <el-select v-model="form.targets" multiple filterable allow-create default-first-option
            placeholder="选择或输入行业类别，如 灯饰" style="width: 100%">
            <el-option v-for="t in industries" :key="t" :label="t" :value="t" />
          </el-select>
          <div class="tip">该行业类别下的协会都会收到，可多选</div>
        </el-form-item>

        <el-form-item v-else-if="form.targetScope === 'LEVEL'" label="活跃度分档">
          <el-select v-model="form.targets" multiple placeholder="可多选" style="width: 100%">
            <el-option label="示范型" value="1" />
            <el-option label="成长型" value="2" />
            <el-option label="休眠型" value="3" />
          </el-select>
          <div class="tip">对应活跃度评估结果，可多选</div>
        </el-form-item>

        <el-form-item label="正文">
          <el-input v-model="form.content" type="textarea" :rows="6" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialog = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">发布</el-button>
      </template>
    </el-dialog>
  </el-card>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { pagePush, publishPush, allAssociation } from '../../api'
import request from '../../utils/request'
import { useUserStore } from '../../store/user'

const router = useRouter()
const user = useUserStore()
const rows = ref([])
const total = ref(0)
const dialog = ref(false)
const saving = ref(false)
const associations = ref([])
const q = reactive({ page: 1, size: 20 })
const form = reactive({ title: '', content: '', targetScope: 'ALL', targets: [] })

const industries = computed(() =>
  [...new Set(associations.value.map((a) => a.industryType).filter(Boolean))]
)

const levelText = (v) => ({ 1: '示范型', 2: '成长型', 3: '休眠型' }[String(v)] || v)
const targetsOf = (row) => {
  try {
    const arr = JSON.parse(row.targetValue || '[]')
    return Array.isArray(arr) ? arr : []
  } catch (e) {
    return []
  }
}
function scopeText(row) {
  const targets = targetsOf(row)
  if (row.targetScope === 'ALL') return '全部协会'
  if (row.targetScope === 'LIST') return `指定协会 ${targets.length} 家`
  if (row.targetScope === 'INDUSTRY') return `行业类别：${targets.join('、') || '—'}`
  if (row.targetScope === 'LEVEL') return `活跃度分档：${targets.map(levelText).join('、') || '—'}`
  return row.targetScope || '—'
}

async function load() {
  const data = await pagePush(q)
  rows.value = data.records
  total.value = Number(data.total)
}

async function openPublish() {
  form.title = ''
  form.content = ''
  form.targetScope = 'ALL'
  form.targets = []
  dialog.value = true
  if (!associations.value.length) {
    associations.value = await allAssociation()
  }
}

function onScopeChange() {
  form.targets = []
}

async function save() {
  if (!form.title.trim()) {
    ElMessage.warning('请填写推送标题')
    return
  }
  if (form.targetScope !== 'ALL' && form.targets.length === 0) {
    ElMessage.warning('请选择推送对象')
    return
  }
  saving.value = true
  try {
    await publishPush({
      title: form.title.trim(),
      content: form.content,
      targetScope: form.targetScope,
      targets: form.targets
    })
    ElMessage.success('推送已发布（送达回执统计待接入）')
    dialog.value = false
    load()
  } finally {
    saving.value = false
  }
}

function goDetail(row) {
  router.push({ path: '/push/detail', query: { id: row.pushId } })
}

async function withdraw(row) {
  await request.post(`/push/${row.pushId}/withdraw`)
  ElMessage.success('已撤回')
  load()
}

onMounted(load)
</script>

<style scoped>
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
.tip { color: #909399; font-size: 12px; line-height: 1.6; margin-top: 4px; }
</style>
