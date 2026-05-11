<template>
  <el-config-provider :locale="zhCn">
    <!-- Login page: no sidebar -->
    <router-view v-if="isLoginPage" />
    <!-- Main app: with sidebar -->
    <el-container v-else class="app-container">
      <el-aside width="220px" class="sidebar">
        <div class="logo">
          <el-icon><Reading /></el-icon>
          <span>知识库问答</span>
        </div>
        <el-menu
          :default-active="activeMenu"
          class="sidebar-menu"
          router
        >
          <el-menu-item index="/chat">
            <el-icon><ChatDotRound /></el-icon>
            <span>智能问答</span>
          </el-menu-item>
          <el-menu-item index="/documents">
            <el-icon><Document /></el-icon>
            <span>文档管理</span>
          </el-menu-item>
          <el-sub-menu index="/system" v-if="hasSystemAccess">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/system/user" v-if="hasPerm('system:user:list')">用户管理</el-menu-item>
            <el-menu-item index="/system/account" v-if="hasPerm('system:account:list')">账号管理</el-menu-item>
            <el-menu-item index="/system/role" v-if="hasPerm('system:role:list')">角色管理</el-menu-item>
            <el-menu-item index="/system/dept" v-if="hasPerm('system:dept:list')">部门管理</el-menu-item>
            <el-menu-item index="/system/menu" v-if="hasPerm('system:menu:list')">菜单管理</el-menu-item>
            <el-menu-item index="/system/region" v-if="hasPerm('system:region:list')">地区管理</el-menu-item>
          </el-sub-menu>
        </el-menu>
        <div class="sidebar-footer">
          <span class="user-name">{{ userName }}</span>
          <el-button type="danger" link size="small" @click="handleLogout">退出</el-button>
        </div>
      </el-aside>
      <el-main class="main-content">
        <router-view />
      </el-main>
    </el-container>
  </el-config-provider>
</template>

<script setup>
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import zhCn from 'element-plus/dist/locale/zh-cn.mjs'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const isLoginPage = computed(() => route.path === '/login')
const activeMenu = computed(() => route.path)
const userName = computed(() => authStore.userInfo?.userName || '')
const hasSystemAccess = computed(() =>
  authStore.isSuperAdmin ||
  ['system:user:list', 'system:account:list', 'system:role:list',
   'system:dept:list', 'system:menu:list', 'system:region:list']
    .some(p => authStore.hasPermission(p))
)

const hasPerm = (perm) => authStore.hasPermission(perm)

const handleLogout = () => {
  authStore.logout()
}

onMounted(async () => {
  if (authStore.isLoggedIn && !authStore.userInfo) {
    try {
      await authStore.loadUserInfo()
    } catch {
      // Will redirect to login via store's logout action
    }
  }
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
}

.app-container {
  height: 100%;
}

.sidebar {
  background: linear-gradient(180deg, #1a1a2e 0%, #16213e 100%);
  color: #fff;
  display: flex;
  flex-direction: column;
}

.logo {
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: 18px;
  font-weight: 600;
  border-bottom: 1px solid rgba(255,255,255,0.1);
  flex-shrink: 0;
}

.sidebar-menu {
  border: none;
  background: transparent;
  flex: 1;
}

.sidebar-menu .el-menu-item {
  color: rgba(255,255,255,0.8);
}

.sidebar-menu .el-menu-item:hover,
.sidebar-menu .el-menu-item.is-active {
  background: rgba(255,255,255,0.1);
  color: #fff;
}

.sidebar-menu .el-sub-menu__title {
  color: rgba(255,255,255,0.8);
}

.sidebar-footer {
  padding: 12px 16px;
  border-top: 1px solid rgba(255,255,255,0.1);
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.sidebar-footer .user-name {
  color: rgba(255,255,255,0.8);
  font-size: 14px;
}

.main-content {
  background: #f5f7fa;
  padding: 0;
}
</style>
