const api = require('../../../utils/api')
const auth = require('../../../utils/auth')
const fmt = require('../../../utils/format')

const STATUS = {
  1: { text: '进行中', cls: 'tag-primary' },
  2: { text: '已结束', cls: 'tag-info' },
  0: { text: '已撤回', cls: 'tag-danger' }
}

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
      const data = await api.taskPage({ page: 1, size: 20 })
      const list = ((data && data.records) || []).map((t) => {
        const st = STATUS[t.status] || { text: '未知', cls: 'tag-info' }
        return Object.assign({}, t, {
          statusText: st.text,
          statusCls: st.cls,
          rangeText: t.endTime ? fmt.formatTime(t.startTime) + ' 至 ' + fmt.formatTime(t.endTime) : '—'
        })
      })
      this.setData({ list })
    } catch (e) {
      // 统一提示
    } finally {
      this.setData({ loading: false })
    }
  },

  openDetail(e) {
    wx.navigateTo({ url: '/pages/task/detail/detail?code=' + e.currentTarget.dataset.code })
  }
})
