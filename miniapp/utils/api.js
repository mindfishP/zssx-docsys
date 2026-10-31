// 后端接口封装：与 Web 端共用同一套 REST 接口
const { get, post, put, del, uploadFile, downloadFile } = require('./request')

module.exports = {
  // 登录与个人
  login: (data) => post('/auth/login', data, { loading: true }),
  wechatLogin: (data) => post('/auth/wechat-login', data, { loading: true }),
  wechatBind: (data) => post('/auth/wechat/bind', data, { loading: true }),
  wechatUnbind: () => post('/auth/wechat/unbind', {}, { loading: true }),
  wechatStatus: () => get('/auth/wechat/status', {}, { loading: false }),
  me: () => get('/auth/me'),
  updateName: (data) => put('/user/profile/name', data),
  changePassword: (data) => put('/user/password', data),

  // 档案
  categoryList: () => get('/category/list', {}, { loading: false }),
  archivePage: (params) => get('/archive/page', params),
  archiveDetail: (bizId) => get('/archive/' + bizId),
  archiveCreate: (data) => post('/archive', data),
  archiveUpdate: (bizId, data) => put('/archive/' + bizId, data),
  archiveReview: (data) => post('/archive/review', data),

  // 政策推送
  pushPage: (params) => get('/push/page', params),
  pushDetail: (pushId) => get('/push/' + pushId),

  // 借阅
  borrowAvailable: (params) => get('/borrow/available', params),
  borrowPage: (params) => get('/borrow/page', params),
  borrowApply: (data) => post('/borrow/apply', data),
  borrowApprove: (data) => post('/borrow/approve', data),
  borrowReturn: (borrowId) => post('/borrow/' + borrowId + '/return'),

  // 专项补录
  taskPage: (params) => get('/task/page', params),
  taskDetail: (code) => get('/task/' + code),

  // 统计与附件
  dashboard: () => get('/stats/dashboard'),
  activityList: () => get('/stats/activity'),
  signedViewUrl: (attachId) => get('/file/' + attachId + '/signed-view-url'),
  deleteAttachment: (attachId) => del('/file/' + attachId),
  uploadFile,
  downloadFile
}
