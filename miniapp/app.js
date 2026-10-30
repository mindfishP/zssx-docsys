// 小程序入口：恢复登录态、记录全局用户信息
const auth = require('./utils/auth')

App({
  globalData: {
    user: null
  },
  onLaunch() {
    this.globalData.user = auth.getUser()
  }
})
