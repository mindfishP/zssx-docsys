// 微信登录（SRS 7.2）：wx.login 取 code，后端换 openid 绑定账号
const { request } = require('../../utils/request')

Page({
  data: { canLogin: true },
  onWxLogin() {
    wx.login({
      success: ({ code }) => {
        request({ url: '/auth/wechat-login', method: 'POST', data: { code } })
          .then((data) => {
            wx.setStorageSync('token', data.token)
            wx.setStorageSync('userInfo', data)
            getApp().globalData.userInfo = data
            wx.reLaunch({ url: '/pages/index/index' })
          })
          .catch(() => {
            wx.showModal({
              title: '提示',
              content: '该微信尚未绑定系统账号，请输入 Web 端账号口令完成绑定（框架阶段直接绑定）',
              showCancel: false
            })
          })
      }
    })
  }
})
