// 后端接口封装：与 Web 端共用同一套 REST 接口
const { get, post, put } = require('./request')

module.exports = {
  // 登录与个人
  login: (data) => post('/auth/login', data, { loading: true }),
  wechatLogin: (data) => post('/auth/wechat-login', data, { loading: true }),
  me: () => get('/auth/me'),
  updateName: (data) => put('/user/profile/name', data),
  changePassword: (data) => put('/user/password', data),

  // 档案
  archivePage: (params) => get('/archive/page', params),
  archiveDetail: (bizId) => get('/archive/' + bizId),

  // 政策推送
  pushPage: (params) => get('/push/page', params),
  pushDetail: (pushId) => get('/push/' + pushId),

  // 借阅
  borrowAvailable: (params) => get('/borrow/available', params),
  borrowPage: (params) => get('/borrow/page', params),
  borrowApply: (data) => post('/borrow/apply', data),

  // 统计与附件
  dashboard: () => get('/stats/dashboard'),
  activityList: () => get('/stats/activity'),
  signedViewUrl: (attachId) => get('/file/' + attachId + '/signed-view-url')
}
