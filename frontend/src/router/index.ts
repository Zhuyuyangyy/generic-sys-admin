/**
 * 路由配置模块
 * @description Vue Router 全局路由表及路由守卫配置，管理页面访问控制与导航逻辑
 */
import { createRouter, createWebHistory } from 'vue-router'
import type { RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/store/user'

/**
 * 路由表定义
 * @description 包含登录页（无需认证）及主布局页（含三个子路由：仪表盘、设备管理、耗材管理）
 */
const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/Login.vue'),
    meta: { requiresAuth: false },
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/dashboard',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/dashboard/Dashboard.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'equipment',
        name: 'Equipment',
        component: () => import('@/views/equipment/Equipment.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'consumables',
        name: 'Consumables',
        component: () => import('@/views/consumables/Consumables.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'ai-studio',
        name: 'AiStudio',
        component: () => import('@/views/ai-studio/AiStudio.vue'),
        meta: { requiresAuth: true },
      },
      {
        path: 'logs',
        name: 'OperationLog',
        component: () => import('@/views/logs/OperationLog.vue'),
        meta: { requiresAuth: true, title: '操作日志' },
      },
    ],
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
})

/**
 * 全局前置守卫 - 路由状态管理
 * @description 每次路由切换前校验认证状态：
 * - 需要认证（requiresAuth !== false）但无 Token → 重定向至 /login
 * - 已持有 Token 访问登录页 → 重定向至 /dashboard
 * - 其他情况 → 放行
 */
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  const hasToken = !!userStore.token

  if (to.meta.requiresAuth !== false && !hasToken) {
    next('/login')
  } else if (to.path === '/login' && hasToken) {
    next('/dashboard')
  } else {
    next()
  }
})

export default router
