<template>
  <div class="ai-chat">
    <!-- 浮窗按钮 -->
    <div v-if="!open" class="ai-fab" @click="open = true">
      <span class="ai-fab-icon">🤖</span>
      <span class="ai-fab-text">AI 问答</span>
    </div>

    <!-- 问答面板 -->
    <transition name="ai-pop">
      <div v-if="open" class="ai-panel">
        <div class="ai-head">
          <span>🌾 溯源智能问答</span>
          <div class="ai-head-btns">
            <button class="ai-head-btn" title="清空对话" @click="clearChat" v-if="messages.length">🗑️</button>
            <button class="ai-head-btn" title="关闭" @click="open = false">✕</button>
          </div>
        </div>
        <div class="ai-body" ref="bodyEl">
          <div class="ai-msg bot">您好！我是本批次的 AI 溯源助手，可以问我：农残合格吗？打过几次农药？怎么保存？也可以拍照上传（农药包装、蔬菜状态）让我识别解答～</div>
          <div v-for="(m, i) in messages" :key="i" class="ai-msg" :class="m.role === 'user' ? 'user' : 'bot'">
            <div class="ai-bubble">
              <img v-if="m.image" :src="m.image" class="ai-msg-img" @click="preview(m.image)" />
              <div>{{ m.text }}</div>
              <!-- 本轮调用的工具徽章 -->
              <div v-if="m.tools && m.tools.length" class="ai-tools">
                <span v-for="(t, ti) in m.tools" :key="ti" class="ai-tool-chip" :title="t.summary">
                  🔧 {{ t.label }}
                </span>
              </div>
            </div>
          </div>
          <div v-if="loading" class="ai-msg bot"><div class="ai-bubble typing">{{ imagePending ? '正在识别图片…' : '正在思考…' }}</div></div>
        </div>
        <!-- 待发送图片预览 -->
        <div v-if="imagePending" class="ai-img-preview">
          <img :src="imagePending" />
          <el-button text circle size="small" class="ai-img-remove" @click="imagePending = ''">✕</el-button>
        </div>
        <div class="ai-input">
          <input ref="fileEl" type="file" accept="image/*" class="ai-file" @change="onPickImage" />
          <button class="ai-cam" title="上传照片提问" @click="fileEl && fileEl.click()" :disabled="loading">📷</button>
          <el-input v-model="input" placeholder="输入问题，回车发送" :disabled="loading"
                    @keyup.enter="send" clearable />
          <el-button type="primary" :loading="loading" @click="send">发送</el-button>
        </div>
      </div>
    </transition>

    <!-- 图片放大预览 -->
    <div v-if="previewUrl" class="ai-preview-mask" @click="previewUrl = ''">
      <img :src="previewUrl" />
    </div>
  </div>
</template>

<script setup>
import { nextTick, ref } from 'vue'
import http from '../api/http'

const props = defineProps({ batchId: { type: Number, required: true } })

const open = ref(false)
const input = ref('')
const loading = ref(false)
const messages = ref([])
const bodyEl = ref(null)
const fileEl = ref(null)
const imagePending = ref('')   // 待发送图片 data URL
const previewUrl = ref('')     // 放大预览

// 会话 ID：页面内保持多轮上下文
let sessionId = `s-${Date.now()}-${Math.floor(Math.random() * 100000)}`

/** 清空对话：重置消息列表并换新会话，后端上下文随之作废 */
function clearChat() {
  messages.value = []
  input.value = ''
  imagePending.value = ''
  sessionId = `s-${Date.now()}-${Math.floor(Math.random() * 100000)}`
}

const scrollBottom = () => nextTick(() => {
  if (bodyEl.value) bodyEl.value.scrollTop = bodyEl.value.scrollHeight
})

/** 选择图片：canvas 压缩到最长边 1024px、JPEG 0.8，控制请求体积 */
function onPickImage(e) {
  const file = e.target.files && e.target.files[0]
  e.target.value = ''
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    const img = new Image()
    img.onload = () => {
      const max = 1024
      let { width, height } = img
      if (width > max || height > max) {
        const scale = max / Math.max(width, height)
        width = Math.round(width * scale)
        height = Math.round(height * scale)
      }
      const canvas = document.createElement('canvas')
      canvas.width = width
      canvas.height = height
      canvas.getContext('2d').drawImage(img, 0, 0, width, height)
      imagePending.value = canvas.toDataURL('image/jpeg', 0.8)
    }
    img.src = reader.result
  }
  reader.readAsDataURL(file)
}

