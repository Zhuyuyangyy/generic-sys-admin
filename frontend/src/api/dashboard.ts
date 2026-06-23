/**
 * 仪表盘模块 API
 * @description 系统概览、设备状态分布、库存汇总、最近活动等接口封装
 */
import request from '@/utils/request'
import type { DashboardOverview, EquipmentStatusDistribution, InventorySummary, RecentActivity } from '@/utils/types'

/**
 * 获取系统概览数据
 */
export const getOverview = (): Promise<DashboardOverview> =>
  request.get<DashboardOverview>('/dashboard/overview') as unknown as Promise<DashboardOverview>

/**
 * 获取设备状态分布
 */
export const getEquipmentStatus = (): Promise<EquipmentStatusDistribution> =>
  request.get<EquipmentStatusDistribution>('/dashboard/equipment-status') as unknown as Promise<EquipmentStatusDistribution>

/**
 * 获取库存汇总数据
 */
export const getInventorySummary = (): Promise<InventorySummary> =>
  request.get<InventorySummary>('/dashboard/inventory-summary') as unknown as Promise<InventorySummary>

/**
 * 获取最近活动列表
 */
export const getRecentActivities = (): Promise<RecentActivity[]> =>
  request.get<RecentActivity[]>('/dashboard/recent-activities') as unknown as Promise<RecentActivity[]>
