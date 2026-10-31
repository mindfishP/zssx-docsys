const api = require('../../utils/api')
const auth = require('../../utils/auth')

Page({
  data: {
    bizId: null,
    editing: false,
    categories: [],
    categoryIndex: -1,
    majors: [],
    majorLabels: [],
    majorIndex: -1,
    categoryOptions: [],
    form: {
      title: '',
      archiveYear: '',
      responsibility: '',
      securityLevel: 1,
      retention: ''
    },
    levels: [{ label: '公开', value: 1 }, { label: '内部', value: 2 }],
    retentions: ['永久', '定期30年', '定期10年'],
    retentionIndex: -1,
    files: [],
    submitting: false,
    loading: true
  },

  onLoad(query) {
    const year = new Date().getFullYear()
    const bizId = query.bizId ? Number(query.bizId) : null
    this.setData({
      bizId,
      editing: !!bizId,
      'form.archiveYear': String(year)
    })
    wx.setNavigationBarTitle({ title: bizId ? '修改档案' : '填报档案' })
    this.init()
  },

  async init() {
    if (!auth.ensureLogin()) return
    if (!auth.isAssoc()) {
      wx.showModal({ title: '无权限', content: '仅商协会管理员可填报档案', showCancel: false, success: () => wx.navigateBack() })
      return
    }
    try {
      const list = await api.categoryList()
      const categories = (list || []).map((c) => ({
        label: c.majorName + ' / ' + c.subName + '（' + c.categoryCode + '）',
        code: c.categoryCode,
        majorCode: c.majorCode,
        defaultLevel: c.defaultSecurityLevel,
        defaultRetention: c.defaultRetention
      }))
      const majorNames = { A: '登记类', B: '治理类', C: '会员类', D: '活动类', E: '财务类', F: '党建类' }
      const majors = []
      categories.forEach((c) => {
        if (majors.indexOf(c.majorCode) < 0) majors.push(c.majorCode)
      })
      const majorLabels = majors.map((code) => code + ' ' + (majorNames[code] || ''))
      this.setData({ categories, majors, majorLabels, loading: false })
      if (this.data.editing) {
        await this.loadArchive()
      } else {
        this.setData({ majorIndex: -1 })
      }
    } catch (e) {
      this.setData({ loading: false })
    }
  },

  async loadArchive() {
    try {
      const detail = await api.archiveDetail(this.data.bizId)
      const a = detail.archive || {}
      const majorIndex = this.data.majors.indexOf(a.majorCode)
      const options = this.data.categories.filter((c) => c.majorCode === a.majorCode)
      const categoryIndex = options.findIndex((c) => c.code === a.categoryCode)
      this.setData({
        majorIndex,
        categoryOptions: options,
        categoryIndex,
        'form.title': a.title || '',
        'form.archiveYear': a.archiveYear || '',
        'form.responsibility': a.responsibility || '',
        'form.securityLevel': a.securityLevel || 1,
        'form.retention': a.retention || '',
        retentionIndex: this.data.retentions.indexOf(a.retention || ''),
        files: (detail.attachments || []).map((x) => ({
          attachId: x.attachId,
          fileName: x.fileName,
          isNew: false
        }))
      })
      if (a.status !== 0 && a.status !== 2) {
        wx.showToast({ title: '仅草稿或退回件可修改', icon: 'none' })
        setTimeout(() => wx.navigateBack(), 1200)
      }
    } catch (e) {
      // 统一提示
    }
  },

  onMajorChange(e) {
    const majorIndex = Number(e.detail.value)
    const majorCode = this.data.majors[majorIndex]
    const options = this.data.categories.filter((c) => c.majorCode === majorCode)
    this.setData({ majorIndex, categoryOptions: options, categoryIndex: -1, 'form.retention': '', retentionIndex: -1 })
  },

  onCategoryChange(e) {
    const index = Number(e.detail.value)
    const cat = this.data.categoryOptions[index]
    if (!cat) return
    this.setData({
      categoryIndex: index,
      'form.securityLevel': cat.defaultLevel || 1,
      'form.retention': cat.defaultRetention || '',
      retentionIndex: this.data.retentions.indexOf(cat.defaultRetention || '')
    })
  },

  onInput(e) {
    const key = e.currentTarget.dataset.k
    const value = e.detail.value
    this.setData({ ['form.' + key]: value })
  },

  onLevelChange(e) {
    this.setData({ 'form.securityLevel': Number(e.detail.value) })
  },

  onRetentionChange(e) {
    const index = Number(e.detail.value)
    this.setData({ retentionIndex: index, 'form.retention': this.data.retentions[index] })
  },

  chooseMedia() {
    wx.chooseMedia({
      count: 9,
      mediaType: ['image'],
      success: (res) => {
        res.tempFiles.forEach((f) => this.upload(f.tempFilePath, 'A'))
      }
    })
  },

  chooseFile() {
    wx.chooseMessageFile({
      count: 5,
      type: 'file',
      extension: ['pdf', 'doc', 'docx', 'xls', 'xlsx', 'ppt', 'pptx', 'zip'],
      success: (res) => {
        res.tempFiles.forEach((f) => this.upload(f.path, 'A'))
      }
    })
  },

  async upload(filePath, majorCode) {
    const major = this.selectedCategory() ? this.selectedCategory().majorCode : majorCode
    wx.showLoading({ title: '上传中', mask: true })
    try {
      const data = await api.uploadFile(filePath, { majorCode: major })
      const files = this.data.files.concat([{
        attachId: data.attachId,
        fileName: data.fileName,
        isNew: true
      }])
      this.setData({ files })
    } catch (e) {
      wx.showToast({ title: '上传失败', icon: 'none' })
    } finally {
      wx.hideLoading()
    }
  },

  removeFile(e) {
    const index = Number(e.currentTarget.dataset.index)
    const file = this.data.files[index]
    const remove = () => {
      const files = this.data.files.slice()
      files.splice(index, 1)
      this.setData({ files })
    }
    if (file.isNew) {
      api.deleteAttachment(file.attachId).catch(() => {}).finally(remove)
      return
    }
    wx.showModal({
      title: '删除附件',
      content: '确认删除已上传的附件？',
      success: (res) => {
        if (!res.confirm) return
        api.deleteAttachment(file.attachId).then(remove).catch(() => {})
      }
    })
  },

  selectedCategory() {
    const { categoryOptions, categoryIndex } = this.data
    if (categoryIndex < 0) return null
    return categoryOptions[categoryIndex] || null
  },

  async onSubmit(e) {
    const submit = e.currentTarget.dataset.submit === 'true'
    const cat = this.selectedCategory()
    if (!cat) {
      wx.showToast({ title: '请选择档案分类', icon: 'none' })
      return
    }
    const form = this.data.form
    if (!form.title.trim()) {
      wx.showToast({ title: '请填写题名', icon: 'none' })
      return
    }
    if (!/^\d{4}$/.test(form.archiveYear || '')) {
      wx.showToast({ title: '年度应为 4 位年份', icon: 'none' })
      return
    }
    const payload = {
      categoryCode: cat.code,
      title: form.title.trim(),
      archiveYear: form.archiveYear,
      responsibility: form.responsibility.trim(),
      securityLevel: form.securityLevel,
      retention: form.retention || null,
      attachIds: this.data.files.filter((f) => f.isNew).map((f) => f.attachId),
      submit
    }
    this.setData({ submitting: true })
    try {
      if (this.data.editing) await api.archiveUpdate(this.data.bizId, payload)
      else await api.archiveCreate(payload)
      wx.showToast({ title: submit ? '已提交送审' : '已保存草稿', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 700)
    } catch (err) {
      // 统一提示
    } finally {
      this.setData({ submitting: false })
    }
  }
})
