import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '../store/user'

const Layout = () => import('../layout/Layout.vue')

const routes: RouteRecordRaw[] = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/login/Login.vue'),
    meta: { title: '登录', noAuth: true }
  },
  {
    path: '/',
    component: Layout,
    redirect: '/dashboard',
    children: [
      // Dashboard
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('../views/dashboard/Dashboard.vue'),
        meta: { title: '总览驾驶舱', icon: 'Monitor' }
      },
      // Asset Management
      {
        path: 'equipment',
        name: 'Equipment',
        component: () => import('../views/equipment/Equipment.vue'),
        meta: { title: '设备台账', icon: 'Box', group: '资产管理' }
      },
      {
        path: 'equipment/category',
        name: 'EquipmentCategory',
        component: () => import('../views/equipment/Category.vue'),
        meta: { title: '设备分类', icon: 'Menu', group: '资产管理' }
      },
      {
        path: 'equipment/location',
        name: 'Location',
        component: () => import('../views/equipment/Location.vue'),
        meta: { title: '位置管理', icon: 'Location', group: '资产管理' }
      },
      {
        path: 'equipment/maintenance',
        name: 'MaintenancePlan',
        component: () => import('../views/equipment/Maintenance.vue'),
        meta: { title: '维保计划', icon: 'SetUp', group: '资产管理' }
      },
      {
        path: 'equipment/health',
        name: 'AssetHealth',
        component: () => import('../views/equipment/Health.vue'),
        meta: { title: '资产健康', icon: 'FirstAidKit', group: '资产管理' }
      },
      // Inventory Management
      {
        path: 'consumables',
        name: 'Consumables',
        component: () => import('../views/consumables/Consumables.vue'),
        meta: { title: '耗材台账', icon: 'Goods', group: '耗材库存' }
      },
      {
        path: 'consumables/inbound',
        name: 'Inbound',
        component: () => import('../views/consumables/Inbound.vue'),
        meta: { title: '入库管理', icon: 'Download', group: '耗材库存' }
      },
      {
        path: 'consumables/outbound',
        name: 'Outbound',
        component: () => import('../views/consumables/Outbound.vue'),
        meta: { title: '出库管理', icon: 'Upload2', group: '耗材库存' }
      },
      {
        path: 'consumables/alerts',
        name: 'StockAlerts',
        component: () => import('../views/consumables/Alerts.vue'),
        meta: { title: '库存预警', icon: 'Bell', group: '耗材库存' }
      },
      {
        path: 'consumables/suppliers',
        name: 'Suppliers',
        component: () => import('../views/consumables/Suppliers.vue'),
        meta: { title: '供应商管理', icon: 'OfficeBuilding', group: '耗材库存' }
      },
      // Workflow
      {
        path: 'workflow',
        name: 'Workflow',
        component: () => import('../views/workflow/Workflow.vue'),
        meta: { title: '审批中心', icon: 'Stamp', group: '工单流程' }
      },
      {
        path: 'workflow/my-tasks',
        name: 'MyTasks',
        component: () => import('../views/workflow/MyTasks.vue'),
        meta: { title: '我的待办', icon: 'List', group: '工单流程' }
      },
      // NL Assistant
      {
        path: 'nl-assistant',
        name: 'NLAssistant',
        component: () => import('../views/nl/NLAssistant.vue'),
        meta: { title: '智能助手', icon: 'ChatDotRound', group: '智能助手' }
      },
      // System Management
      {
        path: 'system/users',
        name: 'Users',
        component: () => import('../views/system/Users.vue'),
        meta: { title: '用户管理', icon: 'User', group: '系统管理' }
      },
      {
        path: 'system/roles',
        name: 'Roles',
        component: () => import('../views/system/Roles.vue'),
        meta: { title: '角色权限', icon: 'Lock', group: '系统管理' }
      },
      {
        path: 'system/menus',
        name: 'Menus',
        component: () => import('../views/system/Menus.vue'),
        meta: { title: '菜单资源', icon: 'Grid', group: '系统管理' }
      },
      {
        path: 'system/audit-log',
        name: 'AuditLog',
        component: () => import('../views/system/AuditLog.vue'),
        meta: { title: '操作日志', icon: 'Document', group: '系统管理' }
      },
      {
        path: 'system/anomaly',
        name: 'Anomaly',
        component: () => import('../views/system/Anomaly.vue'),
        meta: { title: '审计中心', icon: 'Warning', group: '系统管理' }
      },
      // Reports
      {
        path: 'reports',
        name: 'Reports',
        component: () => import('../views/reports/Reports.vue'),
        meta: { title: '报表中心', icon: 'DataAnalysis' }
      },
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/dashboard'
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.noAuth || userStore.token) {
    next()
  } else {
    next('/login')
  }
})

export default router
