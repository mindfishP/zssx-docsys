const api = require('../../../utils/api')

const SCOPE = { ALL: '全部协会', LIST: '指定协会', INDUSTRY: '按行业类别', LEVEL: '按活跃度分档' }

Page({
  data: { detail: null, scopeLabel: '—', targetsText: '—' },

  onLoad(query) {
    this.pushId = query.id
    this.loadDetail()
  },

  async loadDetail() {
    try {
      const detail = await api.pushDetail(this.pushId)
      let targets = []
      try {
        const parsed = JSON.parse(detail.targetValue || '[]')
        targets = Array.isArray(parsed) ? parsed : []
      } catch (e) {
        targets = []
      }
      this.setData({
        detail,
        scopeLabel: SCOPE[detail.targetScope] || detail.targetScope || '—',
        targetsText: detail.targetScope === 'ALL' ? '全体商协会' : (targets.join('、') || '—')
      })
    } catch (e) {
      wx.showToast({ title: '推送加载失败', icon: 'none' })
    }
  }
})