async function send() {
  const q = input.value.trim()
  const img = imagePending.value
  if ((!q && !img) || loading.value) return
  messages.value.push({ role: 'user', text: q || '请帮我看看这张照片', image: img })
  input.value = ''
  imagePending.value = ''
  loading.value = true
  scrollBottom()
  try {
    const data = await http.post('/api/ai/chat',
      { batchId: props.batchId, question: q || '请分析这张照片', sessionId, image: img },
      { timeout: 90000 })
    messages.value.push({
      role: 'bot',
      text: data?.reply || 'AI 暂时无法回答',
      tools: data?.tools || []
    })
  } catch {
    messages.value.push({ role: 'bot', text: 'AI 服务暂时不可用，请稍后再试', tools: [] })
  } finally {
    loading.value = false
    scrollBottom()
  }
}

function preview(url) { previewUrl.value = url }
</script>

<style scoped>
.ai-chat { position: fixed; right: 18px; bottom: 18px; z-index: 2000; }
.ai-fab {
  display: flex; align-items: center; gap: 6px;
  background: linear-gradient(135deg, #43a047, #1b5e20);
  color: #fff; padding: 10px 18px; border-radius: 28px;
  cursor: pointer; box-shadow: 0 6px 18px rgba(27, 94, 32, .4);
  transition: transform .2s;
}
.ai-fab:hover { transform: translateY(-2px); }
.ai-fab-icon { font-size: 18px; }
.ai-panel {
  width: 340px; max-width: calc(100vw - 36px); height: 480px;
  background: #fff; border-radius: 14px; overflow: hidden;
  box-shadow: 0 12px 40px rgba(0, 0, 0, .18);
  display: flex; flex-direction: column;
}
.ai-head {
  display: flex; justify-content: space-between; align-items: center;
  padding: 10px 14px; color: #fff; font-weight: 600;
  background: linear-gradient(135deg, #43a047, #1b5e20);
}
.ai-head-btns { display: flex; align-items: center; gap: 2px; }
.ai-head-btn {
  border: none; background: transparent; color: #fff; cursor: pointer;
  font-size: 13px; padding: 2px 6px; border-radius: 6px; opacity: .85;
}
.ai-head-btn:hover { background: rgba(255,255,255,.18); opacity: 1; }
.ai-body { flex: 1; overflow-y: auto; padding: 12px; background: #f6f9f6; }
.ai-msg { display: flex; margin-bottom: 10px; }
.ai-msg.user { justify-content: flex-end; }
.ai-bubble {
  max-width: 80%; padding: 8px 12px; border-radius: 10px;
  font-size: 13px; line-height: 1.6; white-space: pre-wrap; word-break: break-word;
}
.ai-msg.bot .ai-bubble { background: #fff; color: #333; border: 1px solid #e3eee4; }
.ai-msg.user .ai-bubble { background: #2e7d32; color: #fff; }
.typing { color: #999; }
.ai-msg-img { width: 140px; border-radius: 8px; margin-bottom: 6px; display: block; cursor: zoom-in; }
.ai-tools { margin-top: 8px; display: flex; flex-wrap: wrap; gap: 4px; }
.ai-tool-chip {
  font-size: 11px; color: #1b5e20; background: #e8f5e9;
  border: 1px solid #c8e6c9; border-radius: 10px; padding: 1px 8px;
}
.ai-img-preview {
  position: relative; margin: 8px 12px 0;
  border: 1px solid #e3eee4; border-radius: 8px; width: 72px; height: 72px; overflow: hidden;
}
.ai-img-preview img { width: 100%; height: 100%; object-fit: cover; }
.ai-img-remove {
  position: absolute; top: 0; right: 0; background: rgba(0,0,0,.45) !important; color: #fff;
}
.ai-input { display: flex; gap: 8px; padding: 10px; border-top: 1px solid #eee; }
.ai-file { display: none; }
.ai-cam {
  border: 1px solid #dcdfe6; background: #fff; border-radius: 6px;
  font-size: 16px; cursor: pointer; padding: 0 10px;
}
.ai-cam:disabled { opacity: .5; cursor: not-allowed; }
.ai-preview-mask {
  position: fixed; inset: 0; z-index: 3000; background: rgba(0,0,0,.7);
  display: flex; align-items: center; justify-content: center; cursor: zoom-out;
}
.ai-preview-mask img { max-width: 90vw; max-height: 90vh; border-radius: 8px; }
.ai-pop-enter-active, .ai-pop-leave-active { transition: all .22s ease; }
.ai-pop-enter-from, .ai-pop-leave-to { opacity: 0; transform: translateY(12px) scale(.96); }
</style>
