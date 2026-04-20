/**
 * 设备管理模块 API
 * @description 设备资产的全生命周期管理接口封装，包含查询、新增、更新、状态流转及软删除
 */
import request from '@/utils/request'
import type { Result } from '@/utils/types'

/**
 * 设备状态枚举
 * - 0: 维修中
 * - 1: 正常
 * - 2: 报废
 */
export type EquipmentStatus = 0 | 1 | 2

/**
 * 设备新增/编辑请求参数
 */
export interface EquipmentSaveDTO {
  name: string
  category: string
  model: string
  serialNumber: string
  status: EquipmentStatus
  location: string
  purchaseDate?: string
  purchasePrice?: number
  supplier?: string
  remarks?: string
}

/** 设备更新 DTO（与 Save 共用字段）*/
export type EquipmentUpdateDTO = EquipmentSaveDTO

/**
 * 设备视图对象
 * @description 包含设备完整信息及计算字段（状态文本、距下次保养天数等）
 */
export interface EquipmentVO extends EquipmentSaveDTO {
  id: number
  status: EquipmentStatus
  statusText: string
  lastMaintenanceDate?: string
  nextMaintenanceDate?: string
  createTime?: string
  updateTime?: string
}

/**
 * 设备分页查询请求参数
 */
export interface EquipmentPageParam {
  pageNum: number
  pageSize: number
  name?: string
  category?: string
  status?: EquipmentStatus
}

/**
 * 分页查询设备列表
 * @param params - 分页参数及过滤条件
 */
export const getEquipmentPage = (params: EquipmentPageParam) =>
  request.get<{ list: EquipmentVO[]; total: number; pageNum: number; pageSize: number }>('/equipment', { params }) as unknown as Promise<{ list: EquipmentVO[]; total: number; pageNum: number; pageSize: number }>

/**
 * 根据 ID 查询设备详情
 * @param id - 设备唯一标识
 */
export const getEquipmentById = (id: number): Promise<EquipmentVO> =>
  request.get<EquipmentVO>(`/equipment/${id}`) as unknown as Promise<EquipmentVO>

/**
 * 新增设备
 * @param data - 设备信息
 */
export const saveEquipment = (data: EquipmentSaveDTO): Promise<EquipmentVO> =>
  request.post<EquipmentVO>('/equipment', data) as unknown as Promise<EquipmentVO>

/**
 * 更新设备信息
 * @param id - 设备唯一标识
 * @param data - 更新后的设备信息
 */
export const updateEquipment = (id: number, data: EquipmentUpdateDTO): Promise<EquipmentVO> =>
  request.put<EquipmentVO>(`/equipment/${id}`, data) as unknown as Promise<EquipmentVO>

/**
 * 更新设备状态（状态机流转）
 * @param id - 设备唯一标识
 * @param status - 目标状态码
 */
export const updateEquipmentStatus = (id: number, status: EquipmentStatus): Promise<void> =>
  request.patch<void>(`/equipment/${id}/status`, null, { params: { status } }) as unknown as Promise<void>

/**
 * 删除设备（软删除）
 * @param id - 设备唯一标识
 */
export const deleteEquipment = (id: number): Promise<void> =>
  request.delete<void>(`/equipment/${id}`) as unknown as Promise<void>
