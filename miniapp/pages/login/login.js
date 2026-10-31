const api = require('../../utils/api')
const auth = require('../../utils/auth')
const config = require('../../config')

Page({
  data: {
    username: '',
    password: '',
    loading: false,
    baseUrl: ''
  },

  onShow() {
    this.setData({ baseUrl: config.getBaseUrl() })
  },

  /** 真机调试时改成电脑局域网 IP */
  async editServer() {
    const url = await config.promptBaseUrl()
    this.setData({ baseUrl: url })
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
          // 具体原因（未绑定 / 未配置 AppID / 微信接口报错）已由 request 统一提示
        }
      },
      fail: () => wx.showToast({ title: '微信登录不可用', icon: 'none' })
    })
  }
})
