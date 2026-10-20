<template>
  <div class="page">
    <div class="page-head"><div class="page-title">经手批次</div></div>

    <el-card shadow="never">
      <div class="toolbar">
        <el-input v-model="keyword" placeholder="搜索批次号 / 作物名" clearable style="width: 240px"
                  @keyup.enter="load" @clear="load">
          <template #prefix><el-icon><Search /></el-icon></template>
        </el-input>
        <el-button @click="load">查询</el-button>
      </div>

      <el-table :data="list" v-loading="loading">
        <el-table-column prop="batchNo" label="批次号" min-width="170" />
        <el-table-column prop="cropName" label="作物" width="90" />
        <el-table-column prop="variety" label="品种" width="100" />
        <el-table-column prop="farmerName" label="农户" width="100" />
        <el-table-column prop="landLocation" label="产地" min-width="120" show-overflow-tooltip />
        <el-table-column label="状态" width="90">
          <template #default="{ row }">
            <el-tag :type="batchStatusMap[row.status]?.type" size="small">{{ batchStatusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="溯源码" min-width="180">
          <template #default="{ row }">
            <span v-if="row.traceCode" style="font-family: monospace">{{ row.traceCode }}</span>
            <span v-else style="color: #999">-</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import http from '../../api/http'
import { batchStatusMap } from '../../utils'

const list = ref([])
const keyword = ref('')
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    list.value = await http.get('/api/dealer/records', { params: { keyword: keyword.value || undefined } })
  } finally { loading.value = false }
}

onMounted(load)
</script>
