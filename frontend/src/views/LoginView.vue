<template>
  <div class="login-bg">
    <!-- 左侧品牌展示区 -->
    <aside class="brand-panel">
      <div class="brand-glow brand-glow-1"></div>
      <div class="brand-glow brand-glow-2"></div>

      <div class="brand-top">
        <div class="brand-logo">🌾</div>
        <div class="brand-title">安溯·知源</div>
        <div class="brand-sub">VeriTrace · AI 可信判断型消费信任引擎</div>
      </div>

      <div class="brand-slogan">
        <div class="slogan-line">从田间到餐桌</div>
        <div class="slogan-line slogan-line-strong">每一步都可信</div>
      </div>

      <div class="brand-features">
        <div class="feature-item">
          <span class="feature-icon">🛡️</span>
          <div>
            <div class="feature-title">AI 可信指数评分</div>
            <div class="feature-desc">红黄绿三档 · 农事 · 检测 · 流转 · 用药</div>
          </div>
        </div>
        <div class="feature-item">
          <span class="feature-icon">🩺</span>
          <div>
            <div class="feature-title">批次健康体检</div>
            <div class="feature-desc">自动跑完 5 项检查 · 风险等级 · 依据</div>
          </div>
        </div>
        <div class="feature-item">
          <span class="feature-icon">🔗</span>
          <div>
            <div class="feature-title">全链路溯源</div>
            <div class="feature-desc">扫码即查 · 批次档案 · 流转闭环 · 检测报告</div>
          </div>
        </div>
      </div>

      <div class="brand-deco" aria-hidden="true">
        <span class="leaf leaf-1">🍃</span>
        <span class="leaf leaf-2">🌿</span>
        <span class="leaf leaf-3">🍅</span>
        <span class="leaf leaf-4">🥬</span>
      </div>
      <div class="brand-bottom">安心 · 透明 · 可追溯</div>
    </aside>

    <!-- 右侧登录卡片区 -->
    <main class="login-panel">
      <div class="login-wrap">
        <div class="mobile-brand">
          <span class="mobile-logo">🌾</span> 安溯·知源
        </div>

        <div class="login-card">
          <div class="card-head">
            <div class="card-title">欢迎回来</div>
            <div class="card-sub">请选择角色通道登录管理平台</div>
          </div>

          <div class="role-tabs">
            <div v-for="(r, key) in roleMap" :key="key" class="role-tab"
                 :class="{ active: form.role === key }" @click="switchRole(key)">
              <span class="tab-icon">{{ r.icon }}</span>{{ r.name }}
            </div>
          </div>

          <div class="form-area">
            <div class="role-tip">
              当前登录：<strong>{{ roleMap[form.role].name }}</strong> 通道 · 仅本角色账号可登录
            </div>
            <el-form @submit.prevent>
              <el-form-item>
                <el-input v-model="form.username" size="large" placeholder="请输入账号" autocomplete="username">
                  <template #prefix>👤</template>
                </el-input>
              </el-form-item>
              <el-form-item>
                <el-input v-model="form.password" size="large" type="password" show-password
                          placeholder="请输入密码" autocomplete="current-password" @keyup.enter="onLogin">
                  <template #prefix>🔒</template>
                </el-input>
              </el-form-item>
              <el-button class="btn-submit" size="large" :loading="loading" @click="onLogin">登 录</el-button>
            </el-form>
          </div>
        </div>

        <div class="card-foot">
          <div class="test-tip">测试账号：farmer01 / dealer01 / admin　密码均为 123456</div>
          <div class="consumer-entry">
            <router-link to="/complaint/new" class="entry-link">📢 我要投诉</router-link>
            <span class="mx">·</span>
            <router-link to="/complaint/query" class="entry-link">🔍 投诉查询</router-link>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, roleMap, type Role } from '../store/auth'

const route = useRoute()
const router = useRouter()
const form = reactive<{ username: string; password: string; role: Role }>({ username: '', password: '', role: 'farmer' })
const loading = ref<boolean>(false)

const switchRole = (key: string): void => {
  // 仅切换角色通道，不清空已输入的账号密码（避免用户输完再点角色时被清空）
  form.role = key as Role
}

