/**
 * 维保计划模块 API
 * @description 设备维保计划的增删改查、状态流转、逾期/即将到期查询及自动检查接口封装
 */
import request from '@/utils/request'
import type { MaintenancePlanVO } from '@/utils/types'

/**
 * 维保计划新增/编辑请求参数
 */
export interface MaintenancePlanSaveDTO {
  equipmentId: number
  type: string
  title: string
  description?: string
  scheduledDate: string
  assigneeId?: number
  priority?: string
}

/**
 * 维保计划分页查询参数
 */
export interface MaintenancePlanPageParam {
  pageNum: number
  pageSize: number
  equipmentId?: number
  status?: string
  type?: string
}

/**
 * 分页查询维保计划列表
 * @param params - 分页参数及过滤条件
 */
export const getMaintenancePlans = (params: any) =>
  request.get('/equipment/maintenance-plans', { params })

/**
 * 根据 ID 查询维保计划详情
 * @param id - 维保计划唯一标识
 */
export const getMaintenancePlan = (id: number): Promise<MaintenancePlanVO> =>
  request.get<MaintenancePlanVO>(`/equipment/maintenance-plans/${id}`) as unknown as Promise<MaintenancePlanVO>

/**
 * 新增维保计划
 * @param data - 维保计划信息
 */
export const createMaintenancePlan = (data: MaintenancePlanSaveDTO): Promise<MaintenancePlanVO> =>
  request.post<MaintenancePlanVO>('/equipment/maintenance-plans', data) as unknown as Promise<MaintenancePlanVO>

/**
 * 更新维保计划信息
 * @param id - 维保计划唯一标识
 * @param data - 更新后的维保计划信息
 */
export const updateMaintenancePlan = (id: number, data: MaintenancePlanSaveDTO): Promise<MaintenancePlanVO> =>
  request.put<MaintenancePlanVO>(`/equipment/maintenance-plans/${id}`, data) as unknown as Promise<MaintenancePlanVO>

/**
 * 更新维保计划状态
 * @param id - 维保计划唯一标识
 * @param status - 目标状态
 */
export const updatePlanStatus = (id: number, status: string): Promise<void> =>
  request.patch<void>(`/equipment/maintenance-plans/${id}/status`, { status }) as unknown as Promise<void>

/**
 * 删除维保计划
 * @param id - 维保计划唯一标识
 */
export const deleteMaintenancePlan = (id: number): Promise<void> =>
  request.delete<void>(`/equipment/maintenance-plans/${id}`) as unknown as Promise<void>

/**
 * 获取逾期维保列表
 */
export const getOverdueMaintenance = () =>
  request.get('/equipment/maintenance/overdue')

/**
 * 获取即将到期的维保列表
 * @param days - 提前天数（默认 7 天）
 */
export const getUpcomingMaintenance = (days: number = 7) =>
  request.get('/equipment/maintenance/upcoming', { params: { days } })

/**
 * 手动触发维保检查
 */
export const checkMaintenance = () =>
  request.post('/equipment/maintenance/check')
