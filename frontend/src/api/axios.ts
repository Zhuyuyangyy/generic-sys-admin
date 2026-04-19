import axios, { AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'

// 创建 axios 实例
const service: AxiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
  withCredentials: true,
})

// ==================== 请求拦截器 ====================
service.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    // 1. 携带Token
    const token = localStorage.getItem('token')
    if (token && config.headers) {
      config.headers['Authorization'] = `Bearer ${token}`
    }

    // 2. 自动携带TenantId
    const tenantId = localStorage.getItem('tenantId')
    if (tenantId && config.headers) {
      config.headers['Tenant-Id'] = tenantId
    }

    // 3. 请求唯一ID（埋点追踪）
    if (config.headers) {
      config.headers['X-Request-Id'] = crypto.randomUUID()
    }

    return config
  },
  (error) => {
    console.error('请求配置错误:', error)
    return Promise.reject(error)
  }
)

// ==================== 响应拦截器 ====================
service.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data

    // code 为 200 视为成功
    if (res.code === 200) {
      return res
    }

    // 业务错误处理
    switch (res.code) {
      case 401:
        ElMessage.error('登录已过期，请重新登录')
        localStorage.removeItem('token')
        setTimeout(() => (window.location.href = '/login'), 1500)
        break
      case 403:
        ElMessage.error('无权限访问')
        break
      case 404:
        ElMessage.error('资源不存在')
        break
      default:
        ElMessage.error(res.message || '请求失败')
    }

    return Promise.reject(new Error(res.message || '请求失败'))
  },
  (error) => {
    if (error.response) {
      const status = error.response.status
      switch (status) {
        case 400: ElMessage.error('请求参数错误'); break
        case 401:
          ElMessage.error('未登录或登录已过期')
          localStorage.removeItem('token')
          setTimeout(() => (window.location.href = '/login'), 1500)
          break
        case 403: ElMessage.error('无权限访问'); break
        case 404: ElMessage.error('接口不存在'); break
        case 500: ElMessage.error('服务器内部错误'); break
        default: ElMessage.error(`请求失败(${status})`)
      }
    } else if (error.code === 'ECONNABORTED') {
      ElMessage.error('请求超时，请稍后再试')
    } else if (error.code === 'ERR_NETWORK') {
      ElMessage.error('网络连接失败，请检查网络')
    } else {
      ElMessage.error('网络异常，请稍后再试')
    }

    return Promise.reject(error)
  }
)

export default service
