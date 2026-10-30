<template>
  <div class="login-page">
    <el-card class="card">
      <h2 class="title">中山商协会档案资料收集与服务系统</h2>
      <p class="sub">Web 管理后台 · 需求规格说明书 V1.0 对应实现</p>
      <el-form :model="form" @keyup.enter="onLogin">
        <el-form-item>
          <el-input v-model="form.username" placeholder="账号（admin / leader / A001）" size="large" />
        </el-form-item>
        <el-form-item>
          <el-input v-model="form.password" type="password" placeholder="口令（初始 123456）" size="large" show-password />
        </el-form-item>
        <el-button type="primary" size="large" style="width: 100%" :loading="loading" @click="onLogin">
          登 录
        </el-button>
      </el-form>
      <div class="links">
        <el-link type="primary" :underline="false" @click="openRegister">使用邀请码注册账号</el-link>
      </div>
    </el-card>

    <!-- 注册：账号 + 口令 + 管理员发放的邀请码 -->
    <el-dialog v-model="regDialog" title="注册账号" width="440px">
      <el-alert type="info" :closable="false" style="margin-bottom: 14px"
        title="邀请码由主管部门在“账号管理”中发放，12 小时内有效，决定了注册后的角色与所属协会。" />
      <el-form :model="reg" label-width="90px">
        <el-form-item label="账号">
          <el-input v-model="reg.username" placeholder="3-32 位，不能与已有账号重复" />
        </el-form-item>
        <el-form-item label="口令">
          <el-input v-model="reg.password" type="password" show-password placeholder="6-32 位" />
        </el-form-item>
        <el-form-item label="确认口令">
          <el-input v-model="reg.confirm" type="password" show-password />
        </el-form-item>
        <el-form-item label="邀请码">
          <el-input v-model="reg.inviteCode" placeholder="8 位邀请码" @keyup.enter="onRegister" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="regDialog = false">取消</el-button>
        <el-button type="primary" :loading="registering" @click="onRegister">注 册</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, register } from '../../api'
import { useUserStore } from '../../store/user'

const router = useRouter()
const user = useUserStore()
const loading = ref(false)
const form = reactive({ username: '', password: '' })

const regDialog = ref(false)
const registering = ref(false)
const reg = reactive({ username: '', password: '', confirm: '', inviteCode: '' })

async function onLogin() {
  if (!form.username || !form.password) {
    ElMessage.warning('请输入账号与口令')
    return
  }
  loading.value = true
  try {
    const data = await login(form)
    user.save(data)
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}

function openRegister() {
  Object.assign(reg, { username: '', password: '', confirm: '', inviteCode: '' })
  regDialog.value = true
}

async function onRegister() {
  if (!reg.username || !reg.password || !reg.inviteCode) {
    ElMessage.warning('账号、密码、邀请码均为必填')
    return
  }
  if (reg.username.length < 3) {
    ElMessage.warning('账号至少 3 位')
    return
  }
  if (reg.password.length < 6) {
    ElMessage.warning('密码至少 6 位')
    return
  }
  if (reg.password !== reg.confirm) {
    ElMessage.warning('两次输入的密码不一致')
    return
  }
  registering.value = true
  try {
    await register({
      username: reg.username.trim(),
      password: reg.password,
      inviteCode: reg.inviteCode.trim()
    })
    ElMessage.success('注册成功，请使用账号登录')
    form.username = reg.username.trim()
    form.password = ''
    regDialog.value = false
  } finally {
    registering.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #1a2330 0%, #2c3e50 100%);
}
.card { width: 400px; padding: 10px 20px; }
.title { text-align: center; color: #1a2330; margin-bottom: 4px; }
.sub { text-align: center; color: #909399; font-size: 12px; margin-bottom: 24px; }
.links { text-align: center; margin-top: 14px; }
</style>
