/**
 * 报表模块 API
 * @description 资产汇总、库存汇总、维保历史、审计轨迹、库存变动等报表接口封装
 */
import request from '@/utils/request'

/**
 * 报表查询参数
 */
export interface ReportParam {
  startDate?: string
  endDate?: string
  category?: string
  format?: 'json' | 'csv' | 'xlsx'
}

/**
 * 获取资产汇总报表
 * @param params - 报表查询参数
 */
export const getAssetSummaryReport = (params?: any) =>
  request.get('/reports/asset-summary', { params })

/**
 * 获取库存汇总报表
 * @param params - 报表查询参数
 */
export const getInventorySummaryReport = (params?: any) =>
  request.get('/reports/inventory-summary', { params })

/**
 * 获取维保历史报表
 * @param params - 报表查询参数
 */
export const getMaintenanceHistoryReport = (params: any) =>
  request.get('/reports/maintenance-history', { params })

/**
 * 获取审计轨迹报表
 * @param params - 报表查询参数
 */
export const getAuditTrailReport = (params: any) =>
  request.get('/reports/audit-trail', { params })

/**
 * 获取库存变动报表
 * @param params - 报表查询参数
 */
export const getStockMovementReport = (params: any) =>
  request.get('/reports/stock-movement', { params })
