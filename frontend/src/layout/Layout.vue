<template>
  <el-container class="layout-container">
    <!-- Sidebar -->
    <el-aside :width="sidebarCollapsed ? '64px' : '220px'" class="sidebar">
      <div class="sidebar-header">
        <div class="logo-icon">
          <el-icon :size="24"><Monitor /></el-icon>
        </div>
        <transition name="fade">
          <span v-if="!sidebarCollapsed" class="logo-text">智运中心</span>
        </transition>
      </div>
      <el-scrollbar class="sidebar-scroll">
        <el-menu
          :default-active="activeMenu"
          :collapse="sidebarCollapsed"
          :collapse-transition="false"
          router
          background-color="#0c1a2e"
          text-color="#8ca0b8"
          active-text-color="#409eff"
        >
          <!-- Dashboard (no group) -->
          <el-menu-item index="/dashboard">
            <el-icon><Monitor /></el-icon>
            <template #title>总览驾驶舱</template>
          </el-menu-item>

          <!-- Grouped menu items -->
          <template v-for="group in menuGroups" :key="group.label">
            <el-menu-item-group>
              <template #title>
                <span class="group-title">{{ group.label }}</span>
              </template>
              <el-menu-item
                v-for="item in group.items"
                :key="item.path"
                :index="'/' + item.path"
              >
                <el-icon><component :is="item.icon" /></el-icon>
                <template #title>{{ item.title }}</template>
              </el-menu-item>
            </el-menu-item-group>
          </template>

          <!-- Reports (no group) -->
          <el-menu-item index="/reports">
            <el-icon><DataAnalysis /></el-icon>
            <template #title>报表中心</template>
          </el-menu-item>
        </el-menu>
      </el-scrollbar>
    </el-aside>

    <el-container class="main-container">
      <!-- Top header -->
      <el-header class="header" height="56px">
        <div class="header-left">
          <el-icon
            class="collapse-btn"
            :size="20"
            @click="sidebarCollapsed = !sidebarCollapsed"
          >
            <Fold v-if="!sidebarCollapsed" />
            <Expand v-else />
          </el-icon>
          <el-breadcrumb separator="/" class="breadcrumb">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <!-- WebSocket status indicator -->
          <div class="ws-status" :class="{ connected: wsConnected }">
            <span class="ws-dot"></span>
            <span class="ws-label">{{ wsConnected ? '在线' : '离线' }}</span>
          </div>

          <!-- Notification bell -->
          <el-popover
            placement="bottom-end"
            :width="360"
            trigger="click"
            popper-class="notification-popover"
          >
            <template #reference>
              <el-badge :value="unreadCount" :hidden="unreadCount === 0" :max="99" class="notification-badge">
                <el-icon :size="20" class="notification-bell"><Bell /></el-icon>
              </el-badge>
            </template>
            <div class="notification-panel">
              <div class="notification-header">
                <span class="notification-title">实时事件</span>
                <el-button link type="primary" size="small" @click="clearNotifications">全部已读</el-button>
              </div>
              <el-scrollbar max-height="320px">
                <div v-if="notifications.length === 0" class="notification-empty">
                  暂无新通知
                </div>
                <div
                  v-for="(n, i) in notifications"
                  :key="i"
                  class="notification-item"
                >
                  <div class="notification-item-dot" :class="n.level || 'info'"></div>
                  <div class="notification-item-content">
                    <div class="notification-item-text">{{ n.message }}</div>
                    <div class="notification-item-time">{{ n.time }}</div>
                  </div>
                </div>
              </el-scrollbar>
            </div>
          </el-popover>

          <!-- User dropdown -->
          <el-dropdown @command="handleCommand" trigger="click">
            <div class="user-info">
              <el-avatar :size="30" class="user-avatar">
                {{ (userStore.userInfo?.username || 'U').charAt(0).toUpperCase() }}
              </el-avatar>
              <span class="user-name">{{ userStore.userInfo?.username || '用户' }}</span>
              <el-icon class="user-arrow"><ArrowDown /></el-icon>
            </div>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">
                  <el-icon><User /></el-icon>个人中心
                </el-dropdown-item>
                <el-dropdown-item divided command="logout">
                  <el-icon><SwitchButton /></el-icon>退出登录
                </el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- Main content -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed, ref, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../store/user'

