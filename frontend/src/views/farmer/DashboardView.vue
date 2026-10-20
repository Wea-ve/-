<template>
  <div class="page">
    <div class="page-head">
      <div class="page-title">农户工作台</div>
    </div>

    <div class="stat-grid">
      <StatCard :value="d.totalBatches" label="总批次" icon="🌾" />
      <StatCard :value="d.plantingCount" label="种植中" icon="🌱" color="linear-gradient(135deg, #ffb300, #fb8c00)" />
      <StatCard :value="d.harvestedCount" label="已采收" icon="✅" color="linear-gradient(135deg, #1e88e5, #1565c0)" />
      <StatCard :value="d.reportCount" label="检测报告" icon="📋" color="linear-gradient(135deg, #8e24aa, #6a1b9a)" />
    </div>

    <el-row :gutter="16" style="margin-top: 16px">
      <el-col :xs="24" :md="14">
        <el-card shadow="never">
          <template #header>📦 最近批次</template>
          <el-table :data="d.recentBatches" size="large">
            <el-table-column prop="batchNo" label="批次号" min-width="160" />
            <el-table-column prop="cropName" label="作物" width="90" />
            <el-table-column label="状态" width="90">
              <template #default="{ row }">
                <el-tag :type="batchStatusMap[row.status]?.type" size="small">{{ batchStatusMap[row.status]?.text }}</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="播种日期" width="110">
              <template #default="{ row }">{{ fmtDate(row.plantDate) }}</template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="10">
        <el-card shadow="never">
          <template #header>🥬 各作物采收重量（斤）</template>
          <ChartBox v-if="pieOption" :option="pieOption" height="300px" />
          <el-empty v-else description="暂无采收数据" :image-size="80" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import http from '../../api/http'
import StatCard from '../../components/StatCard.vue'
import ChartBox from '../../components/ChartBox.vue'
import { batchStatusMap, fmtDate } from '../../utils'

const d = ref({ recentBatches: [] })

const pieOption = computed(() => {
  const rows = d.value.cropStats || []
  if (!rows.length) return null
  return {
    tooltip: { trigger: 'item', formatter: '{b}: {c} 斤 ({d}%)' },
    legend: { bottom: 0 },
    series: [{
      type: 'pie', radius: ['40%', '68%'], center: ['50%', '45%'],
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: { show: false },
      data: rows.map(r => ({ name: r.name, value: r.value }))
    }]
  }
})

onMounted(async () => {
  d.value = await http.get('/api/farmer/dashboard')
})
</script>

<style scoped>
.stat-grid {
  display: grid; grid-template-columns: repeat(4, 1fr); gap: 14px;
}
@media (max-width: 768px) {
  .stat-grid { grid-template-columns: repeat(2, 1fr); }
}
</style>
