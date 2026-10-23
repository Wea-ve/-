<template>
  <div class="trace-page">
    <header class="t-nav">
      <div class="t-brand">🌾 农芯溯源</div>
      <div class="t-links">
        <router-link to="/complaint/new">我要投诉</router-link>
        <router-link to="/login">管理入口</router-link>
      </div>
    </header>

    <div class="t-body">
      <el-card shadow="never" class="t-card">
        <template #header>🔍 投诉进度查询</template>
        <div style="display: flex; gap: 10px; flex-wrap: wrap">
          <el-input v-model="keyword" placeholder="输入手机号或查询码" clearable
                    style="max-width: 320px" @keyup.enter="load">
            <template #prefix>🔑</template>
          </el-input>
          <el-button type="primary" :loading="loading" @click="load">查询</el-button>
        </div>
      </el-card>

      <el-alert v-if="justSubmitted" type="success" :closable="false" show-icon class="t-card"
                :title="`投诉提交成功！查询码 ${keyword} ，请截图保存，后续凭此查询进度`" />

      <el-empty v-if="!loading && searched && !list.length"
                description="没有查到相关投诉，请确认手机号或查询码是否正确" />

      <el-card v-for="c in list" :key="c.id" shadow="never" class="t-card">
        <div class="c-head">
          <div>
            <span class="c-no">查询码 {{ c.queryCode || '—' }}</span>
            <el-tag :type="complaintStatusMap[c.status]?.type" size="small" style="margin-left: 10px">
              {{ complaintStatusMap[c.status]?.text }}
            </el-tag>
          </div>
          <span class="c-time">{{ fmtTime(c.createTime) }}</span>
        </div>

        <el-descriptions :column="2" size="small" border style="margin-top: 12px">
          <el-descriptions-item label="类型">{{ complaintTypeMap[c.type || ''] || c.type || '-' }}</el-descriptions-item>
          <el-descriptions-item label="关联批次">
            {{ c.batchNo ? `${c.cropName}（${c.batchNo}）` : '未关联' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="c-block">
          <div class="c-label">您的投诉内容</div>
          <div class="c-content">{{ c.content }}</div>
        </div>

        <div v-if="c.reply" class="c-block admin">
          <div class="c-label">👨‍💼 管理员回复（{{ fmtTime(c.replyTime) }}）</div>
          <div class="c-content">{{ c.reply }}</div>
        </div>

        <div v-if="c.consumerReply" class="c-block consumer">
          <div class="c-label">🙋 您的确认反馈（{{ fmtTime(c.consumerReplyTime) }}）· 工单已办结</div>
          <div class="c-content">{{ c.consumerReply }}</div>
        </div>

        <!-- 处理中：消费者确认 -->
        <template v-if="c.status === 'PROCESSING'">
          <el-alert type="info" :closable="false" style="margin-top: 12px"
                    title="管理员已回复，如问题已解决，请填写查询码并确认反馈以关闭工单" />
          <div style="display: flex; gap: 10px; margin-top: 12px; flex-wrap: wrap">
            <el-input v-model="confirmCode" placeholder="查询码" style="max-width: 160px" />
            <el-input v-model="confirmText" placeholder="如：问题已解决，满意" size="default" style="max-width: 300px" />
            <el-button type="success" :loading="saving" @click="confirm(c)">确认并关闭工单</el-button>
          </div>
        </template>

        <!-- 待处理提示 -->
        <el-alert v-if="c.status === 'PENDING'" type="warning" :closable="false" style="margin-top: 12px"
                  title="工作人员会尽快处理，请耐心等待" />
      </el-card>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../../api/http'
import { complaintStatusMap, complaintTypeMap, fmtTime } from '../../utils'
import type { Complaint } from '../../types'

const route = useRoute()
const keyword = ref<string>(route.query.keyword ? String(route.query.keyword) : '')
const justSubmitted: boolean = route.query.submitted === '1'
const list = ref<Complaint[]>([])
// 模板原有引用，保持默认 false，与原行为一致
const searched = ref<boolean>(false)
const loading = ref<boolean>(false)
const saving = ref<boolean>(false)
const confirmText = ref<string>('')
const confirmCode = ref<string>('')

async function load(): Promise<void> {
  const kw = keyword.value.trim()
  if (!kw) {
    ElMessage.warning('请输入手机号或查询码')
    return
  }
  loading.value = true
  try {
    list.value = await http.get<Complaint[]>('/api/complaint/query', { params: { keyword: kw } })
  } finally { loading.value = false }
}

async function confirm(c: Complaint): Promise<void> {
  if (!confirmCode.value.trim()) {
    ElMessage.warning('请填写查询码')
    return
  }
  if (!confirmText.value.trim()) {
    ElMessage.warning('请填写确认内容')
    return
  }
  saving.value = true
  try {
    await http.post(`/api/complaint/${c.id}/confirm`, {
      consumerReply: confirmText.value,
      queryCode: confirmCode.value.trim().toUpperCase()
    })
    ElMessage.success('感谢反馈，工单已关闭')
    load()
  } catch { /* 已提示 */ } finally { saving.value = false }
}

onMounted(() => { if (keyword.value) load() })
</script>

<style scoped>
.trace-page { min-height: 100vh; background: #f4f7f4; }
.t-nav {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; background: #fff; box-shadow: 0 1px 8px rgba(31, 66, 39, .08);
}
.t-brand { font-weight: 700; color: #1b5e20; font-size: 17px; }
.t-links { display: flex; gap: 18px; }
.t-links a { color: #4a674e; text-decoration: none; font-size: 14px; }
.t-body { max-width: 720px; margin: 0 auto; padding: 24px 14px 60px; }
.t-card { margin-bottom: 16px; }
.c-head { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 8px; }
.c-no { font-weight: 700; color: #23422a; }
.c-time { font-size: 12.5px; color: #999; }
.c-block { border-radius: 10px; padding: 12px 14px; margin-top: 12px; background: #f7f9f7; }
.c-block.admin { background: #f2f8f2; border: 1px solid #dcecdc; }
.c-block.consumer { background: #eef5fd; border: 1px solid #d3e5f7; }
.c-label { font-size: 12.5px; color: #7d8f81; margin-bottom: 6px; }
.c-content { font-size: 14px; color: #333; line-height: 1.6; white-space: pre-wrap; }
</style>
