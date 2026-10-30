// 工作台：填报入口 + 最近档案（SRS FR-04，小程序轻量化填报入口）
const { request } = require('../../utils/request')

Page({
  data: { recent: [], user: null },
  onShow() {
    const user = getApp().globalData.userInfo || wx.getStorageSync('userInfo')
    this.setData({ user })
    request({ url: '/archive/page', data: { page: 1, size: 5 } })
      .then((d) => this.setData({ recent: d.records || [] }))
      .catch(() => {})
  },
  goFill() { wx.switchTab({ url: '/pages/fill/fill' }) }
})
