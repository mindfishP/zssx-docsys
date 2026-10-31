const api = require('../../../utils/api')
const req = require('../../../utils/request')
const auth = require('../../../utils/auth')
const fmt = require('../../../utils/format')

Page({
  data: {
    archive: null,
    attachments: [],
    reviews: [],
    statusText: '',
    statusCls: '',
    submitAt: '—',
    borrowed: false,
    canReview: false,
    canEdit: false
  },

  onLoad(query) {
    this.bizId = query.bizId
  },

  onShow() {
    this.loadDetail()
  },

  async loadDetail() {
    try {
      const detail = await api.archiveDetail(this.bizId)
      const archive = detail.archive || {}
      const access = detail.access || {}
      const st = fmt.archiveStatus(archive.status)
      const isAdmin = auth.isAdmin()
      const isAssoc = auth.isAssoc()
      this.setData({
        archive,
        statusText: st.text,
        statusCls: st.cls,
        submitAt: fmt.formatTime(archive.submitAt),
        borrowed: !!access.borrowed,
        canReview: isAdmin && archive.status === 1,
        canEdit: isAssoc && (archive.status === 0 || archive.status === 2),
        attachments: (detail.attachments || []).map((a) => Object.assign({}, a, {
          sizeText: fmt.fileSize(a.fileSize)
        })),
        reviews: (detail.reviews || []).map((r) => Object.assign({}, r, {
          timeText: fmt.formatTime(r.reviewAt)
        }))
      })
    } catch (e) {
      wx.showToast({ title: '档案加载失败', icon: 'none' })
    }
  },

  async openAttachment(e) {
    const attachId = e.currentTarget.dataset.id
    const fileName = e.currentTarget.dataset.name || '附件'
    const ext = (fileName.split('.').pop() || 'pdf').toLowerCase()
    try {
      const signed = await api.signedViewUrl(attachId)
      const tempPath = await req.downloadFile(signed)
      if (['png', 'jpg', 'jpeg', 'gif', 'webp'].indexOf(ext) >= 0) {
        wx.previewImage({ urls: [tempPath] })
        return
      }
      wx.openDocument({
        filePath: tempPath,
        fileType: ext,
        showMenu: false,
        fail: () => wx.showToast({ title: '该格式暂不支持预览', icon: 'none' })
      })
    } catch (err) {
      wx.showToast({ title: '附件查看失败', icon: 'none' })
    }
  },

  goEdit() {
    wx.navigateTo({ url: '/pages/fill/fill?bizId=' + this.bizId })
  },

  onPass() {
    this.review(true)
  },

  onReject() {
    this.review(false)
  },

  review(pass) {
    const title = pass ? '通过并归档' : '退回修改'
    wx.showModal({
      title,
      editable: true,
      placeholderText: pass ? '审核意见（可留空）' : '请填写退回原因',
      success: async (res) => {
        if (!res.confirm) return
        const opinion = (res.content || '').trim()
        if (!pass && !opinion) {
          wx.showToast({ title: '退回需填写原因', icon: 'none' })
          return
        }
        try {
          await api.archiveReview({ bizId: Number(this.bizId), pass, opinion })
          wx.showToast({ title: pass ? '已归档' : '已退回', icon: 'success' })
          setTimeout(() => wx.navigateBack(), 600)
        } catch (e) {
          // 统一提示
        }
      }
    })
  }
})
