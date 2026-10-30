import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

// 统一请求封装（SRS 7.1：JWT 头 + code/message/data 约定）
const request = axios.create({ baseURL: '/api', timeout: 30000 })

request.interceptors.request.use((config) => {
  const token = localStorage.getItem('token')
  if (token) config.headers.Authorization = `Bearer ${token}`
  return config
})

request.interceptors.response.use(
  (resp) => {
    const { code, message, data } = resp.data
    if (code === 200) return data
    if (code === 401) {
      localStorage.clear()
      router.push('/login')
    }
    ElMessage.error(message || '请求失败')
    return Promise.reject(new Error(message))
  },
  (err) => {
    if (err.response?.status === 401) {
      localStorage.clear()
      router.push('/login')
    }
    ElMessage.error(err.response?.data?.message || '网络异常')
    return Promise.reject(err)
  }
)

export default request
