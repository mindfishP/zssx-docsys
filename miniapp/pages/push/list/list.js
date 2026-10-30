const api = require('../../../utils/api')
const auth = require('../../../utils/auth')

Page({
  data: { list: [], loading: false },

  onShow() {
    if (!auth.ensureLogin()) return
    this.load()
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh())
  },

  async load() {
    this.setData({ loading: true })
    try {
      const data = await api.pushPage({ page: 1, size: 20 })
      this.setData({ list: (data && data.records) || [] })
    } catch (e) {
      // 统一提示
    } finally {
      this.setData({ loading: false })
    }
  },

  openDetail(e) {
    wx.navigateTo({ url: '/pages/push/detail/detail?id=' + e.currentTarget.dataset.id })
  }
})
