// 档案填报（SRS 用例 UC-04 小程序侧）：分类选择 + 元数据 + 拍照/选文件上传
const { request, uploadFile } = require('../../utils/request')

Page({
  data: {
    majors: ['A 登记类', 'B 治理类', 'C 会员类', 'D 活动类', 'E 财务类', 'F 党建类'],
    majorIndex: -1,
    form: { categoryCode: '', title: '', archiveYear: '', responsibility: '' },
    files: [],
    submitting: false
  },
  onPickMajor(e) {
    const idx = Number(e.detail.value)
    this.setData({ majorIndex: idx, 'form.categoryCode': this.data.majors[idx].split(' ')[0] + '01' })
  },
  onInput(e) {
    const k = e.currentTarget.dataset.k
    this.setData({ ['form.' + k]: e.detail.value })
  },
  chooseImage() {
    wx.chooseMedia({
      count: 9,
      mediaType: ['image'],
      success: (res) => this.setData({ files: this.data.files.concat(res.tempFiles.map((f) => f.tempFilePath)) })
    })
  },
  removeFile(e) {
    const i = e.currentTarget.dataset.i
    const files = this.data.files.slice()
    files.splice(i, 1)
    this.setData({ files })
  },
  async submit(e) {
    const f = this.data.form
    const submit = e.currentTarget.dataset.submit === '1'
    if (!f.categoryCode || !f.title || !f.archiveYear) {
      wx.showToast({ title: '请填写分类/题名/年度', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      // 附件先传（登记哈希），再创建条目并回填 attachIds（SRS 5.4）
      const attachIds = []
      for (const p of this.data.files) {
        const att = await uploadFile(p, { majorCode: f.categoryCode.substring(0, 1) })
        attachIds.push(att.attachId)
      }
      const archive = await request({
        url: '/archive',
        method: 'POST',
        data: { ...f, attachIds, submit }
      })
      wx.showToast({ title: submit ? '已提交送审' : '已存草稿', icon: 'success' })
      this.setData({ form: { categoryCode: '', title: '', archiveYear: '', responsibility: '' }, files: [], majorIndex: -1 })
      if (archive && archive.bizId) { /* 保留：后续跳转详情 */ }
    } finally {
      this.setData({ submitting: false })
    }
  }
})
