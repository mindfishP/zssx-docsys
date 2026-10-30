const api = require('../../utils/api')
const auth = require('../../utils/auth')

Page({
  data: { oldPassword: '', newPassword: '', confirm: '', saving: false },

  onOld(e) { this.setData({ oldPassword: e.detail.value }) },
  onNew(e) { this.setData({ newPassword: e.detail.value }) },
  onConfirm(e) { this.setData({ confirm: e.detail.value }) },

  async save() {
    const { oldPassword, newPassword, confirm } = this.data
    if (!oldPassword || !newPassword) {
      wx.showToast({ title: '请填写完整', icon: 'none' })
      return
    }
    if (newPassword.length < 6) {
      wx.showToast({ title: '新密码至少 6 位', icon: 'none' })
      return
    }
    if (newPassword !== confirm) {
      wx.showToast({ title: '两次输入不一致', icon: 'none' })
      return
    }
    this.setData({ saving: true })
    try {
      await api.changePassword({ oldPassword, newPassword })
      wx.showToast({ title: '已修改，请重新登录', icon: 'success' })
      setTimeout(() => auth.logout(), 800)
    } catch (e) {
      // 统一提示
    } finally {
      this.setData({ saving: false })
    }
  }
})
