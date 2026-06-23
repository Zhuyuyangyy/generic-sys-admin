/**
 * 库存预警模块 API
 * @description 耗材库存预警查询、汇总统计及确认接口封装
 */
import request from '@/utils/request'
import type { StockAlertVO } from '@/utils/types'

/**
 * 库存预警分页查询参数
 */
export interface StockAlertPageParam {
  pageNum: number
  pageSize: number
  acknowledged?: boolean
  severity?: string
}

/**
 * 分页查询库存预警列表
 * @param params - 分页参数及过滤条件
 */
export const getStockAlerts = (params: any) =>
  request.get('/consumables/alerts', { params })

/**
 * 获取库存预警汇总统计
 */
export const getAlertSummary = () =>
  request.get('/consumables/alerts/summary')

/**
 * 确认库存预警
 * @param id - 预警唯一标识
 */
export const acknowledgeAlert = (id: number): Promise<void> =>
  request.patch<void>(`/consumables/alerts/${id}/acknowledge`) as unknown as Promise<void>
