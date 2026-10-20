import { reactive } from 'vue'
import http from '../api/http'

/** 全局登录态（简单响应式单例，未引入 pinia） */
export const auth = reactive({
  user: null,   // { id, username, realName, role, phone, address, avatar }
  loaded: false
})

export const roleMap = {
  farmer: { name: '农户', icon: '🧑‍🌾', home: '/farmer/dashboard' },
  dealer: { name: '经销商', icon: '🚚', home: '/dealer/dashboard' },
  admin: { name: '管理员', icon: '🛡️', home: '/admin/dashboard' }
}

export async function fetchMe(force = false) {
  if (auth.loaded && !force) return auth.user
  try {
    auth.user = await http.get('/api/auth/me')
  } catch {
    auth.user = null
  }
  auth.loaded = true
  return auth.user
}

export async function login(username, password, role) {
  const user = await http.post('/api/auth/login', { username, password, role })
  auth.user = user
  auth.loaded = true
  return user
}

export async function logout() {
  try { await http.post('/api/auth/logout') } catch { /* 忽略 */ }
  auth.user = null
  auth.loaded = true
}
