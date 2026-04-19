import service from './axios'
import type { AxiosRequestConfig } from 'axios'

// 泛型请求方法
export function request<T = any>(config: AxiosRequestConfig): Promise<T> {
  return service.request(config)
}

// 快捷方法
export const get = <T = any>(url: string, params?: any, config?: AxiosRequestConfig) =>
  request<T>({ method: 'GET', url, params, ...config })

export const post = <T = any>(url: string, data?: any, config?: AxiosRequestConfig) =>
  request<T>({ method: 'POST', url, data, ...config })

export const put = <T = any>(url: string, data?: any, config?: AxiosRequestConfig) =>
  request<T>({ method: 'PUT', url, data, ...config })

export const del = <T = any>(url: string, params?: any, config?: AxiosRequestConfig) =>
  request<T>({ method: 'DELETE', url, params, ...config })
