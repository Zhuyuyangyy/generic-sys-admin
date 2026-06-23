/**
 * 位置管理模块 API
 * @description 设备存放位置的增删改查接口封装
 */
import request from '@/utils/request'
import type { LocationVO } from '@/utils/types'

/**
 * 位置新增/编辑请求参数
 */
export interface LocationSaveDTO {
  name: string
  building?: string
  floor?: string
  room?: string
  description?: string
}

/**
 * 获取所有位置列表
 */
export const getLocations = (): Promise<LocationVO[]> =>
  request.get<LocationVO[]>('/equipment/locations') as unknown as Promise<LocationVO[]>

/**
 * 新增位置
 * @param data - 位置信息
 */
export const createLocation = (data: LocationSaveDTO): Promise<LocationVO> =>
  request.post<LocationVO>('/equipment/locations', data) as unknown as Promise<LocationVO>

/**
 * 更新位置信息
 * @param id - 位置唯一标识
 * @param data - 更新后的位置信息
 */
export const updateLocation = (id: number, data: LocationSaveDTO): Promise<LocationVO> =>
  request.put<LocationVO>(`/equipment/locations/${id}`, data) as unknown as Promise<LocationVO>

/**
 * 删除位置
 * @param id - 位置唯一标识
 */
export const deleteLocation = (id: number): Promise<void> =>
  request.delete<void>(`/equipment/locations/${id}`) as unknown as Promise<void>
