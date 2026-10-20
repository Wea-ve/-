<template>
  <div class="stat-card">
    <div class="stat-icon" :style="{ background: color }">{{ icon }}</div>
    <div>
      <div class="stat-value">{{ value }}</div>
      <div class="stat-label">{{ label }}</div>
    </div>
  </div>
</template>

<script setup>
defineProps({
  value: { type: [Number, String], default: 0 },
  label: String,
  icon: { type: String, default: '🌱' },
  color: { type: String, default: 'linear-gradient(135deg, #43a047, #2e7d32)' }
})
</script>
