<template>
  <el-menu
    :default-active="activeMenu"
    :collapse="isCollapse"
    :collapse-transition="false"
    :unique-opened="true"
    class="dynamic-menu"
    background-color="#304156"
    text-color="#bfcbd9"
    active-text-color="#409EFF"
  >
    <template v-for="menu in menuList" :key="menu.id">
      <!-- 二级菜单 -->
      <el-sub-menu v-if="menu.children && menu.children.length > 0" :index="menu.path">
        <template #title>
          <el-icon><component :is="menu.icon" /></el-icon>
          <span>{{ menu.name }}</span>
        </template>
        <template v-for="child in menu.children" :key="child.id">
          <el-menu-item
            :index="child.path"
            @click="handleMenuClick(child)"
          >
            <el-icon><component :is="child.icon" /></el-icon>
            <span>{{ child.name }}</span>
          </el-menu-item>
        </template>
      </el-sub-menu>

      <!-- 一级菜单 -->
      <el-menu-item v-else :index="menu.path" @click="handleMenuClick(menu)">
        <el-icon><component :is="menu.icon" /></el-icon>
        <template #title>
          <span>{{ menu.name }}</span>
        </template>
      </el-menu-item>
    </template>
  </el-menu>
</template>

<script setup lang="ts">
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useMenuStore } from '@/stores/menu'
import type { MenuItem } from '@/types/menu'

const router = useRouter()
const route = useRoute()
const menuStore = useMenuStore()

const menuList = computed<MenuItem[]>(() => menuStore.menuList)
const isCollapse = computed(() => menuStore.isCollapse)

const activeMenu = computed(() => route.path)

const handleMenuClick = (menu: MenuItem) => {
  if (menu.isExternal) {
    window.open(menu.path, '_blank')
  } else {
    router.push(menu.path)
  }
}

onMounted(async () => {
  await menuStore.fetchMenuTree()
})
</script>

<style scoped>
.dynamic-menu {
  height: 100%;
  border-right: none;
}
.dynamic-menu:not(.el-menu--collapse) {
  width: 210px;
}
</style>