interface NotificationItem {
  message: string
  time: string
  level?: 'info' | 'warning' | 'error' | 'success'
}

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const sidebarCollapsed = ref(false)
const wsConnected = ref(false)
const notifications = ref<NotificationItem[]>([])
const unreadCount = computed(() => notifications.value.length)

let ws: WebSocket | null = null
let wsReconnectTimer: ReturnType<typeof setTimeout> | null = null

const activeMenu = computed(() => route.path)

/** Build grouped menu structure from router children */
const menuGroups = computed(() => {
  const groupMap = new Map<string, { label: string; items: { path: string; title: string; icon: string }[] }>()

  // Get the root layout route's children
  const rootRoute = router.options.routes.find(r => r.path === '/')
  const children = rootRoute?.children || []

  for (const child of children) {
    const meta = child.meta as { title?: string; icon?: string; group?: string } | undefined
    if (!meta?.group) continue

    if (!groupMap.has(meta.group)) {
      groupMap.set(meta.group, { label: meta.group, items: [] })
    }
    groupMap.get(meta.group)!.items.push({
      path: child.path,
      title: meta.title || child.name as string || '',
      icon: meta.icon || 'Document'
    })
  }

  // Maintain display order
  const order = ['资产管理', '耗材库存', '工单流程', '智能助手', '系统管理']
  return order
    .filter(g => groupMap.has(g))
    .map(g => groupMap.get(g)!)
})

/** Connect to WebSocket for real-time events */
function connectWS() {
  const protocol = window.location.protocol === 'https:' ? 'wss:' : 'ws:'
  const wsUrl = `${protocol}//${window.location.host}/ws/events`
  try {
    ws = new WebSocket(wsUrl)
    ws.onopen = () => {
      wsConnected.value = true
    }
    ws.onclose = () => {
      wsConnected.value = false
      scheduleReconnect()
    }
    ws.onerror = () => {
      wsConnected.value = false
    }
    ws.onmessage = (event) => {
      try {
        const data = JSON.parse(event.data)
        addNotification({
          message: data.message || data.type || '新事件',
          time: formatTime(new Date()),
          level: data.level || 'info'
        })
      } catch {
        addNotification({
          message: event.data,
          time: formatTime(new Date()),
          level: 'info'
        })
      }
    }
  } catch {
    wsConnected.value = false
    scheduleReconnect()
  }
}

function scheduleReconnect() {
  if (wsReconnectTimer) clearTimeout(wsReconnectTimer)
  wsReconnectTimer = setTimeout(() => {
    connectWS()
  }, 5000)
}

function addNotification(item: NotificationItem) {
  notifications.value.unshift(item)
  if (notifications.value.length > 50) {
    notifications.value.pop()
  }
}

function clearNotifications() {
  notifications.value = []
}

function formatTime(date: Date): string {
  const h = String(date.getHours()).padStart(2, '0')
  const m = String(date.getMinutes()).padStart(2, '0')
  const s = String(date.getSeconds()).padStart(2, '0')
  return `${h}:${m}:${s}`
}

function handleCommand(command: string) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}

onMounted(() => {
  connectWS()
})

onUnmounted(() => {
  if (ws) {
    ws.close()
    ws = null
  }
  if (wsReconnectTimer) {
    clearTimeout(wsReconnectTimer)
    wsReconnectTimer = null
  }
})
</script>

<style scoped lang="scss">
$sidebar-bg: #0c1a2e;
$sidebar-hover: #132844;
$sidebar-active: #0d3b7a;
$header-bg: #ffffff;
$header-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
$accent-blue: #409eff;
$text-muted: #8ca0b8;

.layout-container {
  height: 100vh;
  overflow: hidden;
}

/* ─── Sidebar ─── */
.sidebar {
  background-color: $sidebar-bg;
  transition: width 0.28s ease;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  border-right: 1px solid #0a1525;
}

.sidebar-header {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 0 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.06);
  flex-shrink: 0;

  .logo-icon {
    display: flex;
    align-items: center;
    justify-content: center;
    color: $accent-blue;
    flex-shrink: 0;
  }

  .logo-text {
    color: #e4eaf2;
    font-size: 17px;
    font-weight: 700;
    letter-spacing: 2px;
    white-space: nowrap;
  }
}

.sidebar-scroll {
  flex: 1;
  overflow: hidden;

  :deep(.el-scrollbar__wrap) {
    overflow-x: hidden;
  }
}

