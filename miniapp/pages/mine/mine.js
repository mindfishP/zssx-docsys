// 我的：账号信息与退出（SRS FR-01-06）
Page({
  data: { user: null },
  onShow() {
    this.setData({ user: wx.getStorageSync('userInfo') })
  },
  logout() {
    wx.removeStorageSync('token')
    wx.removeStorageSync('userInfo')
    wx.reLaunch({ url: '/pages/login/login' })
  }
})
