import { reactive } from 'vue'
import http from '../api/http'

export type Role = 'farmer' | 'dealer' | 'admin'

export interface AuthUser {
  id: number
  username: string
  realName: string
  role: Role
  phone?: string
  address?: string
  avatar?: string
}

interface AuthState {
  user: AuthUser | null
  loaded: boolean
}

interface RoleInfo {
  name: string
  icon: string
  home: string
}

/** 全局登录态（简单响应式单例，未引入 pinia） */
export const auth = reactive<AuthState>({
  user: null,
  loaded: false
})

export const roleMap: Record<Role, RoleInfo> = {
  farmer: { name: '农户', icon: '🧑‍🌾', home: '/farmer/dashboard' },
  dealer: { name: '经销商', icon: '🚚', home: '/dealer/dashboard' },
  admin: { name: '管理员', icon: '🛡️', home: '/admin/dashboard' }
}

export async function fetchMe(force = false): Promise<AuthUser | null> {
  if (auth.loaded && !force) return auth.user
  try {
    auth.user = await http.get<AuthUser>('/api/auth/me')
  } catch {
    auth.user = null
  }
  auth.loaded = true
  return auth.user
}

export async function login(username: string, password: string, role: Role): Promise<AuthUser> {
  const user = await http.post<AuthUser>('/api/auth/login', { username, password, role })
  auth.user = user
  auth.loaded = true
  return user
}

export async function logout(): Promise<void> {
  try { await http.post('/api/auth/logout') } catch { /* 忽略 */ }
  auth.user = null
  auth.loaded = true
}
