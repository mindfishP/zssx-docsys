// 消息：政策推送 + 审核结果通知（SRS FR-09-03 接收侧）
const { request } = require('../../utils/request')

Page({
  data: { pushes: [] },
  onShow() {
    request({ url: '/push/page', data: { page: 1, size: 20 } })
      .then((d) => this.setData({ pushes: d.records || [] }))
      .catch(() => {})
  }
})
