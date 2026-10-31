// 小程序端配置：后端地址支持运行时修改（真机调试用电脑局域网 IP）
const DEFAULT_BASE_URL = 'http://localhost:8080/api'
const STORAGE_KEY = 'baseUrl'
const APP_NAME = '商协会档案'

function normalize(url) {
  return (url || '').trim().replace(/\/+$/, '')
}

/** 当前使用的后端地址：优先取本地设置，其次用默认值 */
function getBaseUrl() {
  const saved = wx.getStorageSync(STORAGE_KEY)
  return normalize(saved) || DEFAULT_BASE_URL
}

function setBaseUrl(url) {
  const value = normalize(url)
  if (!value) {
    wx.removeStorageSync(STORAGE_KEY)
    return DEFAULT_BASE_URL
  }
  wx.setStorageSync(STORAGE_KEY, value)
  return value
}

function resetBaseUrl() {
  wx.removeStorageSync(STORAGE_KEY)
  return DEFAULT_BASE_URL
}

/** 弹窗修改后端地址，返回修改后的地址 */
function promptBaseUrl() {
  return new Promise((resolve) => {
    wx.showModal({
      title: '服务器地址',
      editable: true,
      placeholderText: '如 http://192.168.1.10:8080/api',
      content: '当前：' + getBaseUrl(),
      success(res) {
        if (!res.confirm) {
          resolve(getBaseUrl())
          return
        }
        const value = (res.content || '').trim()
        if (!/^https?:\/\//i.test(value)) {
          wx.showToast({ title: '需以 http:// 或 https:// 开头', icon: 'none' })
          resolve(getBaseUrl())
          return
        }
        const saved = setBaseUrl(value)
        wx.showToast({ title: '已保存', icon: 'success' })
        resolve(saved)
      },
      fail() {
        resolve(getBaseUrl())
      }
    })
  })
}

module.exports = {
  APP_NAME,
  DEFAULT_BASE_URL,
  getBaseUrl,
  setBaseUrl,
  resetBaseUrl,
  promptBaseUrl
}
