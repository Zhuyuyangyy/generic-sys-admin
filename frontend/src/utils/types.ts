/**
 * 通用响应包装器
 * @description 统一前后端数据响应结构，所有 API 响应均包装于此
 */
export interface Result<T = any> {
  /** 业务状态码，200 表示成功 */
  code: number
  /** 响应描述信息 */
  message: string
  /** 响应数据负载 */
  data: T
  /** 响应时间戳 */
  timestamp?: number
  /** 请求链路追踪 ID */
  traceId?: string
}

/**
 * 分页请求参数
 */
export interface PageParam {
  pageNum: number
  pageSize: number
}

/**
 * 分页响应数据
 */
export interface PageVO<T> {
  list: T[]
  total: number
  pageNum: number
  pageSize: number
}
