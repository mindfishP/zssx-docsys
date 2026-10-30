const api = require('../../utils/api')
const auth = require('../../utils/auth')

const ROLES = { ADMIN: '主管部门操作员', LEADER: '主管领导', ASSOC: '商协会管理员' }

Page({
  data: { user: null, roleText: '', initial: '—' },

  onShow() {
    if (!auth.ensureLogin()) return
    const user = auth.getUser() || {}
    this.setData({
      user,
      roleText: ROLES[user.roleCode] || user.roleCode || '—',
      initial: (user.realName || user.username || '?').slice(0, 1)
    })
    this.refreshMe()
  },

  async refreshMe() {
    try {
      const me = await api.me()
      if (me) {
        const user = Object.assign({}, auth.getUser(), me)
        auth.saveUser(user)
        this.setData({
          user,
          roleText: ROLES[user.roleCode] || user.roleCode || '—',
          initial: (user.realName || user.username || '?').slice(0, 1)
        })
      }
    } catch (e) {
      // 统一提示
    }
  },

  editName() {
    wx.showModal({
      title: '修改姓名',
      editable: true,
      placeholderText: '请输入真实姓名',
      success: async (res) => {
        if (!res.confirm) return
        const realName = (res.content || '').trim()
        if (!realName) {
          wx.showToast({ title: '姓名不能为空', icon: 'none' })
          return
        }
        try {
          await api.updateName({ realName })
          this.refreshMe()
          wx.showToast({ title: '已修改', icon: 'success' })
        } catch (e) {
          // 统一提示
        }
      }
    })
  },

  changePassword() {
    wx.navigateTo({ url: '/pages/mine/password' })
  },

  about() {
    wx.showModal({
      title: '关于',
      content: '中山市商协会档案资料收集与服务系统（移动端）\n毕业设计项目 v1.0',
      showCancel: false
    })
  },

  logout() {
    wx.showModal({
      title: '提示',
      content: '确认退出登录？',
      success: (res) => {
        if (res.confirm) auth.logout()
      }
    })
  }
})
