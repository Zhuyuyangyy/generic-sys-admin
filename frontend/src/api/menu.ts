/**
 * 菜单管理模块 API
 * @description 菜单树增删改查接口封装
 */
import request from '@/utils/request'
import type { MenuVO } from '@/utils/types'

/**
 * 菜单新增/编辑请求参数
 */
export interface MenuSaveDTO {
  parentId?: number
  name: string
  path?: string
  component?: string
  icon?: string
  sort?: number
  visible?: boolean
  type?: number
  permission?: string
}

/**
 * 获取菜单树
 */
export const getMenuTree = (): Promise<MenuVO[]> =>
  request.get<MenuVO[]>('/menus') as unknown as Promise<MenuVO[]>

/**
 * 根据 ID 查询菜单详情
 * @param id - 菜单唯一标识
 */
export const getMenu = (id: number): Promise<MenuVO> =>
  request.get<MenuVO>(`/menus/${id}`) as unknown as Promise<MenuVO>

/**
 * 新增菜单
 * @param data - 菜单信息
 */
export const createMenu = (data: MenuSaveDTO): Promise<MenuVO> =>
  request.post<MenuVO>('/menus', data) as unknown as Promise<MenuVO>

/**
 * 更新菜单信息
 * @param id - 菜单唯一标识
 * @param data - 更新后的菜单信息
 */
export const updateMenu = (id: number, data: MenuSaveDTO): Promise<MenuVO> =>
  request.put<MenuVO>(`/menus/${id}`, data) as unknown as Promise<MenuVO>

/**
 * 删除菜单
 * @param id - 菜单唯一标识
 */
export const deleteMenu = (id: number): Promise<void> =>
  request.delete<void>(`/menus/${id}`) as unknown as Promise<void>
