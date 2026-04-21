/**
 * Dashboard 统计 API
 */
import request from '@/utils/request'

/**
 * Dashboard 统计数据响应
 */
export interface DashboardStatsVO {
  equipment: {
    total: number
    normal: number
    maintenance: number
    scrapped: number
  }
  consumable: {
    total: number
    lowStock: number
    expiring: number
  }
  user: {
    total: number
    active: number
    disabled: number
  }
  logTrend: Array<{ date: string; count: number }>
  equipmentStatus: Array<{ label: string; value: number }>
  operationType: Array<{ label: string; value: number }>
}

/**
 * 获取首页统计数据（聚合后端计算，前端不再拉全量明细）
 */
export const getDashboardStats = (): Promise<DashboardStatsVO> =>
  request.get<DashboardStatsVO>('/dashboard/stats') as unknown as Promise<DashboardStatsVO>