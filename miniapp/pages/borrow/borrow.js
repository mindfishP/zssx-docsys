const api = require('../../utils/api')
const auth = require('../../utils/auth')
const fmt = require('../../utils/format')

Page({
  data: {
    tab: 'available',
    isAssoc: false,
    isAdmin: false,
    available: [],
    records: [],
    recordsLabel: '借阅记录',
    loading: false
  },

  onShow() {
    if (!auth.ensureLogin()) return
    const isAssoc = auth.isAssoc()
    const isAdmin = auth.isAdmin()
    this.setData({
      isAssoc,
      isAdmin,
      tab: isAssoc ? 'available' : 'records',
      recordsLabel: isAssoc ? '本协会借阅记录' : '借阅记录'
    })
    wx.setNavigationBarTitle({ title: isAssoc ? '档案借阅' : '借阅管理' })
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

  approve(e) {
    const borrowId = e.currentTarget.dataset.id
    wx.showModal({
      title: '审批通过',
      editable: true,
      placeholderText: '审批意见（可留空）',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await api.borrowApprove({ borrowId: Number(borrowId), pass: true, opinion: (res.content || '').trim() })
          wx.showToast({ title: '已通过', icon: 'success' })
          this.load()
        } catch (err) {
          // 统一提示
        }
      }
    })
  },

  reject(e) {
    const borrowId = e.currentTarget.dataset.id
    wx.showModal({
      title: '拒绝申请',
      editable: true,
      placeholderText: '请填写拒绝原因',
      success: async (res) => {
        if (!res.confirm) return
        const opinion = (res.content || '').trim()
        if (!opinion) {
          wx.showToast({ title: '拒绝需填写原因', icon: 'none' })
          return
        }
        try {
          await api.borrowApprove({ borrowId: Number(borrowId), pass: false, opinion })
          wx.showToast({ title: '已拒绝', icon: 'success' })
          this.load()
        } catch (err) {
          // 统一提示
        }
      }
    })
  },

  returnBack(e) {
    const borrowId = e.currentTarget.dataset.id
    wx.showModal({
      title: '登记归还',
      content: '确认已归还该档案？归还后查阅权限立即收回。',
      success: async (res) => {
        if (!res.confirm) return
        try {
          await api.borrowReturn(Number(borrowId))
          wx.showToast({ title: '已登记归还', icon: 'success' })
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
