const api = require('../../utils/api')
const auth = require('../../utils/auth')
const fmt = require('../../utils/format')

Page({
  data: {
    user: null,
    levelText: '未评估',
    stats: {},
    pendingCount: 0,
    archivedCount: 0,
    recent: []
  },

  onShow() {
    if (!auth.ensureLogin()) return
    this.setData({ user: auth.getUser() })
    this.loadData()
  },

  async loadData() {
    try {
      const [stats, pending, archived, all, activity] = await Promise.all([
        api.dashboard(),
        api.archivePage({ page: 1, size: 1, status: 1 }),
        api.archivePage({ page: 1, size: 1, status: 3 }),
        api.archivePage({ page: 1, size: 3 }),
        api.activityList().catch(() => [])
      ])
      const own = (activity && activity.length) ? activity[0] : null
      this.setData({
        stats: stats || {},
        pendingCount: Number((pending && pending.total) || 0),
        archivedCount: Number((archived && archived.total) || 0),
        levelText: own ? (own.levelText || fmt.levelText(own.level)) : '未评估',
        recent: ((all && all.records) || []).map((item) => {
          const st = fmt.archiveStatus(item.status)
          return Object.assign({}, item, { statusText: st.text, statusCls: st.cls })
        })
      })
    } catch (e) {
      // 统一提示
    }
  },

  goArchive() {
    wx.switchTab({ url: '/pages/archive/list/list' })
  },
  goBorrow() {
    wx.switchTab({ url: '/pages/borrow/borrow' })
  },
  goPush() {
    wx.navigateTo({ url: '/pages/push/list/list' })
  },
  goFill() {
    wx.navigateTo({ url: '/pages/fill/fill' })
  },
  openDetail(e) {
    wx.navigateTo({ url: '/pages/archive/detail/detail?bizId=' + e.currentTarget.dataset.id })
  }
})
