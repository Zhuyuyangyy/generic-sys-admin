/**
 * Axios 实例封装 - API 请求核心模块
 * @description 基于 Axios 封装全局请求拦截器、响应拦截器，统一处理认证注入、错误提示及 Result<T> 结构解析
 */
import axios, { type AxiosResponse, type InternalAxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'

/** Axios 实例，baseURL 指向 Vite 代理路径 /api */
const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
})

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
 * - 若 code !== 200，提示错误信息并对 401 执行清除凭证+跳转登录
 * - 若 code === 200，拦截器返回 data 负载（由调用方泛型 T 决定类型）
 * - 实际返回类型为 Promise<T>（拦截器已将 AxiosResponse<T> → T）
 */
request.interceptors.response.use(
  (response: AxiosResponse) => {
    const res = response.data as { code: number; message?: string; data: unknown }
    if (res.code !== 200) {
      ElMessage.error(res.message || '请求失败')
      if (res.code === 401) {
        localStorage.removeItem('access_token')
        router.push('/login')
      }
      return Promise.reject(new Error(res.message || '请求失败'))
    }
    // 返回 data 负载，as any 跳过静态类型检查；运行时类型由 T 决定
    return res.data as any
  },
  (error) => {
    ElMessage.error(error.message || '网络错误')
    return Promise.reject(error)
  }
)

// 重导出 axios 类型，供 API 层做显式类型声明
export { type AxiosResponse }

export default request
