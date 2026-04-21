/**
 * 操作日志模块 API
 */
import request from '@/utils/request'
import type { Result, PageVO } from '@/utils/types'

/**
 * 操作日志视图对象
 */
export interface SysOperationLogVO {
  id: number
  module: string
  operation: string
  methodName: string
  targetTable: string
  targetId: string
  requestMethod: string
  requestUrl: string
  requestParams: string
  userId: number
  username: string
  ipAddress: string
  userAgent: string
  operationTime: string
  durationMs: number
  resultStatus: number
  errorDetail: string
}

/**
 * 日志查询参数
 */
export interface LogQueryDTO {
  pageNum: number
  pageSize: number
  module?: string
  operation?: string
  operator?: string
  status?: string
  startTime?: string
  endTime?: string
}

/**
 * 分页获取操作日志
 */
export const getOperationLogs = (params: LogQueryDTO): Promise<PageVO<SysOperationLogVO>> =>
  request.get<PageVO<SysOperationLogVO>>('/logs', { params }) as unknown as Promise<PageVO<SysOperationLogVO>>