// 登录态与身份工具
const TOKEN_KEY = 'token'
const USER_KEY = 'user'

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

function isAssoc() {
  const u = getUser()
  return !!u && u.roleCode === 'ASSOC'
}

function isAdmin() {
  const u = getUser()
  return !!u && u.roleCode === 'ADMIN'
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

module.exports = { getToken, getUser, setAuth, saveUser, clearAuth, isAssoc, isAdmin, ensureLogin, logout }
