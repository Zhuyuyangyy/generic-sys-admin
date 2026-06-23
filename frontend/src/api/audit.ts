/**
 * 审计日志模块 API
 * @description 操作日志查询、异常事件扫描与统计接口封装
 */
import request from '@/utils/request'
import type { OperationLogVO, AnomalyEvent, AnomalySummary } from '@/utils/types'

/**
 * 操作日志分页查询参数
 */
export interface OperationLogPageParam {
  pageNum: number
  pageSize: number
  username?: string
  action?: string
  startTime?: string
  endTime?: string
}

/**
 * 异常事件分页查询参数
 */
export interface AnomalyPageParam {
  pageNum: number
  pageSize: number
  type?: string
  severity?: string
  resolved?: boolean
}

/**
 * 分页查询操作日志
 * @param params - 分页参数及过滤条件
 */
export const getOperationLogs = (params: any) =>
  request.get('/audit/logs', { params })

/**
 * 根据 ID 查询操作日志详情
 * @param id - 日志唯一标识
 */
export const getOperationLog = (id: number): Promise<OperationLogVO> =>
  request.get<OperationLogVO>(`/audit/logs/${id}`) as unknown as Promise<OperationLogVO>

/**
 * 删除操作日志
 * @param id - 日志唯一标识
 */
export const deleteOperationLog = (id: number): Promise<void> =>
  request.delete<void>(`/audit/logs/${id}`) as unknown as Promise<void>

/**
 * 分页查询异常事件
 * @param params - 分页参数及过滤条件
 */
export const getAnomalies = (params: any) =>
  request.get('/audit/anomalies', { params })

/**
 * 获取异常事件汇总统计
 */
export const getAnomalySummary = (): Promise<AnomalySummary> =>
  request.get<AnomalySummary>('/audit/anomalies/summary') as unknown as Promise<AnomalySummary>

/**
 * 手动触发异常扫描
 */
export const scanAnomalies = () =>
  request.post('/audit/anomalies/scan')
