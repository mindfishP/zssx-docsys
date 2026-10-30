// 小程序入口：登录态检查（SRS 2.2 接入层）
App({
  onLaunch() {
    const token = wx.getStorageSync('token')
    if (!token) {
      wx.reLaunch({ url: '/pages/login/login' })
    }
  },
  globalData: {
    userInfo: wx.getStorageSync('userInfo') || null
  }
})
