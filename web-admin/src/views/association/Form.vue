<template>
  <div v-loading="loading">
    <el-page-header @back="goBack" :content="editing ? '修改商协会' : '新增商协会'" />

    <el-card class="box">
      <el-form :model="form" label-width="130px" class="form">
        <el-form-item label="协会编号" :required="!editing">
          <el-input v-model="form.assocCode" maxlength="16" style="width: 220px"
            placeholder="如 A008" :disabled="editing" />
          <span class="tip">{{ editing ? '协会编号创建后不可修改' : '协会唯一编号，用作档案目录与账号归属' }}</span>
        </el-form-item>

        <el-form-item label="协会名称" required>
          <el-input v-model="form.assocName" maxlength="100" placeholder="如 中山市古镇灯饰产业商会" />
        </el-form-item>

        <el-form-item label="行业类别" required>
          <el-select v-model="form.industryType" placeholder="选择行业" style="width: 220px">
            <el-option v-for="i in industries" :key="i" :label="i" :value="i" />
          </el-select>
          <span class="tip">用于行业分布统计与政策定向推送</span>
        </el-form-item>

        <el-form-item label="登记机关">
          <el-input v-model="form.registerOrg" maxlength="64" style="width: 320px"
            placeholder="如 中山市民政局" />
        </el-form-item>

        <el-form-item label="登记日期">
          <el-date-picker v-model="form.registerDate" type="date" value-format="YYYY-MM-DD"
            placeholder="选择登记日期" style="width: 220px" :clearable="false"
            :disabled="editing" @change="onDateChange" />
          <span class="tip">{{ editing ? '登记日期不可修改' : '默认今天，可按登记证书上的日期修改' }}</span>
        </el-form-item>

        <el-form-item label="法定代表人">
          <el-input v-model="form.legalPerson" maxlength="32" style="width: 220px" placeholder="如 张三" />
        </el-form-item>

        <el-form-item label="联系电话">
          <el-input v-model="form.contactPhone" maxlength="20" style="width: 260px"
            placeholder="如 0760-88888888 或 13800000000" />
        </el-form-item>

        <el-form-item v-if="editing" label="协会状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">正常</el-radio>
            <el-radio :value="0">注销</el-radio>
            <el-radio :value="2">异常</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>

      <div class="actions">
        <el-button @click="goBack">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">{{ editing ? '保存修改' : '保存' }}</el-button>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { computed, onMounted, onUnmounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { allAssociation, createAssociation, updateAssociation } from '../../api'

const route = useRoute()
const router = useRouter()
const industries = ['灯饰', '五金', '家电', '服装', '家具', '其他']

const editing = computed(() => !!route.query.code)
const editingCode = computed(() => route.query.code || '')

const today = () => {
  const d = new Date()
  const m = String(d.getMonth() + 1).padStart(2, '0')
  const day = String(d.getDate()).padStart(2, '0')
  return `${d.getFullYear()}-${m}-${day}`
}

const loading = ref(false)
const saving = ref(false)
// 用户手动改过日期后，就不再自动跟随当天
const dateTouched = ref(false)
const form = reactive({
  assocCode: '',
  assocName: '',
  industryType: '其他',
  registerOrg: '中山市民政局',
  registerDate: today(),
  legalPerson: '',
  contactPhone: '',
  status: 1
})

function onDateChange() {
  dateTouched.value = true
}

// 新增时页面停留跨天 / 切回窗口，日期自动更新为当天（未手动修改时）
let timer = null
function syncToday() {
  if (!dateTouched.value) {
    form.registerDate = today()
  }
}
function onVisible() {
  if (!document.hidden) syncToday()
}

async function load() {
  if (!editing.value) return
  loading.value = true
  try {
    const list = await allAssociation()
    const a = (list || []).find((x) => x.assocCode === editingCode.value)
    if (!a) {
      ElMessage.warning('协会不存在或已删除')
      router.replace('/association')
      return
    }
    Object.assign(form, {
      assocCode: a.assocCode,
      assocName: a.assocName,
      industryType: a.industryType || '其他',
      registerOrg: a.registerOrg || '',
      registerDate: a.registerDate || '',
      legalPerson: a.legalPerson || '',
      contactPhone: a.contactPhone || '',
      status: a.status ?? 1
    })
    dateTouched.value = true   // 编辑态不自动覆盖日期
  } finally {
    loading.value = false
  }
}

function goBack() {
  router.push('/association')
}

async function save() {
  if (!form.assocCode || !form.assocName) {
    ElMessage.warning('协会编号与名称为必填项')
    return
  }
  if (!/^[A-Za-z0-9]{2,16}$/.test(form.assocCode)) {
    ElMessage.warning('协会编号建议使用字母数字组合，如 A008')
    return
  }
  saving.value = true
  try {
    if (editing.value) {
      // 登记日期不可修改：提交时不带该字段
      await updateAssociation(editingCode.value, {
        assocName: form.assocName,
        industryType: form.industryType,
        registerOrg: form.registerOrg,
        legalPerson: form.legalPerson,
        contactPhone: form.contactPhone,
        status: form.status
      })
      ElMessage.success('协会信息已更新')
    } else {
      await createAssociation({
        assocCode: form.assocCode,
        assocName: form.assocName,
        industryType: form.industryType,
        registerOrg: form.registerOrg,
        registerDate: form.registerDate,
        legalPerson: form.legalPerson,
        contactPhone: form.contactPhone
      })
      ElMessage.success('协会已创建，统一社会信用代码已生成，可在“查看”中核对')
    }
    router.push('/association')
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  load()
  syncToday()
  timer = setInterval(syncToday, 60 * 1000)
  document.addEventListener('visibilitychange', onVisible)
})
onUnmounted(() => {
  if (timer) clearInterval(timer)
  document.removeEventListener('visibilitychange', onVisible)
})
</script>

<style scoped>
.box { margin-top: 16px; }
.form { max-width: 860px; }
.tip { color: #909399; font-size: 12px; line-height: 1.6; margin-left: 8px; }
.actions { padding-left: 130px; display: flex; gap: 10px; }
</style>
