/**
 * 认证模块 API
 * @description 用户登录、注册、资料管理、密码修改等认证相关接口封装
 */
import request from '@/utils/request'
import type { Result } from '@/utils/types'

/**
 * 登录请求参数
 */
export interface LoginDTO {
  username: string
  password: string
}

/**
 * 注册请求参数
 */
export interface RegisterDTO {
  username: string
  password: string
  realName: string
  email?: string
  phone?: string
}

/**
 * 用户视图对象
 * @description 用户敏感信息已脱敏，不包含密码等字段
 */
export interface UserVO {
  id: number
  username: string
  realName: string
  email?: string
  phone?: string
  avatarUrl?: string
  status: number
  statusText: string
  lastLoginIp?: string
  lastLoginAt?: string
  createTime: string
  passwordChangedAt?: string
  passwordAgeDays?: number
}

/**
 * 登录成功响应
 * @description 包含 JWT 访问令牌、刷新令牌、用户资料及登录时间戳
 */
export interface LoginVO {
  accessToken: string
  refreshToken: string
  expiresIn: number
  tokenType: string
  user: UserVO
  loginTime: string
}

/**
 * 用户登录
 * @param data - 登录凭证（用户名+密码）
 */
export const login = (data: LoginDTO): Promise<LoginVO> =>
  request.post<LoginVO>('/users/login', data) as unknown as Promise<LoginVO>

/**
 * 用户注册
 * @param data - 注册信息
 */
export const register = (data: RegisterDTO): Promise<UserVO> =>
  request.post<UserVO>('/users', data) as unknown as Promise<UserVO>

/**
 * 获取当前登录用户资料
 */
export const getCurrentUser = (): Promise<UserVO> =>
  request.get<UserVO>('/users/me') as unknown as Promise<UserVO>

/**
 * 更新当前用户资料
 * @param data - 需要更新的字段
 */
export const updateProfile = (data: Partial<UserVO>): Promise<UserVO> =>
  request.put<UserVO>('/users/me', data) as unknown as Promise<UserVO>

/**
 * 修改当前用户密码
 * @param data - 旧密码与新密码
 */
export const changePassword = (data: { oldPassword: string; newPassword: string }): Promise<void> =>
  request.put<void>('/users/me/password', data) as unknown as Promise<void>

/**
 * 用户退出登录
 * @description 前端仅清理本地凭证，服务端 Token 失效由后端处理
 */
export const logout = () => Promise.resolve()
