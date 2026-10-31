const api = require('../../../utils/api')
const auth = require('../../../utils/auth')
const fmt = require('../../../utils/format')

Page({
  data: {
    tabs: [],
    status: '',
    title: '档案管理',
    global: false,
    list: [],
    page: 1,
    size: 10,
    hasMore: true,
    loading: false
  },

  onLoad() {
    const isAdmin = auth.isAdmin()
    const isLeader = auth.isLeader()
    const global = isAdmin || isLeader
    const tabs = global
      ? [
          { label: '待审核', value: 1 },
          { label: '全部', value: '' },
          { label: '已归档', value: 3 },
          { label: '退回', value: 2 },
          { label: '草稿', value: 0 }
        ]
      : [
          { label: '全部', value: '' },
          { label: '草稿', value: 0 },
          { label: '待审核', value: 1 },
          { label: '退回', value: 2 },
          { label: '已归档', value: 3 }
        ]
    this.setData({
      tabs,
      status: global ? 1 : '',
      global,
      title: isAdmin ? '审核归档' : (isLeader ? '档案查阅' : '本协会档案')
    })
    wx.setNavigationBarTitle({ title: this.data.title })
  },

  onShow() {
    if (!auth.ensureLogin()) return
    this.refresh()
  },

  onPullDownRefresh() {
    this.refresh().finally(() => wx.stopPullDownRefresh())
  },

  onReachBottom() {
    this.loadMore()
  },

  async refresh() {
    this.setData({ page: 1, hasMore: true })
    return this.load(true)
  },

  async load(reset) {
    if (this.data.loading) return
    this.setData({ loading: true })
    try {
      const params = { page: this.data.page, size: this.data.size }
      if (this.data.status !== '') params.status = this.data.status
      const data = await api.archivePage(params)
      const rows = ((data && data.records) || []).map((item) => {
        const st = fmt.archiveStatus(item.status)
        return Object.assign({}, item, { statusText: st.text, statusCls: st.cls })
      })
      const list = reset ? rows : this.data.list.concat(rows)
      this.setData({ list, hasMore: list.length < Number((data && data.total) || 0) })
    } catch (e) {
      // 统一提示
    } finally {
      this.setData({ loading: false })
    }
  },

  loadMore() {
    if (!this.data.hasMore || this.data.loading) return
    this.setData({ page: this.data.page + 1 })
    this.load(false)
  },

  onTab(e) {
    this.setData({ status: e.currentTarget.dataset.value })
    this.refresh()
  },

  openDetail(e) {
    wx.navigateTo({ url: '/pages/archive/detail/detail?bizId=' + e.currentTarget.dataset.id })
  }
})
