const api = require('../../utils/api')
const auth = require('../../utils/auth')

Page({
  data: {
    username: '',
    password: '',
    loading: false
  },

  onUsername(e) {
    this.setData({ username: e.detail.value })
  },

  onPassword(e) {
    this.setData({ password: e.detail.value })
  },

  async onLogin() {
    const { username, password } = this.data
    if (!username || !password) {
      wx.showToast({ title: '请输入账号和密码', icon: 'none' })
      return
    }
    this.setData({ loading: true })
    try {
      const data = await api.login({ username, password })
      auth.setAuth(data)
      wx.reLaunch({ url: '/pages/index/index' })
    } catch (e) {
      // 错误提示已在 request 内统一处理
    } finally {
      this.setData({ loading: false })
    }
  },

  onWechatLogin() {
    wx.login({
      success: async (res) => {
        if (!res.code) {
          wx.showToast({ title: '微信登录失败', icon: 'none' })
          return
        }
        try {
          const data = await api.wechatLogin({ code: res.code })
          auth.setAuth(data)
          wx.reLaunch({ url: '/pages/index/index' })
        } catch (e) {
          wx.showToast({ title: '该微信尚未绑定账号，请先用账号登录', icon: 'none' })
        }
      }
    })
  }
})
