const api = require('../../../utils/api')
const fmt = require('../../../utils/format')

const SCOPE = { ALL: '全部协会', LIST: '指定协会', INDUSTRY: '按行业类别' }
const MAJOR = { A: '登记类', B: '治理类', C: '会员类', D: '活动类', E: '财务类', F: '党建类' }

Page({
  data: { task: null, scopeText: '—', majorText: '不限', rangeText: '—' },

  onLoad(query) {
    this.taskCode = query.code
    this.load()
  },

  async load() {
    try {
      const task = await api.taskDetail(this.taskCode)
      let targets = []
      try {
        const parsed = JSON.parse(task.targetValue || '[]')
        targets = Array.isArray(parsed) ? parsed : []
      } catch (e) {
        targets = []
      }
      const scope = SCOPE[task.targetScope] || task.targetScope || '—'
      const scopeText = task.targetScope === 'ALL' ? '全部协会' : scope + '：' + (targets.join('、') || '—')
      const majorText = (task.majorCodes || '').split(',').filter(Boolean)
        .map((c) => c + ' ' + (MAJOR[c] || '')).join('、') || '不限'
      this.setData({
        task,
        scopeText,
        majorText,
        rangeText: fmt.formatTime(task.startTime) + ' 至 ' + fmt.formatTime(task.endTime)
      })
    } catch (e) {
      wx.showToast({ title: '任务加载失败', icon: 'none' })
    }
  }
})
