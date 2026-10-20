<template>
  <div ref="el" :style="{ height, width: '100%' }"></div>
</template>

<script setup>
import * as echarts from 'echarts'
import { onMounted, onBeforeUnmount, ref, watch } from 'vue'

const props = defineProps({
  option: { type: Object, default: null },
  height: { type: String, default: '300px' }
})

const el = ref(null)
let chart = null

const render = () => {
  if (chart && props.option) chart.setOption(props.option, true)
}

const resize = () => chart && chart.resize()

onMounted(() => {
  chart = echarts.init(el.value)
  render()
  window.addEventListener('resize', resize)
})

watch(() => props.option, render, { deep: true })

onBeforeUnmount(() => {
  window.removeEventListener('resize', resize)
  if (chart) chart.dispose()
})
</script>
