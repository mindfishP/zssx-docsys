// 统一请求封装：自动带 JWT、统一错误提示、401 回登录页
const config = require('../config')
const auth = require('./auth')

let networkTipShown = false

/** 请求失败时给出可操作的提示（真机最常见的坑是 localhost 与域名校验） */
function notifyNetworkError(url) {
  if (networkTipShown) {
    wx.showToast({ title: '网络异常', icon: 'none' })
    return
  }
  networkTipShown = true
  wx.showModal({
    title: '无法连接服务器',
    content: '当前地址：' + url + '\n\n真机调试请把地址改成电脑的局域网 IP（如 http://192.168.1.10:8080/api），并确保手机与电脑在同一 Wi-Fi；开发者工具里请勾选“不校验合法域名”。',
    showCancel: false,
    confirmText: '我知道了'
  })
}

function request(options) {
  const base = config.getBaseUrl()
  const url = base + options.url
  const showLoading = options.loading !== false
  if (showLoading) wx.showLoading({ title: '加载中', mask: true })
  return new Promise((resolve, reject) => {
    wx.request({
      url,
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
        console.error('[request] 请求失败', url, err)
        notifyNetworkError(url)
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
  const url = config.getBaseUrl() + '/file/upload'
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url,
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
      fail(err) {
        console.error('[uploadFile] 上传失败', url, err)
        notifyNetworkError(url)
        reject(err)
      }
    })
  })
}

/** 借阅查阅/下载：带 token 下载到临时文件，返回临时路径 */
function downloadFile(signedPath) {
  const base = config.getBaseUrl()
  const origin = base.replace(/\/api\/?$/, '')
  const url = signedPath.indexOf('http') === 0 ? signedPath : origin + signedPath
  return new Promise((resolve, reject) => {
    wx.downloadFile({
      url,
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
