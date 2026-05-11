import { defineStore } from 'pinia'
import { login as loginApi, getUserInfo, getUserMenus } from '@/api/auth'
import router from '@/router'

export const useAuthStore = defineStore('auth', {
  state: () => ({
    token: localStorage.getItem('accessToken') || null,
    userInfo: null,
    permissions: [],
    menus: []
  }),

  getters: {
    isLoggedIn: (state) => !!state.token,
    isSuperAdmin: (state) => state.userInfo?.isSuperAdmin === 1,
    hasPermission: (state) => (perm) => {
      if (state.userInfo?.isSuperAdmin === 1) return true
      return state.permissions.includes(perm)
    }
  },

  actions: {
    async login(credentials) {
      const res = await loginApi(credentials)
      this.token = res.data.accessToken
      this.userInfo = res.data
      this.permissions = res.data.permissions || []
      localStorage.setItem('accessToken', res.data.accessToken)
      return res.data
    },

    async loadUserInfo() {
      try {
        const res = await getUserInfo()
        this.userInfo = res.data
        this.permissions = res.data.permissions || []
        const menuRes = await getUserMenus()
        this.menus = menuRes.data || []
      } catch {
        this.logout()
      }
    },

    logout() {
      this.token = null
      this.userInfo = null
      this.permissions = []
      this.menus = []
      localStorage.removeItem('accessToken')
      router.push('/login')
    }
  }
})
