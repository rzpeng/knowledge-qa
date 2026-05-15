import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/login/LoginView.vue')
  },
  {
    path: '/',
    redirect: '/chat'
  },
  {
    path: '/chat',
    name: 'Chat',
    component: () => import('@/views/ChatView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/documents',
    name: 'Documents',
    component: () => import('@/views/DocumentsView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/agent',
    name: 'Agent',
    component: () => import('@/views/AgentView.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/system',
    redirect: '/system/user',
    meta: { requiresAuth: true }
  },
  {
    path: '/system/user',
    name: 'User',
    component: () => import('@/views/system/UserView.vue'),
    meta: { requiresAuth: true, permission: 'system:user:list' }
  },
  {
    path: '/system/account',
    name: 'Account',
    component: () => import('@/views/system/AccountView.vue'),
    meta: { requiresAuth: true, permission: 'system:account:list' }
  },
  {
    path: '/system/role',
    name: 'Role',
    component: () => import('@/views/system/RoleView.vue'),
    meta: { requiresAuth: true, permission: 'system:role:list' }
  },
  {
    path: '/system/dept',
    name: 'Dept',
    component: () => import('@/views/system/DeptView.vue'),
    meta: { requiresAuth: true, permission: 'system:dept:list' }
  },
  {
    path: '/system/menu',
    name: 'Menu',
    component: () => import('@/views/system/MenuView.vue'),
    meta: { requiresAuth: true, permission: 'system:menu:list' }
  },
  {
    path: '/system/region',
    name: 'Region',
    component: () => import('@/views/system/RegionView.vue'),
    meta: { requiresAuth: true, permission: 'system:region:list' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('accessToken')
  if (to.meta.requiresAuth && !token) {
    next('/login')
  } else if (to.path === '/login' && token) {
    next('/')
  } else {
    next()
  }
})

export default router
