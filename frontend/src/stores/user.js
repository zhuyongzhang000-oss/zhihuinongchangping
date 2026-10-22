import { defineStore } from 'pinia'
import { authApi } from '@/api'
import { TOKEN_KEY } from '@/api/request'

export const useUserStore = defineStore('user', {
  state: () => ({ token: localStorage.getItem(TOKEN_KEY) || '', user: null }),
  getters: {
    role: s => s.user?.role,
    isAdmin: s => s.user?.role === 'ADMIN'
  },
  actions: {
    async login(form) {
      const res = await authApi.login(form)
      this.token = res.token
      this.user = res.user
      localStorage.setItem(TOKEN_KEY, res.token)
    },
    async fetchMe() {
      this.user = await authApi.me()
      return this.user
    },
    /** 管理员拥有全部权限；roles 为空表示所有登录用户 */
    can(...roles) {
      return !!this.user && (this.user.role === 'ADMIN' || roles.length === 0 || roles.includes(this.user.role))
    },
    logout() {
      this.token = ''
      this.user = null
      localStorage.removeItem(TOKEN_KEY)
    }
  }
})
