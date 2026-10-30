import axios from 'axios'
import request from '../utils/request'

// 认证
export const login = (data) => request.post('/auth/login', data)
export const register = (data) => request.post('/auth/register', data)
export const fetchMe = () => request.get('/auth/me')

// 注册邀请码（主管部门）
export const createInvite = (data) => request.post('/invite', data)
export const pageInvites = (params) => request.get('/invite/page', { params })

// 账号管理
export const changePassword = (data) => request.put('/user/password', data)
export const updateMyName = (data) => request.put('/user/profile/name', data)
export const updateUserName = (id, data) => request.put(`/user/${id}/name`, data)
export const pageUsers = (params) => request.get('/user/page', { params })
export const deleteUser = (id) => request.delete(`/user/${id}`)

// 商协会
export const pageAssociation = (params) => request.get('/association/page', { params })
export const allAssociation = () => request.get('/association/all')
export const createAssociation = (data) => request.post('/association', data)
export const updateAssociation = (code, data) => request.put(`/association/${code}`, data)
export const getAssociation = (code) => request.get(`/association/${code}`)
export const getAssociationUsers = (code) => request.get(`/association/${code}/users`)

// 档案分类
export const listCategory = () => request.get('/category/list')

// 档案条目
export const pageArchive = (params) => request.get('/archive/page', { params })
export const getArchiveDetail = (bizId) => request.get(`/archive/${bizId}`)
export const createArchive = (data) => request.post('/archive', data)
export const updateArchive = (bizId, data) => request.put(`/archive/${bizId}`, data)
export const importArchives = (formData) =>
  request.post('/archive/import', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
// 模板下载需带 JWT，用裸 axios 取二进制后由页面另存
export const downloadImportTemplate = async () => {
  const token = localStorage.getItem('token')
  const res = await axios.get('/api/archive/import-template', {
    responseType: 'blob',
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  return res.data
}
export const deleteArchive = (bizId) => request.delete(`/archive/${bizId}`)
export const resubmitArchive = (bizId, data) => request.put(`/archive/${bizId}/resubmit`, data)
export const reviewArchive = (data) => request.post('/archive/review', data)

// 附件
export const getSignedUrl = (attachId) => request.get(`/file/${attachId}/signed-url`)
// 借阅查阅：仅在线查看，不提供下载
export const getSignedViewUrl = (attachId) => request.get(`/file/${attachId}/signed-view-url`)
export const uploadFile = (formData) =>
  request.post('/file/upload', formData, { headers: { 'Content-Type': 'multipart/form-data' } })
export const deleteAttachment = (attachId) => request.delete(`/file/${attachId}`)
// 附件二进制拉取：签名地址用裸 axios，避免响应拦截器按 JSON 解析
export const fetchAttachment = async (signedPath) => {
  const token = localStorage.getItem('token')
  const res = await axios.get(signedPath, {
    responseType: 'blob',
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  })
  return res.data
}

// 借阅
export const pageBorrow = (params) => request.get('/borrow/page', { params })
export const borrowableArchives = (params) => request.get('/borrow/available', { params })
export const applyBorrow = (data) => request.post('/borrow/apply', data)
export const approveBorrow = (data) => request.post('/borrow/approve', data)
export const returnBorrow = (borrowId) => request.post(`/borrow/${borrowId}/return`)

// 专项补录
export const pageTask = (params) => request.get('/task/page', { params })
export const createTask = (data) => request.post('/task', data)
export const getTask = (taskCode) => request.get(`/task/${taskCode}`)

// 政策推送
export const pagePush = (params) => request.get('/push/page', { params })
export const publishPush = (data) => request.post('/push', data)
export const getPushDetail = (pushId) => request.get(`/push/${pushId}`)

// 统计
export const dashboard = () => request.get('/stats/dashboard')
export const activityList = () => request.get('/stats/activity')
export const evaluateActivity = () => request.post('/stats/activity/evaluate')

// 审计日志
export const pageAuditLog = (params) => request.get('/audit-log/page', { params })