async function onLogin(): Promise<void> {
  if (!form.username.trim() || !form.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  loading.value = true
  try {
    const user = await login(form.username.trim(), form.password, form.role)
    ElMessage.success(`欢迎回来，${user.realName || user.username}`)
    const redirect = route.query.redirect
    if (redirect && typeof redirect === 'string' && redirect.startsWith('/')) {
      router.push(redirect)
    } else {
      router.push(roleMap[user.role].home)
    }
  } catch { /* 拦截器已提示 */ } finally { loading.value = false }
}
</script>

<style scoped>
.login-bg {
  min-height: 100vh;
  display: flex;
  background: #f2f6f2;
  font-family: 'PingFang SC', 'Microsoft YaHei', system-ui, sans-serif;
}

/* ========== 左侧品牌区 ========== */
.brand-panel {
  position: relative;
  flex: 1.15;
  min-width: 0;
  display: flex;
  flex-direction: column;
  justify-content: center;
  padding: 64px 6vw;
  overflow: hidden;
  color: #fff;
  background:
    linear-gradient(rgba(13, 48, 22, .55), rgba(13, 48, 22, .72)),
    url('../assets/login-bg.jpg') center / cover no-repeat,
    linear-gradient(140deg, #1b5e20 0%, #2e7d32 55%, #388e3c 100%);
}

.brand-glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(90px);
  opacity: .5;
  pointer-events: none;
}
.brand-glow-1 {
  width: 380px; height: 380px;
  background: rgba(129, 199, 132, .5);
  top: -120px; right: -80px;
  animation: glowFloat 9s ease-in-out infinite;
}
.brand-glow-2 {
  width: 300px; height: 300px;
  background: rgba(255, 213, 79, .28);
  bottom: -100px; left: -60px;
  animation: glowFloat 12s ease-in-out infinite reverse;
}
@keyframes glowFloat {
  0%, 100% { transform: translateY(0) scale(1); }
  50% { transform: translateY(24px) scale(1.06); }
}

.brand-top { position: relative; z-index: 2; }
.brand-logo {
  font-size: 62px;
  line-height: 1;
  filter: drop-shadow(0 6px 14px rgba(0, 0, 0, .3));
  animation: logoBob 4s ease-in-out infinite;
}
@keyframes logoBob {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-7px); }
}
.brand-title {
  font-size: 38px;
  font-weight: 800;
  letter-spacing: 8px;
  margin: 18px 0 8px;
  text-shadow: 0 3px 12px rgba(0, 0, 0, .35);
}
.brand-sub {
  font-size: 14px;
  letter-spacing: 2.5px;
  color: rgba(255, 255, 255, .85);
}

.brand-slogan {
  position: relative;
  z-index: 2;
  margin: 46px 0 34px;
  line-height: 1.75;
}
.slogan-line { font-size: 22px; font-weight: 600; letter-spacing: 3px; color: rgba(255, 255, 255, .92); }
.slogan-line-strong { font-size: 27px; font-weight: 800; color: #ffe082; text-shadow: 0 2px 10px rgba(0, 0, 0, .3); }

.brand-features { position: relative; z-index: 2; display: flex; flex-direction: column; gap: 20px; }
.feature-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 18px;
  border-radius: 14px;
  background: rgba(255, 255, 255, .1);
  border: 1px solid rgba(255, 255, 255, .18);
  backdrop-filter: blur(8px);
  transition: transform .25s ease, background .25s ease;
}
.feature-item:hover { transform: translateX(6px); background: rgba(255, 255, 255, .17); }
.feature-icon {
  width: 44px; height: 44px;
  flex: 0 0 auto;
  display: flex; align-items: center; justify-content: center;
  font-size: 23px;
  border-radius: 12px;
  background: rgba(255, 255, 255, .16);
}
.feature-title { font-size: 15px; font-weight: 700; letter-spacing: 1px; }
.feature-desc { font-size: 12px; color: rgba(255, 255, 255, .75); margin-top: 3px; letter-spacing: .5px; }

.brand-deco { position: absolute; inset: 0; z-index: 1; pointer-events: none; }
.leaf {
  position: absolute;
  font-size: 34px;
  opacity: .5;
  filter: drop-shadow(0 4px 8px rgba(0, 0, 0, .25));
  animation: leafFloat 7s ease-in-out infinite;
}
.leaf-1 { top: 16%; left: 8%; }
.leaf-2 { bottom: 24%; right: 12%; animation-delay: 1.6s; font-size: 28px; }
.leaf-3 { top: 38%; right: 6%; animation-delay: 3s; font-size: 24px; }
.leaf-4 { bottom: 10%; left: 22%; animation-delay: 4.2s; font-size: 30px; }
@keyframes leafFloat {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  50% { transform: translateY(-16px) rotate(8deg); }
}

