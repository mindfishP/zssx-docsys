<template>
  <div v-loading="loading">
    <el-page-header @back="goBack" :content="detail ? detail.assocName : '商协会详情'">
      <template #extra>
        <el-button v-if="user.isAdmin && detail" size="small" type="primary" @click="goEdit">修改</el-button>
      </template>
    </el-page-header>

    <template v-if="detail">
      <el-card class="box">
        <template #header><span>基本信息</span></template>
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="协会编号">{{ detail.assocCode }}</el-descriptions-item>
          <el-descriptions-item label="协会名称">{{ detail.assocName }}</el-descriptions-item>
          <el-descriptions-item label="统一社会信用代码" :span="2">
            <span class="code">{{ detail.creditCode || '未生成' }}</span>
          </el-descriptions-item>
          <el-descriptions-item label="行业类别">{{ detail.industryType || '—' }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="{ 1: 'success', 0: 'info', 2: 'danger' }[detail.status]" size="small">
              {{ { 1: '正常', 0: '注销', 2: '异常' }[detail.status] || '未知' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="登记机关">{{ detail.registerOrg || '—' }}</el-descriptions-item>
          <el-descriptions-item label="登记日期">{{ detail.registerDate || '—' }}</el-descriptions-item>
          <el-descriptions-item label="法定代表人">{{ detail.legalPerson || '—' }}</el-descriptions-item>
          <el-descriptions-item label="联系电话">{{ detail.contactPhone || '—' }}</el-descriptions-item>
          <el-descriptions-item label="活跃度分档">
            <el-tag :type="{ 1: 'success', 2: 'warning', 3: 'info' }[detail.activityLevel]" size="small">
              {{ { 1: '示范型', 2: '成长型', 3: '休眠型' }[detail.activityLevel] || '未评估' }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="活跃度得分">{{ detail.activityScore ?? '—' }}</el-descriptions-item>
          <el-descriptions-item label="建档时间">{{ fmt(detail.createdAt) }}</el-descriptions-item>
          <el-descriptions-item label="最近更新">{{ fmt(detail.updatedAt) }}</el-descriptions-item>
        </el-descriptions>
      </el-card>

      <el-card class="box">
        <template #header>
          <div class="card-head">
            <span>成员账号（{{ users.length }}）</span>

          </div>
        </template>
        <el-empty v-if="!users.length" description="该协会暂无账号" :image-size="80" />
        <el-table v-else :data="users" border size="small">
          <el-table-column prop="username" label="登录账号" width="160" />
          <el-table-column label="姓名" width="160">
            <template #default="{ row }">{{ row.realName || '—' }}</template>
          </el-table-column>
          <el-table-column label="角色" width="140">
            <template #default="{ row }">
              <el-tag size="small" effect="plain">{{ roleText(row.roleCode) }}</el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="phone" label="联系电话" width="150">
            <template #default="{ row }">{{ row.phone || '—' }}</template>
          </el-table-column>
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
                {{ row.status === 1 ? '正常' : '停用' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="最近登录" min-width="170">
            <template #default="{ row }">{{ row.lastLoginAt ? fmt(row.lastLoginAt) : '从未登录' }}</template>
          </el-table-column>
          <el-table-column label="创建时间" min-width="170">
            <template #default="{ row }">{{ fmt(row.createdAt) }}</template>
          </el-table-column>
        </el-table>
      </el-card>
    </template>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getAssociation, getAssociationUsers } from '../../api'
import { useUserStore } from '../../store/user'

const route = useRoute()
const router = useRouter()
const user = useUserStore()

const assocCode = computed(() => route.query.code || '')
const loading = ref(false)
const detail = ref(null)
const users = ref([])

// 后端返回 ISO 时间（2026-10-29T22:44:15），展示时换成空格分隔
const fmt = (v) => (v ? String(v).replace('T', ' ').slice(0, 19) : '—')

const roleText = (code) => ({
  ADMIN: '主管部门操作员', LEADER: '主管领导', ASSOC: '商协会管理员'
}[code] || code || '—')

function goBack() {
  router.push('/association')
}

function goEdit() {
  router.push({ path: '/association/edit', query: { code: assocCode.value } })
}

async function load() {
  if (!assocCode.value) {
    ElMessage.warning('缺少协会编号')
    router.replace('/association')
    return
  }
  loading.value = true
  try {
    detail.value = await getAssociation(assocCode.value)
    users.value = await getAssociationUsers(assocCode.value)
  } catch (e) {
    ElMessage.error('协会详情加载失败')
    router.replace('/association')
  } finally {
    loading.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.box { margin-top: 16px; }
.card-head { display: flex; align-items: baseline; gap: 12px; }
.tip { color: #909399; font-size: 12px; }
.code { font-family: Consolas, monospace; letter-spacing: 1px; }
</style>
