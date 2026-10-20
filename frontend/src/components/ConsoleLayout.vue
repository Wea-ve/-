<template>
  <div class="console" v-if="auth.user">
    <!-- 顶栏 -->
    <header class="topbar">
      <div class="topbar-left">
        <el-button text class="burger" @click="drawer = true">
          <el-icon :size="22"><Fold /></el-icon>
        </el-button>
        <div class="brand">🌾 农芯溯源</div>
        <el-tag size="small" effect="dark" round class="role-tag">{{ roleInfo.name }}端</el-tag>
      </div>
      <div class="topbar-right">
        <div class="clock">{{ clock }}</div>
        <!-- 管理员专属：待处理投诉提醒铃铛 -->
        <el-badge v-if="isAdmin" :value="pendingCount" :hidden="pendingCount === 0" class="bell"
                  :class="{ shake: bellShake }">
          <el-button text @click="goPending">
            <el-icon :size="20"><Bell /></el-icon>
          </el-button>
        </el-badge>
        <el-dropdown trigger="click" @command="onCommand">
          <div class="avatar-wrap">
            <el-avatar :size="34" :src="auth.user.avatar || undefined" class="avatar">
              {{ (auth.user.realName || auth.user.username || '?').slice(0, 1) }}
            </el-avatar>
            <span class="avatar-name">{{ auth.user.realName || auth.user.username }}</span>
            <el-icon><ArrowDown /></el-icon>
          </div>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="profile"><el-icon><User /></el-icon>个人中心</el-dropdown-item>
              <el-dropdown-item divided command="logout"><el-icon><SwitchButton /></el-icon>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </header>

    <!-- 全屏隐藏式抽屉菜单 -->
    <transition name="drawer">
      <div v-if="drawer" class="drawer-mask" @click.self="drawer = false">
        <aside class="drawer-panel">
          <div class="drawer-head">
            <div class="drawer-brand">🌾 农芯溯源</div>
            <el-button text circle class="drawer-close" @click="drawer = false">
              <el-icon :size="20"><Close /></el-icon>
            </el-button>
          </div>
          <div class="drawer-user">
            <el-avatar :size="42" :src="auth.user.avatar || undefined">
              {{ (auth.user.realName || auth.user.username || '?').slice(0, 1) }}
            </el-avatar>
            <div>
              <div class="drawer-name">{{ auth.user.realName || auth.user.username }}</div>
              <div class="drawer-role">{{ roleInfo.name }} · {{ auth.user.username }}</div>
            </div>
          </div>
          <nav class="drawer-menu">
            <div v-for="item in menus" :key="item.path"
                 class="menu-item" :class="{ active: isActive(item) }"
                 @click="go(item)">
              <span class="menu-icon">{{ item.icon }}</span>
              <span>{{ item.title }}</span>
            </div>
          </nav>
          <div class="drawer-foot">从田间到餐桌 · 每一步都可信</div>
        </aside>
      </div>
    </transition>

    <!-- 主内容 -->
    <main class="content">
      <router-view v-slot="{ Component }">
        <transition name="fade-slide" mode="out-in">
          <component :is="Component" :key="$route.fullPath" />
        </transition>
      </router-view>
    </main>

    <!-- 个人中心弹窗 -->
    <el-dialog v-model="profileVisible" title="个人中心" width="460px">
      <el-tabs v-model="profileTab">
        <el-tab-pane label="基本信息" name="info">
          <div class="avatar-row">
            <el-avatar :size="64" :src="auth.user.avatar || undefined">
              {{ (auth.user.realName || auth.user.username || '?').slice(0, 1) }}
            </el-avatar>
            <el-upload :show-file-list="false" :auto-upload="false" accept=".png,.jpg,.jpeg,.gif"
                       :on-change="onAvatarChange">
              <el-button size="small">更换头像</el-button>
            </el-upload>
          </div>
          <el-form label-width="60px" style="margin-top: 12px">
            <el-form-item label="账号"><el-input v-model="auth.user.username" disabled /></el-form-item>
            <el-form-item label="姓名"><el-input v-model="profileForm.realName" /></el-form-item>
            <el-form-item label="电话"><el-input v-model="profileForm.phone" /></el-form-item>
            <el-form-item label="地址"><el-input v-model="profileForm.address" type="textarea" :rows="2" /></el-form-item>
          </el-form>
          <div style="text-align: right">
            <el-button type="primary" :loading="saving" @click="saveProfile">保存</el-button>
          </div>
        </el-tab-pane>
        <el-tab-pane label="修改密码" name="pwd">
          <el-form label-width="80px">
            <el-form-item label="原密码"><el-input v-model="pwdForm.oldPassword" type="password" show-password /></el-form-item>
            <el-form-item label="新密码"><el-input v-model="pwdForm.newPassword" type="password" show-password /></el-form-item>
            <el-form-item label="确认密码"><el-input v-model="pwdForm.confirm" type="password" show-password /></el-form-item>
          </el-form>
          <div style="text-align: right">
            <el-button type="primary" :loading="savingPwd" @click="savePwd">修改</el-button>
          </div>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<script setup>
