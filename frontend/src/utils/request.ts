/**
 * Axios 实例封装 - API 请求核心模块
 * @description 基于 Axios 封装全局请求拦截器、响应拦截器，统一处理认证注入、错误提示及 Result<T> 结构解析
 * @description 支持 Token 自动刷新机制：响应 401 时使用 refreshToken 换取新 accessToken，并重试原始请求
 */
import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/** Axios 实例，baseURL 指向 Vite 代理路径 /api */
const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

/** 刷新Token的Promise互斥锁，防止并发刷新 */
let isRefreshing = false
let refreshPromise: Promise<string | null> | null = null

/**
 * 执行 Token 刷新
 * 使用 Promise 锁防止多个请求并发触发刷新
 */
function doRefreshToken(): Promise<string | null> {
  const refreshToken = localStorage.getItem('refresh_token')
  if (!refreshToken) {
    return Promise.resolve(null)
  }

  return axios.post('/api/users/refresh-token', { refreshToken }, {
    baseURL: '/api',
    timeout: 10000,
  }).then(res => {
    const data = (res.data as { code: number; data: { accessToken: string; refreshToken: string } })
    if (data.code === 200 && data.data) {
      localStorage.setItem('access_token', data.data.accessToken)
      localStorage.setItem('refresh_token', data.data.refreshToken)
      return data.data.accessToken
    }
    return null
  }).catch(() => {
    // 刷新失败，清除所有Token并跳转登录
    localStorage.removeItem('access_token')
    localStorage.removeItem('refresh_token')
    router.push('/login')
    return null
  })
}

/**
 * 获取新的 accessToken
 * 串行化多个并发刷新请求
 */
function getAccessToken(): Promise<string | null> {
  if (isRefreshing) {
    return refreshPromise!
  }
  isRefreshing = true
  refreshPromise = doRefreshToken().finally(() => {
    isRefreshing = false
    refreshPromise = null
  })
  return refreshPromise
}

/** 正在等待重试的请求队列 */
let pendingRequests: Array<{
  resolve: (token: string) => void
  reject: (err: Error) => void
}> = []

/** 将请求加入重试队列 */
function enqueueRequest(): Promise<string> {
  return new Promise((resolve, reject) => {
    pendingRequests.push({ resolve, reject })
  })
}

/** 通知队列中的所有请求，刷新完成或失败 */
function notifyRequests(token: string | null, error?: Error) {
  pendingRequests.forEach(({ resolve, reject }) => {
    if (token) {
      resolve(token)
    } else if (error) {
      reject(error)
    }
  })
  pendingRequests = []
}

/**
 * 请求拦截器 - 异步请求认证注入
 * @description 在每次请求发送前从 localStorage 提取 access_token，注入至 Authorization Header
 */
request.interceptors.request.use(
  (config: InternalAxiosRequestConfig) => {
    const token = localStorage.getItem('access_token')
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  (error) => Promise.reject(error)
)

/**
 * 响应拦截器 - 异步响应预处理与错误处理
 * @description 识别后端 Result<T> 统一响应结构：
 * - 若 code !== 200，提示错误信息并对 401 执行 Token 刷新+重试
 * - 若 code === 200，拦截器返回 data 负载（由调用方泛型 T 决定类型）
 * - 实际返回类型为 Promise<T>（拦截器已将 AxiosResponse<T> → T）
 */
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data as { code: number; message?: string; data: unknown }
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 返回 data 负载，as any 跳过静态类型检查；运行时类型由 T 决定
    return res.data as any
  },
  async (error) => {
    const originalRequest = error.config

    // 1. 如果是 401 且还没重试过，尝试刷新Token
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true

      try {
        const newToken = await getAccessToken()
        if (newToken) {
          // 重试所有排队的请求
          notifyRequests(newToken)
          // 重试当前请求
          originalRequest.headers.Authorization = `Bearer ${newToken}`
          return request(originalRequest)
        }
      } catch (refreshError) {
        notifyRequests(null, refreshError as Error)
        return Promise.reject(refreshError)
      }
    }

    // 2. 刷新Token后重试时发现刷新失败（其他请求已清除Token）
    if (error.response?.status === 401 && originalRequest._retry && !isRefreshing) {
      ElMessage.error('登录已过期，请重新登录')
      localStorage.removeItem('access_token')
      localStorage.removeItem('refresh_token')
      router.push('/login')
    } else if (error.response?.status === 401) {
      // 正在刷新中，等待
      try {
        const token = await enqueueRequest()
        originalRequest.headers.Authorization = `Bearer ${token}`
        return request(originalRequest)
      } catch {
        return Promise.reject(error)
      }
    }

    // 3. 其他错误
    const message = error.response?.data?.message || error.message || '网络错误'
    ElMessage.error(message)
    return Promise.reject(error)
  }
)

// 重导出 axios 类型，供 API 层做显式类型声明
export { type AxiosResponse }

export default request