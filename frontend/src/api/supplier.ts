/**
 * 供应商管理模块 API
 * @description 耗材供应商的增删改查接口封装
 */
import request from '@/utils/request'
import type { SupplierVO } from '@/utils/types'

/**
 * 供应商新增/编辑请求参数
 */
export interface SupplierSaveDTO {
  name: string
  contactPerson?: string
  phone?: string
  email?: string
  address?: string
  description?: string
}

/**
 * 供应商分页查询参数
 */
export interface SupplierPageParam {
  pageNum: number
  pageSize: number
  name?: string
}

/**
 * 分页查询供应商列表
 * @param params - 分页参数及过滤条件
 */
export const getSuppliers = (params: any) =>
  request.get('/consumables/suppliers', { params })

/**
 * 根据 ID 查询供应商详情
 * @param id - 供应商唯一标识
 */
export const getSupplier = (id: number): Promise<SupplierVO> =>
  request.get<SupplierVO>(`/consumables/suppliers/${id}`) as unknown as Promise<SupplierVO>

/**
 * 新增供应商
 * @param data - 供应商信息
 */
export const createSupplier = (data: SupplierSaveDTO): Promise<SupplierVO> =>
  request.post<SupplierVO>('/consumables/suppliers', data) as unknown as Promise<SupplierVO>

/**
 * 更新供应商信息
 * @param id - 供应商唯一标识
 * @param data - 更新后的供应商信息
 */
export const updateSupplier = (id: number, data: SupplierSaveDTO): Promise<SupplierVO> =>
  request.put<SupplierVO>(`/consumables/suppliers/${id}`, data) as unknown as Promise<SupplierVO>

/**
 * 删除供应商
 * @param id - 供应商唯一标识
 */
export const deleteSupplier = (id: number): Promise<void> =>
  request.delete<void>(`/consumables/suppliers/${id}`) as unknown as Promise<void>
