// 登录态、角色判断
const TOKEN_KEY = 'token'
const USER_KEY = 'user'

const ROLE_TEXT = { ADMIN: '主管部门操作员', LEADER: '主管领导', ASSOC: '商协会管理员' }

function getToken() {
  return wx.getStorageSync(TOKEN_KEY) || ''
}

function getUser() {
  return wx.getStorageSync(USER_KEY) || null
}

function setAuth(data) {
  wx.setStorageSync(TOKEN_KEY, data.token)
  wx.setStorageSync(USER_KEY, {
    id: data.id || data.userId,
    username: data.username,
    realName: data.realName,
    roleCode: data.roleCode,
    assocCode: data.assocCode
  })
}

function saveUser(user) {
  wx.setStorageSync(USER_KEY, user)
}

function clearAuth() {
  wx.removeStorageSync(TOKEN_KEY)
  wx.removeStorageSync(USER_KEY)
}

function roleCode() {
  const u = getUser()
  return u ? u.roleCode : ''
}

function roleText(code) {
  return ROLE_TEXT[code || roleCode()] || '未登录'
}

function isAssoc() {
  return roleCode() === 'ASSOC'
}

function isAdmin() {
  return roleCode() === 'ADMIN'
}

function isLeader() {
  return roleCode() === 'LEADER'
}

/** 主管部门与领导可看全量数据，协会只看本会 */
function isGlobal() {
  const code = roleCode()
  return code === 'ADMIN' || code === 'LEADER'
}

function ensureLogin() {
  if (getToken()) return true
  wx.reLaunch({ url: '/pages/login/login' })
  return false
}

function logout() {
  clearAuth()
  wx.reLaunch({ url: '/pages/login/login' })
}

module.exports = {
  getToken, getUser, setAuth, saveUser, clearAuth,
  roleCode, roleText, isAssoc, isAdmin, isLeader, isGlobal, ensureLogin, logout
}
