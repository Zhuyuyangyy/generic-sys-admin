/**
 * 耗材管理模块 API
 * @description 耗材库存的全生命周期管理接口封装，包含查询、新增、更新、出入库、库存调整及软删除
 */
import request from '@/utils/request'
import type { Result } from '@/utils/types'

/**
 * 耗材状态枚举
 * - 1: 正常
 * - 0/其他: 停用
 */
export type ConsumableStatus = number

/**
 * 耗材新增/编辑请求参数
 */
export interface ConsumableSaveDTO {
  name: string
  category: string
  unit: string
  minStockLevel: number
  maxStockLevel: number
  unitCost: number
  expirationDate?: string
  supplier?: string
  storageLocation?: string
  remarks?: string
}

/** 耗材更新 DTO（与 Save 共用字段）*/
export type ConsumableUpdateDTO = ConsumableSaveDTO

/**
 * 耗材视图对象
 * @description 包含耗材完整信息及实时库存数据
 */
export interface ConsumableVO extends ConsumableSaveDTO {
  id: number
  currentStock: number
  status: ConsumableStatus
  statusText: string
  reorderPoint: number
  createTime?: string
  updateTime?: string
}

/**
 * 耗材分页查询请求参数
 */
export interface ConsumablePageParam {
  pageNum: number
  pageSize: number
  name?: string
  category?: string
  status?: ConsumableStatus
}

/**
 * 分页查询耗材列表
 * @param params - 分页参数及过滤条件
 */
export const getConsumablePage = (params: ConsumablePageParam) =>
  request.get<{ list: ConsumableVO[]; total: number; pageNum: number; pageSize: number }>('/consumables', { params }) as unknown as Promise<{ list: ConsumableVO[]; total: number; pageNum: number; pageSize: number }>

/**
 * 根据 ID 查询耗材详情
 * @param id - 耗材唯一标识
 */
export const getConsumableById = (id: number): Promise<ConsumableVO> =>
  request.get<ConsumableVO>(`/consumables/${id}`) as unknown as Promise<ConsumableVO>

/**
 * 新增耗材
 * @param data - 耗材信息
 */
export const saveConsumable = (data: ConsumableSaveDTO): Promise<ConsumableVO> =>
  request.post<ConsumableVO>('/consumables', data) as unknown as Promise<ConsumableVO>

/**
 * 更新耗材信息
 * @param id - 耗材唯一标识
 * @param data - 更新后的耗材信息
 */
export const updateConsumable = (id: number, data: ConsumableUpdateDTO): Promise<ConsumableVO> =>
  request.put<ConsumableVO>(`/consumables/${id}`, data) as unknown as Promise<ConsumableVO>

/**
 * 删除耗材（软删除）
 * @param id - 耗材唯一标识
 */
export const deleteConsumable = (id: number): Promise<void> =>
  request.delete<void>(`/consumables/${id}`) as unknown as Promise<void>

/**
 * 耗材入库
 * @param id - 耗材唯一标识
 * @param quantity - 入库数量
 * @param referenceNo - 参考单号（可选）
 * @param remarks - 备注（可选）
 */
export const inboundConsumable = (id: number, quantity: number, referenceNo?: string, remarks?: string): Promise<void> =>
  request.post<void>(`/consumables/${id}/inbound`, null, { params: { quantity, referenceNo, remarks } }) as unknown as Promise<void>

/**
 * 耗材出库
 * @param id - 耗材唯一标识
 * @param quantity - 出库数量
 * @param referenceNo - 参考单号（可选）
 * @param remarks - 备注（可选）
 */
export const outboundConsumable = (id: number, quantity: number, referenceNo?: string, remarks?: string): Promise<void> =>
  request.post<void>(`/consumables/${id}/outbound`, null, { params: { quantity, referenceNo, remarks } }) as unknown as Promise<void>

/**
 * 手动库存调整
 * @param id - 耗材唯一标识
 * @param delta - 调整量（正数增加，负数减少）
 * @param referenceNo - 参考单号（可选）
 * @param remarks - 备注（可选）
 */
export const adjustConsumableStock = (id: number, delta: number, referenceNo?: string, remarks?: string): Promise<void> =>
  request.patch<void>(`/consumables/${id}/stock`, null, { params: { delta, referenceNo, remarks } }) as unknown as Promise<void>
