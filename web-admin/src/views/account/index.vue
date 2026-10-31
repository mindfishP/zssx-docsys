<template>
  <div>
    <!-- 个人资料：账号用于登录，姓名用于展示，分开维护 -->
    <el-card class="box">
      <template #header><span class="card-title">个人资料</span></template>
      <el-form label-width="100px" class="pwd-form">
        <el-form-item label="账号">
          <el-input :model-value="user.username" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="profile.realName" maxlength="32" placeholder="请输入姓名" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="savingName" @click="saveName">保存姓名</el-button>
  
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 所有角色：修改本人密码 -->
    <el-card class="box">
      <template #header><span class="card-title">修改密码</span></template>
      <el-form :model="pwd" label-width="100px" class="pwd-form">
        <el-form-item label="原密码">
          <el-input v-model="pwd.oldPassword" type="password" show-password autocomplete="current-password" />
        </el-form-item>
        <el-form-item label="新密码">
          <el-input v-model="pwd.newPassword" type="password" show-password autocomplete="new-password"
            placeholder="8-32 位，含字母和数字" />
        </el-form-item>
        <el-form-item label="确认新密码">
          <el-input v-model="pwd.confirm" type="password" show-password autocomplete="new-password" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submitPassword">保存</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 主管部门操作员：账号列表与删除 -->
    <el-card v-if="user.isAdmin" class="box">
      <template #header><span class="card-title">账号管理</span></template>
      <div class="toolbar">
        <el-input v-model="q.keyword" placeholder="账号或姓名" clearable style="width: 220px"
          @keyup.enter="loadUsers" />
        <el-select v-model="q.roleCode" placeholder="角色" clearable style="width: 170px">
          <el-option label="主管部门操作员" value="ADMIN" />
          <el-option label="主管领导" value="LEADER" />
          <el-option label="商协会管理员" value="ASSOC" />
        </el-select>
        <el-button type="primary" @click="loadUsers">查询</el-button>
      </div>

      <el-table :data="users" border stripe>
        <el-table-column prop="id" label="ID" width="70" />
        <el-table-column prop="username" label="账号" width="140" />
        <el-table-column label="姓名" width="160" show-overflow-tooltip>
          <template #default="{ row }">{{ row.realName || '未设置' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="160">
          <template #default="{ row }">{{ roleText(row.roleCode) }}</template>
        </el-table-column>
        <el-table-column label="所属协会" width="110">
          <template #default="{ row }">{{ row.assocCode || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'info'" size="small">
              {{ row.status === 1 ? '正常' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="lastLoginAt" label="最近登录" min-width="160" />
        <el-table-column label="操作" width="160" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openRename(row)">改名</el-button>
            <span v-if="row.username === user.username" class="muted">当前账号</span>
            <el-button v-else link type="danger" @click="removeUser(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>

      <el-pagination style="margin-top: 12px" layout="total, prev, pager, next"
        :total="total" :page-size="q.size" v-model:current-page="q.page" @current-change="loadUsers" />
    </el-card>

    <!-- 修改姓名（主管部门可改任意账号） -->
    <el-dialog v-model="renameDialog" title="修改姓名" width="400px">
      <el-form label-width="80px">
        <el-form-item label="账号">
          <el-input :model-value="renameForm.username" disabled />
        </el-form-item>
        <el-form-item label="姓名">
          <el-input v-model="renameForm.realName" maxlength="32" placeholder="请输入真实姓名" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="renameDialog = false">取消</el-button>
        <el-button type="primary" :loading="renaming" @click="submitRename">保存</el-button>
      </template>
    </el-dialog>

    <!-- 主管部门操作员：注册邀请码 -->
    <el-card v-if="user.isAdmin" class="box">
      <template #header>
        <div class="card-head">
          <span class="card-title">注册邀请码</span>
          <el-button type="primary" size="small" @click="openInvite">创建邀请码</el-button>
        </div>
      </template>
      <el-table :data="invites" border stripe>
        <el-table-column prop="code" label="邀请码" width="150" />
        <el-table-column label="注册后角色" width="170">
          <template #default="{ row }">{{ roleText(row.roleCode) }}</template>
        </el-table-column>
        <el-table-column label="加入协会" width="120">
          <template #default="{ row }">{{ row.assocCode || '—' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="inviteType(row)" size="small">{{ inviteText(row) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="expiresAt" label="有效期至" min-width="160" />
        <el-table-column prop="createdAt" label="创建时间" width="160" />
      </el-table>
      <el-pagination style="margin-top: 12px" layout="total, prev, pager, next"
        :total="inviteTotal" :page-size="inviteQ.size" v-model:current-page="inviteQ.page"
        @current-change="loadInvites" />
    </el-card>

    <!-- 创建邀请码 -->
    <el-dialog v-model="inviteDialog" title="创建邀请码" width="470px">
      <el-form :model="inviteForm" label-width="100px">
        <el-form-item label="注册后角色">
          <el-select v-model="inviteForm.roleCode" @change="onInviteRoleChange">
            <el-option label="商协会管理员" value="ASSOC" />
            <el-option label="主管部门操作员" value="ADMIN" />
            <el-option label="主管领导（只读）" value="LEADER" />
          </el-select>
        </el-form-item>
        <el-form-item v-if="inviteForm.roleCode === 'ASSOC'" label="加入协会">
          <el-select v-model="inviteForm.assocCode" filterable placeholder="选择所属协会">
            <el-option v-for="a in associations" :key="a.assocCode"
              :label="`${a.assocName}（${a.assocCode}）`" :value="a.assocCode" />
          </el-select>
        </el-form-item>
        
        <div v-if="createdCode" class="created">
          新邀请码：<span class="code">{{ createdCode }}</span>
          <el-button link type="primary" @click="copyCode">复制</el-button>
        </div>
      </el-form>
      <template #footer>
        <el-button @click="inviteDialog = false">关闭</el-button>
        <el-button type="primary" :loading="inviting" @click="submitInvite">创 建</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { changePassword, pageUsers, deleteUser, createInvite, pageInvites, allAssociation,
  updateMyName, updateUserName } from '../../api'
import { useUserStore } from '../../store/user'

const user = useUserStore()

const pwd = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const saving = ref(false)

const profile = reactive({ realName: user.realName || '' })
const savingName = ref(false)

const renameDialog = ref(false)
const renaming = ref(false)
const renameForm = reactive({ id: null, username: '', realName: '' })

const users = ref([])
const total = ref(0)
const q = reactive({ page: 1, size: 20, keyword: '', roleCode: '' })

const invites = ref([])
const inviteTotal = ref(0)
const inviteQ = reactive({ page: 1, size: 10 })
const inviteDialog = ref(false)
const inviting = ref(false)
const inviteForm = reactive({ roleCode: 'ASSOC', assocCode: '' })
const associations = ref([])
const createdCode = ref('')

const roleText = (r) => ({ ADMIN: '主管部门操作员', LEADER: '主管领导（只读）', ASSOC: '商协会管理员' }[r] || r)

async function submitPassword() {
  if (!pwd.oldPassword || !pwd.newPassword) {
    ElMessage.warning('请填写原密码与新密码')
    return
  }
  if (pwd.newPassword.length < 6) {
    ElMessage.warning('新密码至少 8 位，且需包含字母和数字')
    return
  }
  if (pwd.newPassword !== pwd.confirm) {
    ElMessage.warning('两次输入的新密码不一致')
    return
  }
  saving.value = true
  try {
    await changePassword({ oldPassword: pwd.oldPassword, newPassword: pwd.newPassword })
    ElMessage.success('密码已修改，下次登录请使用新密码')
    pwd.oldPassword = ''
    pwd.newPassword = ''
    pwd.confirm = ''
  } finally {
    saving.value = false
  }
}

async function loadUsers() {
  const data = await pageUsers(q)
  users.value = data.records || []
  total.value = Number(data.total)
}

async function removeUser(row) {
  try {
    await ElMessageBox.confirm(
      `确认删除账号「${row.username}${row.realName ? '（' + row.realName + '）' : ''}」？删除后该账号无法登录，操作将记入审计日志。`,
      '删除账号',
      { type: 'warning', confirmButtonText: '确认删除', cancelButtonText: '取消' }
    )
  } catch (e) {
    return
  }
  await deleteUser(row.id)
  ElMessage.success('账号已删除')
  loadUsers()
}

async function saveName() {
  const name = (profile.realName || '').trim()
  if (!name) {
    ElMessage.warning('请输入姓名')
    return
  }
  savingName.value = true
  try {
    await updateMyName({ realName: name })
    user.setRealName(name)
    ElMessage.success('姓名已保存')
  } finally {
    savingName.value = false
  }
}

function openRename(row) {
  renameForm.id = row.id
  renameForm.username = row.username
  renameForm.realName = row.realName || ''
  renameDialog.value = true
}

async function submitRename() {
  const name = (renameForm.realName || '').trim()
  if (!name) {
    ElMessage.warning('请输入姓名')
    return
  }
  renaming.value = true
  try {
    await updateUserName(renameForm.id, { realName: name })
    if (renameForm.username === user.username) {
      user.setRealName(name)
      profile.realName = name
    }
    ElMessage.success('姓名已修改')
    renameDialog.value = false
    loadUsers()
  } finally {
    renaming.value = false
  }
}

async function loadInvites() {
  const data = await pageInvites(inviteQ)
  invites.value = data.records || []
  inviteTotal.value = Number(data.total)
}

// 邀请码状态：已使用 > 已过期 > 未使用
const inviteText = (row) => {
  if (row.status === 1) return '已使用'
  if (row.expiresAt && new Date(row.expiresAt.replace(' ', 'T')) < new Date()) return '已过期'
  return '未使用'
}
const inviteType = (row) => ({ 已使用: 'info', 已过期: 'danger', 未使用: 'success' }[inviteText(row)])

async function openInvite() {
  inviteForm.roleCode = 'ASSOC'
  inviteForm.assocCode = ''
  createdCode.value = ''
  inviteDialog.value = true
  if (!associations.value.length) {
    associations.value = await allAssociation()
  }
}

function onInviteRoleChange() {
  if (inviteForm.roleCode !== 'ASSOC') inviteForm.assocCode = ''
}

async function submitInvite() {
  if (inviteForm.roleCode === 'ASSOC' && !inviteForm.assocCode) {
    ElMessage.warning('商协会管理员需选择加入的协会')
    return
  }
  inviting.value = true
  try {
    const data = await createInvite({
      roleCode: inviteForm.roleCode,
      assocCode: inviteForm.roleCode === 'ASSOC' ? inviteForm.assocCode : null
    })
    createdCode.value = data.code
    ElMessage.success('邀请码已创建，12 小时内有效')
    loadInvites()
  } finally {
    inviting.value = false
  }
}

async function copyCode() {
  try {
    await navigator.clipboard.writeText(createdCode.value)
    ElMessage.success('邀请码已复制')
  } catch (e) {
    ElMessage.warning('复制失败，请手动选中复制')
  }
}

onMounted(() => {
  if (user.isAdmin) {
    loadUsers()
    loadInvites()
  }
})
</script>

<style scoped>
.box { margin-bottom: 16px; }
.card-title { font-weight: 600; color: #1a2330; }
.pwd-form { max-width: 460px; }
.toolbar { display: flex; gap: 10px; margin-bottom: 12px; }
.muted { color: #909399; font-size: 12px; }
.card-head { display: flex; align-items: center; justify-content: space-between; }
.hint { margin-left: 12px; color: #909399; font-size: 12px; }
.created { margin-top: 12px; color: #303133; }
.code { font-family: Consolas, monospace; font-size: 18px; letter-spacing: 2px; color: #d4875a; font-weight: 700; }
</style>
