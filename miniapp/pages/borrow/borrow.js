const api = require('../../utils/api')
const auth = require('../../utils/auth')
const fmt = require('../../utils/format')

Page({
  data: {
    tab: 'available',
    available: [],
    records: [],
    loading: false
  },

  onShow() {
    if (!auth.ensureLogin()) return
    this.load()
  },

  onPullDownRefresh() {
    this.load().finally(() => wx.stopPullDownRefresh())
  },

  onTab(e) {
    this.setData({ tab: e.currentTarget.dataset.tab })
    this.load()
  },

  async load() {
    this.setData({ loading: true })
    try {
      if (this.data.tab === 'available') {
        const data = await api.borrowAvailable({ page: 1, size: 20 })
        this.setData({ available: (data && data.records) || [] })
      } else {
        const data = await api.borrowPage({ page: 1, size: 20 })
        const rows = ((data && data.records) || []).map((item) => {
          const st = fmt.borrowStatus(item.borrowStatus)
          return Object.assign({}, item, {
            statusText: st.text,
            statusCls: st.cls,
            dueText: fmt.formatTime(item.dueAt)
          })
        })
        this.setData({ records: rows })
      }
    } catch (e) {
      // 统一提示
    } finally {
      this.setData({ loading: false })
    }
  },

  apply(e) {
    const bizId = e.currentTarget.dataset.id
    wx.showModal({
      title: '申请借阅',
      editable: true,
      placeholderText: '请填写借阅事由',
      success: async (res) => {
        if (!res.confirm) return
        const reason = (res.content || '').trim()
        if (!reason) {
          wx.showToast({ title: '借阅事由不能为空', icon: 'none' })
          return
        }
        const due = new Date(Date.now() + 7 * 24 * 3600 * 1000)
        const pad = (n) => (n < 10 ? '0' + n : '' + n)
        const dueAt = due.getFullYear() + '-' + pad(due.getMonth() + 1) + '-' + pad(due.getDate()) +
          'T' + pad(due.getHours()) + ':' + pad(due.getMinutes()) + ':00'
        try {
          await api.borrowApply({ bizId: Number(bizId), reason, dueAt })
          wx.showToast({ title: '已提交，等待审批', icon: 'success' })
          this.load()
        } catch (err) {
          // 统一提示
        }
      }
    })
  },

  openArchive(e) {
    wx.navigateTo({ url: '/pages/archive/detail/detail?bizId=' + e.currentTarget.dataset.id })
  }
})