.brand-bottom {
  position: absolute;
  bottom: 30px;
  left: 0; right: 0;
  text-align: center;
  z-index: 2;
  font-size: 13px;
  letter-spacing: 6px;
  color: rgba(255, 255, 255, .65);
}

/* ========== 右侧登录区 ========== */
.login-panel {
  flex: 1;
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32px 4vw;
  background:
    radial-gradient(1200px 600px at 85% -10%, rgba(76, 175, 80, .10), transparent 60%),
    radial-gradient(900px 500px at 0% 110%, rgba(255, 213, 79, .10), transparent 60%),
    #f2f6f2;
}
.login-wrap { width: 100%; max-width: 430px; }

.mobile-brand { display: none; }

.login-card {
  border-radius: 22px;
  padding: 34px 34px 30px;
  background: rgba(255, 255, 255, .94);
  backdrop-filter: blur(18px) saturate(1.3);
  border: 1px solid rgba(255, 255, 255, .9);
  box-shadow: 0 18px 55px rgba(27, 60, 30, .14), 0 2px 8px rgba(27, 60, 30, .06);
  animation: cardIn .5s ease both;
}
@keyframes cardIn {
  from { opacity: 0; transform: translateY(22px); }
  to { opacity: 1; transform: translateY(0); }
}

.card-head { text-align: center; margin-bottom: 22px; }
.card-title { font-size: 24px; font-weight: 800; color: #1b3a22; letter-spacing: 2px; }
.card-sub { font-size: 13px; color: #8a9a8e; margin-top: 6px; letter-spacing: .5px; }

.role-tabs {
  display: flex;
  background: #f0f5f0;
  border-radius: 12px;
  padding: 4px;
  gap: 4px;
}
.role-tab {
  flex: 1;
  padding: 11px 0;
  text-align: center;
  cursor: pointer;
  color: #5a6d5e;
  font-size: 14px;
  font-weight: 600;
  letter-spacing: 1px;
  border-radius: 9px;
  transition: all .25s ease;
  user-select: none;
}
.role-tab .tab-icon { font-size: 16px; margin-right: 3px; }
.role-tab:hover { color: #2e7d32; background: rgba(255, 255, 255, .7); }
.role-tab.active {
  color: #fff;
  background: linear-gradient(135deg, #43a047, #2e7d32);
  box-shadow: 0 4px 14px rgba(46, 125, 50, .35);
}

.form-area { padding: 26px 2px 0; }
.role-tip {
  text-align: center;
  font-size: 12px;
  color: #78909c;
  margin-bottom: 20px;
  padding: 8px 12px;
  background: #f4f8f4;
  border: 1px dashed #cfe0cf;
  border-radius: 9px;
}
.role-tip strong { color: #2e7d32; }

.btn-submit {
  width: 100%;
  margin-top: 8px;
  border: none;
  border-radius: 12px;
  background: linear-gradient(135deg, #43a047, #1b5e20);
  color: #fff;
  font-size: 16px;
  letter-spacing: 8px;
  text-indent: 8px;
  font-weight: 700;
  height: 46px;
  box-shadow: 0 8px 20px rgba(46, 125, 50, .35);
  transition: transform .2s ease, box-shadow .2s ease, filter .2s ease;
}
.btn-submit:hover {
  transform: translateY(-2px);
  filter: brightness(1.06);
  box-shadow: 0 12px 26px rgba(46, 125, 50, .45);
}
.btn-submit:active { transform: translateY(0); }

.card-foot { text-align: center; margin-top: 22px; line-height: 1.9; }
.test-tip {
  display: inline-block;
  color: #7a8c7e;
  background: rgba(255, 255, 255, .85);
  border: 1px solid #e0eae0;
  border-radius: 999px;
  padding: 6px 14px;
  font-size: 11.5px;
  box-shadow: 0 2px 8px rgba(27, 60, 30, .05);
}
.consumer-entry { margin-top: 10px; }
.entry-link { color: #2e7d32; text-decoration: none; font-size: 13px; font-weight: 600; }
.entry-link:hover { color: #1b5e20; text-decoration: underline; }
.mx { margin: 0 8px; color: #b0c0b2; }

/* ========== 响应式 ========== */
@media (max-width: 920px) {
  .brand-panel { display: none; }
  .login-panel { flex: 1; padding: 28px 20px; }
  .login-wrap { max-width: 400px; }
  .mobile-brand {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 8px;
    font-size: 20px;
    font-weight: 800;
    color: #1b5e20;
    letter-spacing: 3px;
    margin-bottom: 22px;
  }
  .mobile-logo { font-size: 30px; }
}
</style>
