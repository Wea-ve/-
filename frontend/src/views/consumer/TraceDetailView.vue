<template>
  <div class="trace-page">
    <!-- 顶部导航条 -->
    <header class="t-nav">
      <div class="t-brand">🌾 安溯·知源</div>
      <div class="t-links">
        <router-link to="/complaint/new">我要投诉</router-link>
        <router-link to="/complaint/query">投诉查询</router-link>
        <router-link to="/login">管理入口</router-link>
      </div>
    </header>

    <div class="t-body" v-loading="loading">
      <!-- 加载失败 -->
      <el-card v-if="error" shadow="never" class="t-card">
        <el-result icon="warning" title="溯源码无效或不存在" :sub-title="error">
          <template #extra>
            <el-button type="primary" @click="reload">重新加载</el-button>
          </template>
        </el-result>
      </el-card>

      <template v-else-if="d.batch">
        <!-- 成功提交投诉提示 -->
        <el-alert v-if="justComplained" type="success" :closable="false" show-icon class="t-card"
                  :title="`投诉已提交！您的查询码为 ${$route.query.cid}，可用手机号或查询码在「投诉查询」页跟踪进度`" />

        <!-- Hero 卡片 -->
        <div class="trace-hero">
          <div class="hero-row">
            <div>
              <div class="hero-crop">🥬 {{ d.batch.cropName }}</div>
              <div class="hero-sub">
                <el-tag effect="dark" type="info" size="small" style="background: rgba(255,255,255,.2); border: none">
                  {{ d.batch.variety || '普通品种' }}
                </el-tag>
                <span class="hero-code">溯源码：{{ d.batch.traceCode }}</span>
              </div>
            </div>
            <div class="hero-badge" v-if="(d.nodeCount || 0) > 0">
              <div class="badge-num">{{ d.nodeCount }}</div>
              <div class="badge-text">可信流转节点</div>
            </div>
          </div>
        </div>

        <!-- AI 综合可信度评分：扫码先看红黄绿评分卡，再看明细 -->
        <div v-if="d.trustScore" class="trust-card" :style="{ borderLeftColor: d.trustScore.color }">
          <div class="trust-head">
            <div class="trust-ring" :style="{ background: d.trustScore.color }">{{ d.trustScore.score }}</div>
            <div class="trust-head-info">
              <div class="trust-title">
                <span class="trust-level" :style="{ color: d.trustScore.color }">{{ d.trustScore.levelText }}</span>
                <span class="trust-grade">{{ d.trustScore.grade }} 档 · AI 综合可信度</span>
              </div>
              <div class="trust-headline">{{ d.trustScore.headline }}</div>
            </div>
          </div>

          <div class="trust-dims">
            <div v-for="dim in d.trustScore.dimensions" :key="dim.key" class="trust-dim">
              <div class="dim-top">
                <span class="dim-name">{{ dim.name }}</span>
                <span class="dim-score">{{ dim.score }} / {{ dim.max }}</span>
              </div>
              <div class="dim-bar">
                <i :class="dim.status" :style="{ width: Math.round((dim.score / dim.max) * 100) + '%' }"></i>
              </div>
              <div class="dim-detail">{{ dim.detail }}</div>
            </div>
          </div>

          <div v-if="d.trustScore.advice?.length" class="trust-advice">
            💡 {{ d.trustScore.advice[0] }}
          </div>
          <div class="trust-action">
            <el-button type="success" round :loading="healthLoading" @click="runHealthCheck">
              🩺 批次健康体检
            </el-button>
            <span class="trust-action-tip">AI 自动跑完 5 项检查，输出风险等级 · 依据 · 建议</span>
          </div>
          <div class="trust-foot">评分由农事记录完整度 · 检测结论 · 流转闭环 · 用药频次 四项数据聚合，红黄绿三档呈现</div>
        </div>

        <!-- 批次信息 -->
        <el-card shadow="never" class="t-card">
          <template #header>📋 批次档案</template>
          <el-descriptions :column="3" border>
            <el-descriptions-item label="批次号">{{ d.batch.batchNo }}</el-descriptions-item>
            <el-descriptions-item label="产地">{{ d.batch.landLocation || '未记录' }}</el-descriptions-item>
            <el-descriptions-item label="种植农户">{{ d.batch.farmerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="播种日期">{{ fmtDate(d.batch.plantDate) }}</el-descriptions-item>
            <el-descriptions-item label="采收日期">{{ fmtDate(d.batch.actualHarvestDate) }}</el-descriptions-item>
            <el-descriptions-item label="采收重量">
              {{ d.batch.harvestWeight ? d.batch.harvestWeight + ' 斤' : '未采收' }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <!-- 流转链路 -->
        <el-card shadow="never" class="t-card" v-if="d.records?.length">
          <template #header>🔄 全链路流转（{{ d.records.length }} 个节点）</template>
          <el-timeline>
            <el-timeline-item v-for="r in d.records" :key="r.id"
                              :color="traceTypeMap[r.type]?.color" :hollow="false">
              <div class="flow-item">
                <el-tag size="small" effect="dark" :color="traceTypeMap[r.type]?.color" style="border: none">
                  {{ traceTypeMap[r.type]?.text || r.type }}
                </el-tag>
                <b>{{ r.fromUserName || '?' }} → {{ r.toUserName || '市场/消费者' }}</b>
                <span v-if="r.quantity" class="flow-meta">数量 {{ r.quantity }} 斤</span>
                <span v-if="r.location" class="flow-meta">📍 {{ r.location }}</span>
                <span class="flow-time">{{ fmtTime(r.operateTime) }}</span>
                <div v-if="r.remark" class="flow-remark">{{ r.remark }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
        </el-card>

        <!-- 农事记录 -->
        <el-card shadow="never" class="t-card">
          <template #header>📓 农事记录（{{ d.logs?.length || 0 }} 条）</template>
          <el-timeline v-if="d.logs?.length">
            <el-timeline-item v-for="l in d.logs" :key="l.id" :timestamp="fmtDate(l.logDate)"
                              :color="logTypeMap[l.logType]?.color">
              <div class="flow-item">
                <el-tag size="small" effect="plain">{{ logTypeMap[l.logType]?.text || l.logType }}</el-tag>
                <span v-if="l.material" class="flow-meta">{{ l.material }}<template v-if="l.dosage">（{{ l.dosage }}）</template></span>
                <div v-if="l.content" class="flow-remark">{{ l.content }}</div>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无农事记录" :image-size="70" />
        </el-card>

        <!-- 检测报告 -->
        <el-card shadow="never" class="t-card">
          <template #header>🧪 第三方检测报告</template>
          <template v-if="d.reports?.length">
            <div v-for="r in d.reports" :key="r.id" class="report-row">
              <div>
                <div class="report-no">{{ r.reportNo || '检测报告' }}</div>
                <div class="report-meta">{{ r.institution || '检测机构' }} · {{ fmtDate(r.reportDate) }}</div>
              </div>
              <div style="display: flex; align-items: center; gap: 10px">
                <el-tag :type="r.result === '合格' ? 'success' : 'danger'" effect="dark" size="small">
                  {{ r.result === '合格' ? '✓ ' : '✗ ' }}{{ r.result }}
                </el-tag>
                <el-link v-if="r.reportUrl" type="primary" :href="r.reportUrl" target="_blank">查看报告</el-link>
              </div>
            </div>
          </template>
          <el-empty v-else description="该批次暂无检测报告" :image-size="70" />
        </el-card>

        <!-- 投诉入口 -->
        <el-card shadow="never" class="t-card" style="text-align: center">
          <div style="color: #8a9a8e; margin-bottom: 12px">对这批农产品有疑问或问题？</div>
          <el-button type="warning" size="large" round @click="complaintVisible = true">
            📢 对本批次发起投诉
          </el-button>
        </el-card>

        <div class="t-foot">从田间到餐桌 · 每一步都可判断 —— 安溯·知源 VeriTrace</div>
      </template>
    </div>

    <!-- 投诉弹窗 -->
    <el-dialog v-model="complaintVisible" title="投诉反馈" width="480px">
      <el-alert type="info" :closable="false" style="margin-bottom: 14px"
                :title="`投诉对象：${d.batch?.cropName}（${d.batch?.batchNo}）`" />
      <el-form label-width="80px">
        <el-form-item label="投诉人" required><el-input v-model="cForm.complainant" /></el-form-item>
        <el-form-item label="手机号"><el-input v-model="cForm.phone" placeholder="用于查询处理进度" /></el-form-item>
        <el-form-item label="类型">
          <el-select v-model="cForm.type" style="width: 100%">
            <el-option v-for="(v, k) in complaintTypeMap" :key="k" :label="v" :value="k" />
          </el-select>
        </el-form-item>
        <el-form-item label="内容" required>
          <el-input v-model="cForm.content" type="textarea" :rows="4"
                    placeholder="请描述您遇到的问题（变质、缺斤少两、信息不实等）" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="complaintVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submitComplaint">提交投诉</el-button>
      </template>
    </el-dialog>

    <!-- 批次健康体检报告 -->
    <el-dialog v-model="healthVisible" title="🩺 批次健康体检报告" width="560px">
      <div v-if="health" class="health-body">
        <div class="health-head" :style="{ borderLeftColor: health.riskColor }">
          <div class="health-badge" :style="{ background: health.riskColor }">{{ health.riskLevel }}</div>
          <div style="min-width: 0">
            <div class="health-summary">{{ health.summary }}</div>
            <div class="health-meta">
              规则评分 {{ health.score }}/100 · {{ health.engine }} · 已检查
              {{ health.tools?.length || 0 }} 项
            </div>
          </div>
        </div>

        <div class="health-sec">
          <div class="health-sec-title">检查依据</div>
          <ul class="health-list">
            <li v-for="(item, i) in health.evidence" :key="'e' + i">{{ item }}</li>
          </ul>
        </div>

        <div class="health-sec">
          <div class="health-sec-title">给消费者的建议</div>
          <ul class="health-list">
            <li v-for="(item, i) in health.suggestions" :key="'s' + i">{{ item }}</li>
          </ul>
        </div>

        <div class="health-tools">
          <el-tag v-for="(t, i) in health.tools" :key="'t' + i" size="small" effect="plain">
            {{ t.label }}：{{ t.summary }}
          </el-tag>
        </div>
        <div class="health-tip">{{ health.disclaimer }}</div>
      </div>
      <el-empty v-else-if="healthError" :description="healthError" :image-size="70" />
      <div v-else class="health-loading" v-loading="true">AI 正在核对批次、农事、检测、流转与知识库…</div>
    </el-dialog>

    <!-- AI 浮窗 -->
    <AiChat v-if="d.batch" :batch-id="d.batch.id" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http, { type ApiError } from '../../api/http'
import AiChat from '../../components/AiChat.vue'
import { complaintTypeMap, fmtDate, fmtTime, logTypeMap, traceTypeMap } from '../../utils'
import type { ComplaintSubmitResult, HealthCheckReport, TraceDetailData } from '../../types'

const route = useRoute()
const d = ref<Partial<TraceDetailData>>({})
const loading = ref<boolean>(false)
const error = ref<string>('')
const saving = ref<boolean>(false)
const complaintVisible = ref<boolean>(false)
const justComplained = computed<boolean>(() => route.query.complained === '1')
const cForm = reactive({ complainant: '', phone: '', type: 'QUALITY', content: '' })
// 批次健康体检
const healthVisible = ref<boolean>(false)
const healthLoading = ref<boolean>(false)
const health = ref<HealthCheckReport | null>(null)
const healthError = ref<string>('')

/** 一键体检：Agent 自动跑完 5 个工具，输出风险等级 + 依据 + 建议 */
async function runHealthCheck(): Promise<void> {
  healthVisible.value = true
  health.value = null
  healthError.value = ''
  healthLoading.value = true
  try {
    health.value = await http.get<HealthCheckReport>(`/api/ai/health/${d.value.batch!.id}`)
  } catch (e) {
    healthError.value = (e as ApiError)?.msg || '体检生成失败，请稍后再试'
  } finally { healthLoading.value = false }
}

async function load(): Promise<void> {
  loading.value = true
  try {
    d.value = await http.get<TraceDetailData>(`/api/trace/${route.params.code}`)
  } catch (e) {
    error.value = (e as ApiError)?.msg || '请核对二维码是否正确'
  } finally { loading.value = false }
}
const reload = (): void => { error.value = ''; load() }

async function submitComplaint(): Promise<void> {
  if (!cForm.complainant.trim()) {
    ElMessage.warning('请填写投诉人')
    return
  }
  if (!cForm.content.trim()) {
    ElMessage.warning('请填写投诉内容')
    return
  }
  saving.value = true
  try {
    const data = await http.post<ComplaintSubmitResult>('/api/complaint', {
      ...cForm,
      batchId: d.value.batch!.id
    })
    complaintVisible.value = false
    ElMessage.success('投诉已提交')
    // 回到查询页展示查询码
    const kw = cForm.phone || data.id
    location.href = `/app/complaint/query?keyword=${kw}&submitted=1&cid=${data.id}`
  } catch { /* 已提示 */ } finally { saving.value = false }
}

onMounted(load)
</script>

<style scoped>
.trace-page { min-height: 100vh; background: #f4f7f4; }
.t-nav {
  display: flex; align-items: center; justify-content: space-between;
  padding: 12px 20px; background: #fff;
  box-shadow: 0 1px 8px rgba(31, 66, 39, .08);
  position: sticky; top: 0; z-index: 100;
}
.t-brand { font-weight: 700; color: #1b5e20; font-size: 17px; letter-spacing: 1px; }
.t-links { display: flex; gap: 18px; }
.t-links a { color: #4a674e; text-decoration: none; font-size: 14px; }
.t-links a:hover { color: #2e7d32; }
.t-body { max-width: 860px; margin: 0 auto; padding: 20px 14px 60px; }
.t-card { margin-bottom: 16px; }
/* AI 综合可信度评分卡 */
.trust-card {
  background: #fff; border-left: 6px solid #2e7d32; border-radius: 14px;
  padding: 18px 20px; margin-bottom: 16px;
  box-shadow: 0 4px 18px rgba(31, 66, 39, .08);
}
.trust-head { display: flex; align-items: center; gap: 16px; }
.trust-ring {
  width: 78px; height: 78px; flex: 0 0 auto; border-radius: 50%; color: #fff;
  display: flex; align-items: center; justify-content: center;
  font-size: 30px; font-weight: 800; letter-spacing: -1px;
  box-shadow: 0 6px 16px rgba(0, 0, 0, .14);
}
.trust-head-info { flex: 1; min-width: 0; }
.trust-title { display: flex; align-items: baseline; gap: 10px; flex-wrap: wrap; }
.trust-level { font-size: 21px; font-weight: 800; letter-spacing: 1px; }
.trust-grade { font-size: 12.5px; color: #8a9a8e; }
.trust-headline { margin-top: 4px; color: #4a674e; font-size: 13.5px; line-height: 1.6; }
.trust-dims {
  display: grid; grid-template-columns: 1fr 1fr; gap: 12px 22px; margin-top: 18px;
}
.trust-dim { min-width: 0; }
.dim-top { display: flex; justify-content: space-between; font-size: 13px; color: #3d5a43; }
.dim-name { font-weight: 600; }
.dim-score { color: #8a9a8e; font-family: monospace; }
.dim-bar { height: 7px; background: #eef3ee; border-radius: 4px; overflow: hidden; margin: 5px 0 4px; }
.dim-bar i { display: block; height: 100%; border-radius: 4px; background: #2e7d32; transition: width .4s; }
.dim-bar i.warn { background: #f9a825; }
.dim-bar i.bad { background: #d32f2f; }
.dim-detail { font-size: 11.5px; color: #9aa89c; line-height: 1.5; }
.trust-advice {
  margin-top: 14px; padding: 9px 12px; border-radius: 8px;
  background: #f4f9f4; border: 1px dashed #cfe6cf; color: #2e7d32; font-size: 13px;
}
.trust-action { display: flex; align-items: center; gap: 12px; margin-top: 14px; flex-wrap: wrap; }
.trust-action-tip { color: #9aa89c; font-size: 12px; }
.trust-foot { margin-top: 10px; color: #a5b2a7; font-size: 11.5px; }
/* 体检报告弹窗 */
.health-head {
  display: flex; align-items: center; gap: 14px; padding: 12px 14px;
  background: #fafdfa; border-left: 5px solid #2e7d32; border-radius: 10px;
}
.health-badge {
  flex: 0 0 auto; color: #fff; font-weight: 800; font-size: 14px; letter-spacing: 1px;
  padding: 8px 14px; border-radius: 999px;
}
.health-summary { color: #2c4633; font-size: 14px; font-weight: 700; line-height: 1.5; }
.health-meta { color: #8a9a8e; font-size: 12px; margin-top: 4px; }
.health-sec { margin-top: 16px; }
.health-sec-title { font-size: 13.5px; font-weight: 700; color: #2e7d32; margin-bottom: 6px; }
.health-list { margin: 0; padding-left: 18px; color: #4a5f50; font-size: 13px; line-height: 1.8; }
.health-tools { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 16px; }
.health-tip { margin-top: 12px; color: #9aa89c; font-size: 11.5px; line-height: 1.6; }
.health-loading { height: 130px; display: flex; align-items: center; justify-content: center; color: #8a9a8e; font-size: 13px; }
.hero-row { display: flex; justify-content: space-between; align-items: center; flex-wrap: wrap; gap: 12px; }
.hero-crop { font-size: 30px; font-weight: 800; letter-spacing: 2px; }
.hero-sub { margin-top: 10px; display: flex; align-items: center; gap: 10px; }
.hero-code { color: rgba(255, 255, 255, .85); font-size: 14px; font-family: monospace; }
.hero-badge {
  text-align: center; background: rgba(255, 255, 255, .16);
  border: 1px solid rgba(255, 255, 255, .4); border-radius: 14px; padding: 10px 18px;
}
.badge-num { font-size: 26px; font-weight: 800; }
.badge-text { font-size: 12px; color: rgba(255, 255, 255, .85); }
.flow-item { line-height: 1.9; }
.flow-meta { margin-left: 10px; color: #666; font-size: 13px; }
.flow-time { margin-left: 10px; color: #999; font-size: 12.5px; }
.flow-remark { color: #777; font-size: 13px; margin-top: 2px; }
.report-row {
  display: flex; justify-content: space-between; align-items: center;
  padding: 12px 4px; border-bottom: 1px dashed #e4efe5;
}
.report-no { font-weight: 600; color: #23422a; }
.report-meta { font-size: 12.5px; color: #8a9a8e; margin-top: 2px; }
.t-foot { text-align: center; color: #9aab9d; font-size: 12px; letter-spacing: 2px; margin-top: 10px; }
@media (max-width: 768px) {
  .hero-crop { font-size: 24px; }
  .t-links { gap: 12px; }
  .trust-card { padding: 16px; }
  .trust-ring { width: 64px; height: 64px; font-size: 24px; }
  .trust-level { font-size: 18px; }
  .trust-dims { grid-template-columns: 1fr; }
}
</style>
