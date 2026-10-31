const api = require('../../utils/api')
const auth = require('../../utils/auth')
const fmt = require('../../utils/format')

Page({
  data: {
    user: null,
    roleText: '',
    levelText: '未评估',
    isAssoc: false,
    isAdmin: false,
    isLeader: false,
    entries: [],
    stats: {},
    statItems: [],
    recent: []
  },

  onShow() {
    if (!auth.ensureLogin()) return
    const user = auth.getUser() || {}
    const isAssoc = auth.isAssoc()
    const isAdmin = auth.isAdmin()
    const isLeader = auth.isLeader()
    let entries
    if (isAssoc) {
      entries = [
        { key: 'fill', icon: '＋', text: '填报档案' },
        { key: 'push', icon: '✉', text: '政策推送' },
        { key: 'borrow', icon: '⇄', text: '档案借阅' },
        { key: 'task', icon: '☑', text: '专项补录' }
      ]
    } else if (isAdmin) {
      entries = [
        { key: 'review', icon: '✓', text: '审核归档' },
        { key: 'borrow', icon: '⇄', text: '借阅审批' },
        { key: 'push', icon: '✉', text: '政策推送' },
        { key: 'task', icon: '☑', text: '专项补录' }
      ]
    } else {
      entries = [
        { key: 'archive', icon: '档', text: '档案查阅' },
        { key: 'borrow', icon: '⇄', text: '借阅记录' },
        { key: 'push', icon: '✉', text: '政策推送' },
        { key: 'task', icon: '☑', text: '专项补录' }
      ]
    }
    this.setData({ user, roleText: auth.roleText(), isAssoc, isAdmin, isLeader, entries })
    this.loadData()
  },

  async loadData() {
    const isAssoc = auth.isAssoc()
    try {
      const [stats, pending, archived, pendingBorrow, assocs, all, activity] = await Promise.all([
        api.dashboard(),
        api.archivePage({ page: 1, size: 1, status: 1 }),
        api.archivePage({ page: 1, size: 1, status: 3 }),
        api.borrowPage({ page: 1, size: 1, status: 0 }),
        api.archivePage({ page: 1, size: 1 }),
        api.archivePage({ page: 1, size: 3 }),
        api.activityList().catch(() => [])
      ])
      const statItems = isAssoc
        ? [
            { label: '档案总数', value: Number((all && all.total) || 0) },
            { label: '待审核', value: Number((pending && pending.total) || 0) },
            { label: '已归档', value: Number((archived && archived.total) || 0) }
          ]
        : [
            { label: '待审核档案', value: Number((pending && pending.total) || 0) },
            { label: '借阅待审批', value: Number((pendingBorrow && pendingBorrow.total) || 0) },
            { label: '已归档档案', value: Number((archived && archived.total) || 0) }
          ]
      const own = isAssoc && activity && activity.length ? activity[0] : null
      this.setData({
        stats: stats || {},
        statItems,
        levelText: own ? (own.levelText || fmt.levelText(own.level)) : '',
        recent: ((all && all.records) || []).map((item) => {
          const st = fmt.archiveStatus(item.status)
          return Object.assign({}, item, { statusText: st.text, statusCls: st.cls })
        })
      })
    } catch (e) {
      // 统一提示
    }
  },

  onEntry(e) {
    const key = e.currentTarget.dataset.key
    if (key === 'fill') wx.navigateTo({ url: '/pages/fill/fill' })
    if (key === 'push') wx.navigateTo({ url: '/pages/push/list/list' })
    if (key === 'task') wx.navigateTo({ url: '/pages/task/list/list' })
    if (key === 'archive') wx.switchTab({ url: '/pages/archive/list/list' })
    if (key === 'review') wx.switchTab({ url: '/pages/archive/list/list' })
    if (key === 'borrow') wx.switchTab({ url: '/pages/borrow/borrow' })
  },

  openDetail(e) {
    wx.navigateTo({ url: '/pages/archive/detail/detail?bizId=' + e.currentTarget.dataset.id })
  },

  goAll() {
    wx.switchTab({ url: '/pages/archive/list/list' })
  }
})
