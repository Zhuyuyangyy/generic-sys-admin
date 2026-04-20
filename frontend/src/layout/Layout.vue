<template>
  <el-container class="layout-container">
    <!-- 侧边栏：动态菜单 -->
    <el-aside width="200px" class="sidebar">
      <div class="logo">通用管理系统</div>
      <el-menu
        :default-active="activeMenu"
        router
        background-color="#304156"
        text-color="#bfcbd9"
        active-text-color="#409EFF"
      >
        <el-menu-item index="/dashboard">
          <el-icon><DataAnalysis /></el-icon>
          <span>仪表盘</span>
        </el-menu-item>
        <el-menu-item index="/equipment">
          <el-icon><Box /></el-icon>
          <span>设备管理</span>
        </el-menu-item>
        <el-menu-item index="/consumables">
          <el-icon><Goods /></el-icon>
          <span>耗材管理</span>
        </el-menu-item>
        <el-menu-item index="/ai-studio">
          <el-icon><MagicStick /></el-icon>
          <span>智能演播厅</span>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <!-- 顶部导航栏 -->
      <el-header class="header">
        <div class="header-left">
          <el-breadcrumb separator="/">
            <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
            <el-breadcrumb-item v-if="route.meta.title">{{ route.meta.title }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <div class="header-right">
          <el-dropdown @command="handleCommand">
            <span class="user-info">
              <el-icon><User /></el-icon>
              {{ userStore.userInfo?.username || '用户' }}
            </span>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="logout">退出登录</el-dropdown-item>
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
 * 主布局组件
 * @description 通用后台管理布局，包含侧边栏菜单、顶部导航（面包屑+用户信息）、主内容区路由出口
 */
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/store/user'
import { DataAnalysis, Box, Goods, User, MagicStick } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

/** 当前激活菜单路径（用于侧边栏高亮） */
const activeMenu = computed(() => route.path)

/**
 * 下拉菜单命令处理
 * @param command - 菜单命令标识
 */
const handleCommand = (command: string) => {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  }
}
</script>

<style scoped lang="scss">
.layout-container {
  height: 100vh;
}

.sidebar {
  background-color: #304156;
  .logo {
    height: 60px;
    line-height: 60px;
    text-align: center;
    color: #fff;
    font-size: 16px;
    font-weight: bold;
    border-bottom: 1px solid #3a4a5c;
  }
  .el-menu {
    border-right: none;
  }
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
  box-shadow: 0 1px 4px rgba(0,0,0,.08);
  .user-info {
    display: flex;
    align-items: center;
    gap: 6px;
    cursor: pointer;
  }
}

.main-content {
  background: #f0f2f5;
  padding: 20px;
}
</style>
