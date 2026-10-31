const api = require('../../utils/api')
const auth = require('../../utils/auth')

const ROLES = { ADMIN: '主管部门操作员', LEADER: '主管领导', ASSOC: '商协会管理员' }

Page({
  data: { user: null, roleText: '', initial: '—', wechatBound: false },

  onShow() {
    if (!auth.ensureLogin()) return
    const user = auth.getUser() || {}
    this.setData({
      user,
      roleText: ROLES[user.roleCode] || user.roleCode || '—',
      initial: (user.realName || user.username || '?').slice(0, 1)
    })
    this.refreshMe()
    this.loadWechatStatus()
  },


  async loadWechatStatus() {
    try {
      const data = await api.wechatStatus()
      this.setData({ wechatBound: !!(data && data.bound) })
    } catch (e) {
      // 未登录时忽略
    }
  },

  /** 已绑定则确认解绑；未绑定则 wx.login 换取 code 后绑定 */
  toggleWechat() {
    if (this.data.wechatBound) {
      wx.showModal({
        title: '解绑微信',
        content: '解绑后该微信将无法一键登录本账号，可用账号密码重新绑定。',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await api.wechatUnbind()
            this.setData({ wechatBound: false })
            wx.showToast({ title: '已解绑', icon: 'success' })
          } catch (e) {
            // 统一提示
          }
        }
      })
      return
    }
    wx.login({
      success: async (res) => {
        if (!res.code) {
          wx.showToast({ title: '获取微信 code 失败', icon: 'none' })
          return
        }
        try {
          await api.wechatBind({ code: res.code })
          this.setData({ wechatBound: true })
          wx.showToast({ title: '绑定成功', icon: 'success' })
        } catch (e) {
          // 错误信息由 request 统一提示（如未配置 AppID、已被其他账号绑定）
        }
      },
      fail: () => wx.showToast({ title: '微信登录不可用', icon: 'none' })
    })
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
