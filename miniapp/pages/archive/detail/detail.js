const api = require('../../../utils/api')
const req = require('../../../utils/request')
const fmt = require('../../../utils/format')

Page({
  data: {
    archive: null,
    attachments: [],
    reviews: [],
    statusText: '',
    statusCls: '',
    submitAt: '—',
    borrowed: false
  },

  onLoad(query) {
    this.bizId = query.bizId
    this.loadDetail()
  },

  async loadDetail() {
    try {
      const detail = await api.archiveDetail(this.bizId)
      const archive = detail.archive || {}
      const access = detail.access || {}
      const st = fmt.archiveStatus(archive.status)
      this.setData({
        archive,
        statusText: st.text,
        statusCls: st.cls,
        submitAt: fmt.formatTime(archive.submitAt),
        borrowed: !!access.borrowed,
        attachments: (detail.attachments || []).map((a) => Object.assign({}, a, {
          sizeText: fmt.fileSize(a.fileSize)
        })),
        reviews: detail.reviews || []
      })
    } catch (e) {
      wx.showToast({ title: '档案加载失败', icon: 'none' })
    }
  },

  async openAttachment(e) {
    const attachId = e.currentTarget.dataset.id
    const fileName = e.currentTarget.dataset.name || '附件'
    try {
      const path = await api.signedViewUrl(attachId)
      const tempPath = await req.downloadFile(path)
      wx.openDocument({
        filePath: tempPath,
        fileType: (fileName.split('.').pop() || 'pdf').toLowerCase(),
        showMenu: false,
        success: () => {},
        fail: () => wx.showToast({ title: '该格式暂不支持预览', icon: 'none' })
      })
    } catch (err) {
      wx.showToast({ title: '附件查看失败', icon: 'none' })
    }
  }
})
