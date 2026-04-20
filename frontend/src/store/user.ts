/**
 * 用户状态管理模块
 * @description 基于 Pinia 的响应式用户状态管理，封装 Token、用户资料的存取及持久化
 */
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { login as apiLogin, logout as apiLogout, getCurrentUser, type LoginVO, type UserVO } from '@/api/auth'

export const useUserStore = defineStore(
  'user',
  () => {
    /** 访问令牌（JWT），用于接口认证 */
    const token = ref(localStorage.getItem('access_token') || '')
    /** 当前登录用户资料 */
    const userInfo = ref<UserVO | null>(null)

    /**
     * 设置访问令牌
     * @param t - JWT 令牌字符串
     */
    const setToken = (t: string) => {
      token.value = t
      localStorage.setItem('access_token', t)
    }

    /**
     * 设置用户资料
     * @param info - 用户视图对象
     */
    const setUserInfo = (info: UserVO) => {
      userInfo.value = info
    }

    /**
     * 用户登录
     * @param username - 用户名
     * @param password - 密码
     * @returns 登录响应数据（包含 Token 及用户信息）
     */
    const login = async (username: string, password: string) => {
      const data = await apiLogin({ username, password })
      setToken(data.accessToken)
      setUserInfo(data.user)
      return data
    }

    /**
     * 获取并同步用户资料
     * @description 从后端拉取最新用户资料并更新本地状态
     */
    const fetchUserInfo = async () => {
      try {
        const data = await getCurrentUser()
        setUserInfo(data)
      } catch {
        // 非关键路径异常不向上穿透
      }
    }

    /**
     * 用户退出登录
     * @description 清除本地 Token 及用户资料，触发路由跳转由调用方控制
     */
    const logout = () => {
      token.value = ''
      userInfo.value = null
      localStorage.removeItem('access_token')
      apiLogout()
    }

    return {
      token,
      userInfo,
      setToken,
      setUserInfo,
      login,
      fetchUserInfo,
      logout,
    }
  },
  {
    persist: {
      key: 'user',
      storage: localStorage,
    },
  }
)
