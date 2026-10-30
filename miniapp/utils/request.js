// 统一请求封装（SRS 7.2）：携带 JWT，401 自动跳登录
const BASE_URL = 'http://localhost:8080/api'

function request(options) {
  return new Promise((resolve, reject) => {
    wx.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data: options.data || {},
      header: {
        'Content-Type': 'application/json',
        Authorization: 'Bearer ' + (wx.getStorageSync('token') || '')
      },
      success(res) {
        const { code, message, data } = res.data
        if (code === 200) {
          resolve(data)
        } else if (code === 401) {
          wx.removeStorageSync('token')
          wx.reLaunch({ url: '/pages/login/login' })
          reject(new Error(message))
        } else {
          wx.showToast({ title: message || '请求失败', icon: 'none' })
          reject(new Error(message))
        }
      },
      fail(err) {
        wx.showToast({ title: '网络异常', icon: 'none' })
        reject(err)
      }
    })
  })
}

/** 附件上传：multipart 直传后端（SRS FR-04-03） */
function uploadFile(filePath, formData) {
  return new Promise((resolve, reject) => {
    wx.uploadFile({
      url: BASE_URL + '/file/upload',
      filePath,
      name: 'file',
      formData: formData || {},
      header: {
        Authorization: 'Bearer ' + (wx.getStorageSync('token') || '')
      },
      success(res) {
        const body = JSON.parse(res.data)
        if (body.code === 200) resolve(body.data)
        else reject(new Error(body.message))
      },
      fail: reject
    })
  })
}

module.exports = { request, uploadFile }
