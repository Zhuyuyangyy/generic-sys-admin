/**
 * 设备健康度模块 API
 * @description 设备健康评分查询与概览接口封装
 */
import request from '@/utils/request'
import type { AssetHealthScore, AssetHealthOverview } from '@/utils/types'

/**
 * 获取所有设备健康评分列表
 */
export const getAllHealthScores = (): Promise<AssetHealthScore[]> =>
  request.get<AssetHealthScore[]>('/equipment/health') as unknown as Promise<AssetHealthScore[]>

/**
 * 获取指定设备健康评分
 * @param id - 设备唯一标识
 */
export const getHealthScore = (id: number): Promise<AssetHealthScore> =>
  request.get<AssetHealthScore>(`/equipment/health/${id}`) as unknown as Promise<AssetHealthScore>

/**
 * 获取设备健康概览统计
 */
export const getHealthOverview = (): Promise<AssetHealthOverview> =>
  request.get<AssetHealthOverview>('/equipment/health/overview') as unknown as Promise<AssetHealthOverview>
