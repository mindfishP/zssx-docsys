// 统一请求封装：自动带 JWT、统一错误提示、401 回登录页
const { BASE_URL } = require('../config')
const auth = require('./auth')

function request(options) {
  const showLoading = options.loading !== false
  if (showLoading) wx.showLoading({ title: '加载中', mask: true })
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      timeout: 15000,
      header: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + auth.getToken()
      },
      success(res) {
        const body = res.data || {}
        if (body.code === 200) {
          resolve(body.data)
        } else if (body.code === 401) {
          auth.clearAuth()
          wx.reLaunch({ url: '/pages/login/login' })
          reject(new Error(body.message || '登录已失效'))
        } else {
          wx.showToast({ title: body.message || '请求失败', icon: 'none' })
          reject(new Error(body.message || '请求失败'))
        }
      },
      fail(err) {
        wx.showToast({ title: '网络异常，请检查后端地址', icon: 'none' })
        reject(err)
      },
      complete() {
        if (showLoading) wx.hideLoading()
      }
    })
  })
}

function get(url, data, options) {
  return request(Object.assign({ url, data }, options || {}))
}

function post(url, data, options) {
  return request(Object.assign({ url, method: 'POST', data }, options || {}))
}

function put(url, data, options) {
  return request(Object.assign({ url, method: 'PUT', data }, options || {}))
}

function del(url, data, options) {
  return request(Object.assign({ url, method: 'DELETE', data }, options || {}))
}

/** 附件上传：multipart 直传，供填报页使用 */
function uploadFile(filePath, formData) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: BASE_URL + '/file/upload',
      filePath,
      name: 'file',
      formData: formData || {},
      header: { Authorization: 'Bearer ' + auth.getToken() },
      success(res) {
        let body = {}
        try {
          body = JSON.parse(res.data)
        } catch (e) {
          body = {}
        }
        if (body.code === 200) resolve(body.data)
        else reject(new Error(body.message || '上传失败'))
      },
      fail: reject
    })
  })
}

/** 借阅查阅/下载：带 token 下载到临时文件，返回临时路径 */
function downloadFile(signedPath) {
  return new Promise((resolve, reject) => {
    wx.downloadFile({
      url: signedPath.indexOf('http') === 0 ? signedPath : BASE_URL.replace('/api', '') + signedPath,
      header: { Authorization: 'Bearer ' + auth.getToken() },
      success(res) {
        if (res.statusCode === 200) resolve(res.tempFilePath)
        else reject(new Error('附件下载失败'))
      },
      fail: reject
    })
  })
}

module.exports = { request, get, post, put, del, uploadFile, downloadFile }