:deep(.el-menu) {
  border-right: none !important;
  background-color: $sidebar-bg !important;

  .el-menu-item {
    height: 44px;
    line-height: 44px;
    font-size: 13px;
    border-radius: 0;
    margin: 0;
    padding-left: 20px !important;

    &:hover {
      background-color: $sidebar-hover !important;
    }

    &.is-active {
      background-color: $sidebar-active !important;
      color: #fff !important;
      position: relative;

      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 0;
        bottom: 0;
        width: 3px;
        background: $accent-blue;
        border-radius: 0 2px 2px 0;
      }
    }

    .el-icon {
      font-size: 16px;
      margin-right: 10px;
    }
  }

  .el-menu-item-group {
    .el-menu-item-group__title {
      padding: 12px 20px 4px !important;
      font-size: 11px;
      color: #4a6278 !important;
      text-transform: uppercase;
      letter-spacing: 1px;
      line-height: 1;
    }
  }

  &.el-menu--collapse {
    .el-menu-item {
      padding-left: 0 !important;
      justify-content: center;
    }
  }
}

.group-title {
  font-size: 11px;
  color: #4a6278;
  letter-spacing: 1px;
}

/* ─── Main Container ─── */
.main-container {
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* ─── Header ─── */
.header {
  background: $header-bg;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: $header-shadow;
  z-index: 10;
  flex-shrink: 0;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;

  &:hover {
    color: $accent-blue;
  }
}

.breadcrumb {
  font-size: 13px;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 20px;
}

/* ─── WebSocket Status ─── */
.ws-status {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: #909399;

  .ws-dot {
    width: 8px;
    height: 8px;
    border-radius: 50%;
    background-color: #f56c6c;
    transition: background-color 0.3s;
  }

  &.connected .ws-dot {
    background-color: #67c23a;
    box-shadow: 0 0 6px rgba(103, 194, 58, 0.6);
  }

  .ws-label {
    font-size: 12px;
  }
}

/* ─── Notification ─── */
.notification-badge {
  line-height: 1;

  :deep(.el-badge__content) {
    font-size: 11px;
  }
}

.notification-bell {
  cursor: pointer;
  color: #606266;
  transition: color 0.2s;

  &:hover {
    color: $accent-blue;
  }
}

.notification-panel {
  .notification-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding-bottom: 10px;
    border-bottom: 1px solid #ebeef5;
    margin-bottom: 8px;

    .notification-title {
      font-weight: 600;
      font-size: 14px;
      color: #303133;
    }
  }

  .notification-empty {
    text-align: center;
    color: #909399;
    padding: 24px 0;
    font-size: 13px;
  }

  .notification-item {
    display: flex;
    align-items: flex-start;
    gap: 10px;
    padding: 10px 4px;
    border-bottom: 1px solid #f5f5f5;

    &:last-child {
      border-bottom: none;
    }

    .notification-item-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      margin-top: 5px;
      flex-shrink: 0;

      &.info { background-color: #909399; }
      &.success { background-color: #67c23a; }
      &.warning { background-color: #e6a23c; }
      &.error { background-color: #f56c6c; }
    }

    .notification-item-content {
      flex: 1;
      min-width: 0;
    }

    .notification-item-text {
      font-size: 13px;
      color: #303133;
      line-height: 1.4;
      word-break: break-all;
    }

    .notification-item-time {
      font-size: 11px;
      color: #909399;
      margin-top: 4px;
    }
  }
}

/* ─── User ─── */
.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 6px;
  transition: background-color 0.2s;

  &:hover {
    background-color: #f5f7fa;
  }

  .user-avatar {
    background-color: $accent-blue;
    color: #fff;
    font-size: 13px;
    font-weight: 600;
  }

  .user-name {
    font-size: 13px;
    color: #303133;
    font-weight: 500;
  }

  .user-arrow {
    font-size: 12px;
    color: #909399;
  }
}

/* ─── Main Content ─── */
.main-content {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}

/* ─── Transitions ─── */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.28s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

/* ─── Responsive ─── */
@media (max-width: 768px) {
  .sidebar {
    position: fixed;
    z-index: 2000;
    height: 100vh;
  }

  .user-name,
  .ws-label {
    display: none;
  }

  .breadcrumb {
    display: none;
  }
}
</style>
