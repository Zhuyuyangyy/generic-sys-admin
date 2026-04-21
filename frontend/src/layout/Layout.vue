<template>
  <el-container class="layout-container">
    <!-- 侧边栏：动态菜单 -->
    <el-aside width="220px" class="sidebar">
      <div class="logo">
        <span class="logo-text">通用管理系统</span>
      </div>
      <el-scrollbar class="menu-scrollbar">
        <el-menu
          :default-active="activeMenu"
          router
          background-color="#1f1f1f"
          text-color="#a0a0a0"
          active-text-color="#ffffff"
          :collapse="sidebarCollapsed"
          :collapse-transition="false"
        >
          <template v-for="item in menuTree" :key="item.id">
            <el-menu-item
              v-if="item.menuType === 1 && item.visible === 1"
              :index="resolvePath(item.path)"
            >
              <el-icon v-if="item.icon">
                <component :is="getIconComponent(item.icon)" />
              </el-icon>
              <span>{{ item.name }}</span>
            </el-menu-item>
            <el-sub-menu
              v-else-if="item.menuType === 0 && item.visible === 1 && item.children?.length"
              :index="String(item.id)"
            >
              <template #title>
                <el-icon v-if="item.icon">
                  <component :is="getIconComponent(item.icon)" />
                </el-icon>
                <span>{{ item.name }}</span>
              </template>
              <el-menu-item
                v-for="child in item.children"
                :key="child.id"
                v-show="child.visible === 1 && child.menuType === 1"
                :index="resolvePath(child.path)"
              >
                <el-icon v-if="child.icon">
                  <component :is="getIconComponent(child.icon)" />
                </el-icon>
                <span>{{ child.name }}</span>
              </el-menu-item>
            </el-sub-menu>
          </template>
        </el-menu>
      </el-scrollbar>

      <!-- 折叠按钮 -->
      <div class="sidebar-toggle" @click="sidebarCollapsed = !sidebarCollapsed">
        <el-icon :size="18">
          <DArrowLeft v-if="!sidebarCollapsed" />
          <DArrowRight v-else />
        </el-icon>
      </div>
    </el-aside>

    <el-container>
      <!-- 顶部导航 -->
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand" trigger="click">
            <span class="user-info">
              <el-avatar :size="32" style="background: linear-gradient(135deg, #667eea, #764ba2);">
                {{ userStore.userInfo?.username?.charAt(0)?.toUpperCase() || 'U' }}
              </el-avatar>
              <span class="username">{{ userStore.userInfo?.username || '用户' }}</span>
              <span class="real-name" v-if="userStore.userInfo?.realName">
                ({{ userStore.userInfo.realName }})
              </span>
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="profile">个人资料</el-dropdown-item>
                <el-dropdown-item command="password">修改密码</el-dropdown-item>
                <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </el-header>

      <!-- 主内容区 -->
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
/**
 * 主布局组件 - 动态菜单版
 * @description 通用后台管理布局，从后端动态加载菜单树，侧边栏可折叠
 */
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { getCurrentUserMenus, type SysMenuVO } from '@/api/menu'
import {
  DataAnalysis, Box, Goods, User, MagicStick,
  HomeFilled, Setting, Document, List,
  DArrowLeft, DArrowRight, Tools
} from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 动态菜单树 */
const menuTree = ref<SysMenuVO[]>([])

/** 侧边栏折叠状态 */
const sidebarCollapsed = ref(false)

/** 当前激活菜单路径 */
const activeMenu = computed(() => route.path)

/** 加载动态菜单 */
const loadMenus = async () => {
  try {
    const menus = await getCurrentUserMenus()
    menuTree.value = menus
  } catch {
    // 菜单加载失败时使用默认静态菜单
    menuTree.value = []
  }
}

/** 解析菜单路径（支持相对路径） */
const resolvePath = (path: string) => {
  if (path.startsWith('/')) return path
  return '/' + path
}

/** 图标名称 → 组件映射（覆盖常见Element Plus图标） */
const iconMap: Record<string, object> = {
  'DataAnalysis': DataAnalysis,
  'Box': Box,
  'Goods': Goods,
  'User': User,
  'MagicStick': MagicStick,
  'HomeFilled': HomeFilled,
  'Setting': Setting,
  'Document': Document,
  'List': List,
  'Tools': Tools,
}

/** 根据图标名获取图标组件 */
const getIconComponent = (iconName: string) => {
  return iconMap[iconName] || Document
}

/**
 * 下拉菜单命令处理
 */
const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'profile') {
    router.push('/profile')
  } else if (command === 'password') {
    router.push('/password')
  }
}

onMounted(() => {
  loadMenus()
})
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
  overflow: hidden;
}

.sidebar {
  background-color: #1f1f1f;
  display: flex;
  flex-direction: column;
  height: 100vh;
  transition: width 0.3s ease;

  .logo {
    height: 60px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-bottom: 1px solid #2a2a2a;
    flex-shrink: 0;

    .logo-text {
      font-size: 16px;
      font-weight: 700;
      background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
      background-clip: text;
      letter-spacing: 0.05em;
    }
  }

  .menu-scrollbar {
    flex: 1;
    overflow-y: auto;
    overflow-x: hidden;

    &::-webkit-scrollbar {
      width: 4px;
    }
    &::-webkit-scrollbar-thumb {
      background: #444;
      border-radius: 2px;
    }
  }

  .el-menu {
    border-right: none;
    background-color: transparent;

    .el-menu-item,
    .el-sub-menu__title {
      &:hover {
        background-color: #2a2a2a !important;
        color: #fff;
      }
    }

    .el-menu-item.is-active {
      background: linear-gradient(90deg, rgba(102, 126, 234, 0.15), transparent);
      border-left: 3px solid #667eea;
      color: #fff;
    }
  }

  .sidebar-toggle {
    height: 48px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-top: 1px solid #2a2a2a;
    color: #666;
    cursor: pointer;
    flex-shrink: 0;
    transition: color 0.2s;

    &:hover {
      color: #fff;
      background: #2a2a2a;
    }
  }
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.08);
  height: 60px;

  .header-left {
    display: flex;
    align-items: center;
  }

  .header-right {
    .user-info {
      display: flex;
      align-items: center;
      gap: 10px;
      cursor: pointer;
      padding: 6px 12px;
      border-radius: 8px;
      transition: background 0.2s;

      &:hover {
        background: #f5f5f5;
      }

      .username {
        font-weight: 600;
        color: #333;
        font-size: 14px;
      }

      .real-name {
        color: #888;
        font-size: 12px;
      }
    }
  }
}

.main-content {
  background: #f0f2f5;
  padding: 20px;
  overflow-y: auto;
}
</style>