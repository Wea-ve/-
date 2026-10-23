<template>
  <div class="trace-page">
    <!-- 顶部导航条 -->
    <header class="t-nav">
      <div class="t-brand">🌾 农芯溯源</div>
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

        <div class="t-foot">从田间到餐桌 · 每一步都可信 —— 农芯溯源平台提供</div>
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
import type { ComplaintSubmitResult, TraceDetailData } from '../../types'

const route = useRoute()
const d = ref<Partial<TraceDetailData>>({})
const loading = ref<boolean>(false)
const error = ref<string>('')
const saving = ref<boolean>(false)
const complaintVisible = ref<boolean>(false)
const justComplained = computed<boolean>(() => route.query.complained === '1')
const cForm = reactive({ complainant: '', phone: '', type: 'QUALITY', content: '' })

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
}
</style>