import { computed, onBeforeUnmount, onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { auth, logout, roleMap } from '../store/auth'

const route = useRoute()
const router = useRouter()

const roleInfo = computed(() => roleMap[auth.user?.role] || {})
const isAdmin = computed(() => auth.user?.role === 'admin')

// 菜单配置（按角色）
const MENU = {
  farmer: [
    { path: '/farmer/dashboard', title: '工作台', icon: '🏠' },
    { path: '/farmer/batch', title: '种植批次', icon: '🌱' },
    { path: '/farmer/logs', title: '农事记录', icon: '📓' },
    { path: '/farmer/trace-codes', title: '溯源码', icon: '🔖' },
    { path: '/farmer/reports', title: '检测报告', icon: '📋' },
    { path: '/farmer/sales', title: '销售记录', icon: '💰' }
  ],
  dealer: [
    { path: '/dealer/dashboard', title: '工作台', icon: '🏠' },
    { path: '/dealer/inbound', title: '收货入库', icon: '📥' },
    { path: '/dealer/outbound', title: '发货出库', icon: '📤' },
    { path: '/dealer/inbound-records', title: '入库记录', icon: '📄' },
    { path: '/dealer/outbound-records', title: '出库记录', icon: '📑' },
    { path: '/dealer/records', title: '经手批次', icon: '📦' },
    { path: '/dealer/trace', title: '溯源查询', icon: '🔍' }
  ],
  admin: [
    { path: '/admin/dashboard', title: '数据大屏', icon: '📊' },
    { path: '/admin/bigscreen', title: '可视化大屏', icon: '🖥️' },
    { path: '/admin/users', title: '用户管理', icon: '👥' },
    { path: '/admin/complaints', title: '投诉工单', icon: '📨' },
    { path: '/admin/batches', title: '批次监管', icon: '🌾' },
    { path: '/admin/farming-logs', title: '农事监管', icon: '📓' },
    { path: '/admin/reports', title: '检测报告', icon: '📋' },
    { path: '/admin/trace-records', title: '流转记录', icon: '🔄' },
    { path: '/admin/trace', title: '溯源查询', icon: '🔍' },
    { path: '/admin/scan-stats', title: '扫码统计', icon: '📱' }
  ]
}
const menus = computed(() => MENU[auth.user?.role] || [])

const isActive = (item) => route.path.startsWith(item.path)
const go = (item) => {
  drawer.value = false
  if (!isActive(item)) router.push(item.path)
}

// 抽屉菜单
const drawer = ref(false)

// 实时时钟
const clock = ref('')
let clockTimer = null
function tick() {
  const d = new Date()
  const p = (n) => String(n).padStart(2, '0')
  clock.value = `${d.getFullYear()}-${p(d.getMonth() + 1)}-${p(d.getDate())} ${p(d.getHours())}:${p(d.getMinutes())}:${p(d.getSeconds())}`
}

// 管理员铃铛：待处理投诉数
const pendingCount = ref(0)
const bellShake = ref(false)
async function loadPending() {
  if (!isAdmin.value) return
  try {
    const d = await http.get('/api/admin/pending-count')
    if (d.count !== pendingCount.value) {
      bellShake.value = true
      setTimeout(() => (bellShake.value = false), 800)
    }
    pendingCount.value = d.count
  } catch { /* 忽略 */ }
}
const goPending = () => {
  ElMessage.info(`有 ${pendingCount.value} 个待处理投诉工单`)
  router.push('/admin/complaints')
}

// 个人中心
const profileVisible = ref(false)
const profileTab = ref('info')
const profileForm = reactive({ realName: '', phone: '', address: '' })
const pwdForm = reactive({ oldPassword: '', newPassword: '', confirm: '' })
const saving = ref(false)
const savingPwd = ref(false)

function openProfile() {
  profileForm.realName = auth.user.realName || ''
  profileForm.phone = auth.user.phone || ''
  profileForm.address = auth.user.address || ''
  pwdForm.oldPassword = pwdForm.newPassword = pwdForm.confirm = ''
  profileTab.value = 'info'
  profileVisible.value = true
}

async function onAvatarChange(file) {
  const fd = new FormData()
  fd.append('file', file.raw)
  try {
    const d = await http.post('/api/auth/avatar', fd)
    auth.user.avatar = d.avatar
    ElMessage.success('头像已更新')
  } catch { /* 拦截器已提示 */ }
}

async function saveProfile() {
  if (!profileForm.realName.trim()) return ElMessage.warning('姓名不能为空')
  saving.value = true
  try {
    const u = await http.put('/api/auth/profile', { ...profileForm })
    auth.user = u
    ElMessage.success('个人信息已保存')
    profileVisible.value = false
  } catch { /* 拦截器已提示 */ } finally { saving.value = false }
}

async function savePwd() {
  if (pwdForm.newPassword.length < 6) return ElMessage.warning('新密码长度至少 6 位')
  if (pwdForm.newPassword !== pwdForm.confirm) return ElMessage.warning('两次输入的新密码不一致')
  savingPwd.value = true
  try {
    await http.put('/api/auth/password', { oldPassword: pwdForm.oldPassword, newPassword: pwdForm.newPassword })
    ElMessage.success('密码修改成功')
    profileVisible.value = false
  } catch { /* 拦截器已提示 */ } finally { savingPwd.value = false }
}

async function onCommand(cmd) {
  if (cmd === 'profile') openProfile()
  if (cmd === 'logout') {
    await ElMessageBox.confirm('确定退出登录吗？', '提示', { type: 'warning', confirmButtonText: '退出', cancelButtonText: '取消' })
    await logout()
    router.push('/login')
  }
}

let bellTimer = null
onMounted(() => {
  tick()
  clockTimer = setInterval(tick, 1000)
  loadPending()
  bellTimer = setInterval(loadPending, 60000)
})
onBeforeUnmount(() => {
  clearInterval(clockTimer)
  clearInterval(bellTimer)
})
</script>

<style scoped>
.console { min-height: 100vh; }

/* ===== 顶栏 ===== */
.topbar {
  position: sticky; top: 0; z-index: 100;
  display: flex; align-items: center; justify-content: space-between;
  height: 56px; padding: 0 16px;
  background: rgba(255, 255, 255, .92); backdrop-filter: blur(8px);
  box-shadow: 0 1px 8px rgba(31, 66, 39, .08);
}
.topbar-left, .topbar-right { display: flex; align-items: center; gap: 12px; }
.burger { padding: 6px; }
.brand { font-size: 17px; font-weight: 700; color: #1b5e20; letter-spacing: 1px; }
.role-tag { background: linear-gradient(135deg, #43a047, #1b5e20); border: none; }
.clock { font-size: 13px; color: #5c6f60; font-variant-numeric: tabular-nums; }
.bell { cursor: pointer; }
.bell.shake :deep(.el-button) { animation: bellShake .8s; }
@keyframes bellShake {
  0%, 100% { transform: rotate(0); }
  20% { transform: rotate(14deg); }
  40% { transform: rotate(-12deg); }
  60% { transform: rotate(8deg); }
  80% { transform: rotate(-6deg); }
}
.avatar-wrap { display: flex; align-items: center; gap: 8px; cursor: pointer; padding: 4px 6px; border-radius: 20px; }
.avatar-wrap:hover { background: #eef5ef; }
.avatar { background: linear-gradient(135deg, #43a047, #1b5e20); font-weight: 600; }
.avatar-name { font-size: 13.5px; color: #34483a; max-width: 90px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ===== 抽屉菜单 ===== */
.drawer-mask {
  position: fixed; inset: 0; z-index: 1500;
  background: rgba(15, 35, 20, .45);
}
.drawer-panel {
  position: absolute; left: 0; top: 0; bottom: 0;
  width: 260px; max-width: 82vw;
  background: #fff; display: flex; flex-direction: column;
  box-shadow: 8px 0 30px rgba(0, 0, 0, .18);
}
.drawer-enter-active, .drawer-leave-active { transition: opacity .22s ease; }
.drawer-enter-active .drawer-panel, .drawer-leave-active .drawer-panel { transition: transform .26s ease; }
.drawer-enter-from, .drawer-leave-to { opacity: 0; }
.drawer-enter-from .drawer-panel, .drawer-leave-to .drawer-panel { transform: translateX(-100%); }

.drawer-head {
  display: flex; align-items: center; justify-content: space-between;
  padding: 16px 14px 12px;
  background: linear-gradient(135deg, #1b5e20, #2e7d32);
}
.drawer-brand { color: #fff; font-size: 17px; font-weight: 700; letter-spacing: 2px; }
.drawer-close { color: #fff; }
.drawer-user {
  display: flex; align-items: center; gap: 12px;
  padding: 16px; background: #f2f8f2; border-bottom: 1px solid #e4efe5;
}
.drawer-name { font-weight: 600; color: #23422a; }
.drawer-role { font-size: 12px; color: #7d8f81; }
.drawer-menu { flex: 1; overflow-y: auto; padding: 10px; }
.menu-item {
  display: flex; align-items: center; gap: 12px;
  padding: 12px 14px; border-radius: 10px;
  color: #3c5244; font-size: 14.5px; cursor: pointer;
  transition: all .18s ease; margin-bottom: 4px;
}
.menu-item:hover { background: #eef7ef; transform: translateX(3px); }
.menu-item.active {
  background: linear-gradient(135deg, #43a047, #2e7d32);
  color: #fff; font-weight: 600;
  box-shadow: 0 4px 12px rgba(46, 125, 50, .3);
}
.menu-icon { font-size: 17px; }
.drawer-foot { padding: 14px; text-align: center; font-size: 11.5px; color: #9aab9d; letter-spacing: 1px; }

/* ===== 主内容 ===== */
.content { padding: 20px 16px 40px; }

@media (max-width: 768px) {
  .clock, .avatar-name, .role-tag { display: none; }
}
</style>
