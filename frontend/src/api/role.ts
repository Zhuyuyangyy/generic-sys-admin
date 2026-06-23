/**
 * 角色管理模块 API
 * @description 角色增删改查、菜单分配等接口封装
 */
import request from '@/utils/request'
import type { RoleVO } from '@/utils/types'

/**
 * 角色分页查询参数
 */
export interface RolePageParam {
  pageNum: number
  pageSize: number
  name?: string
  code?: string
}

/**
 * 角色新增/编辑请求参数
 */
export interface RoleSaveDTO {
  name: string
  code: string
  description?: string
  status?: number
}

/**
 * 分页查询角色列表
 * @param params - 分页参数及过滤条件
 */
export const getRoles = (params: any) =>
  request.get('/roles', { params })

/**
 * 根据 ID 查询角色详情
 * @param id - 角色唯一标识
 */
export const getRole = (id: number): Promise<RoleVO> =>
  request.get<RoleVO>(`/roles/${id}`) as unknown as Promise<RoleVO>

/**
 * 新增角色
 * @param data - 角色信息
 */
export const createRole = (data: RoleSaveDTO): Promise<RoleVO> =>
  request.post<RoleVO>('/roles', data) as unknown as Promise<RoleVO>

/**
 * 更新角色信息
 * @param id - 角色唯一标识
 * @param data - 更新后的角色信息
 */
export const updateRole = (id: number, data: RoleSaveDTO): Promise<RoleVO> =>
  request.put<RoleVO>(`/roles/${id}`, data) as unknown as Promise<RoleVO>

/**
 * 删除角色
 * @param id - 角色唯一标识
 */
export const deleteRole = (id: number): Promise<void> =>
  request.delete<void>(`/roles/${id}`) as unknown as Promise<void>

/**
 * 为角色分配菜单权限
 * @param id - 角色唯一标识
 * @param menuIds - 菜单 ID 列表
 */
export const assignMenus = (id: number, menuIds: number[]): Promise<void> =>
  request.post<void>(`/roles/${id}/menus`, { menuIds }) as unknown as Promise<void>

/**
 * 获取角色已分配的菜单列表
 * @param id - 角色唯一标识
 */
export const getRoleMenus = (id: number) =>
  request.get(`/roles/${id}/menus`)
