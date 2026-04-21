/**
 * 菜单模块 API
 * @description 动态菜单查询接口封装
 */
import request from '@/utils/request'
import type { Result } from '@/utils/types'

/**
 * 菜单视图对象
 */
export interface SysMenuVO {
  id: number
  parentId: number
  name: string
  path: string
  component: string
  icon: string
  sortOrder: number
  visible: number
  permission: string
  menuType: number
  status: number
  createTime: string
  children: SysMenuVO[]
}

/**
 * 获取当前用户动态菜单树
 */
export const getCurrentUserMenus = (): Promise<SysMenuVO[]> =>
  request.get<SysMenuVO[]>('/menus/current') as unknown as Promise<SysMenuVO[]>