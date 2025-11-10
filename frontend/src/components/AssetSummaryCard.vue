<template>
  <el-card class="asset-card" shadow="hover">
    <template #header>
      <div class="header">
        <span class="name">{{ asset.name }}</span>
        <el-tag size="small" type="success">{{ asset.status }}</el-tag>
      </div>
    </template>
    <el-descriptions :column="2" size="small" border>
      <el-descriptions-item label="类别">{{ asset.category }}</el-descriptions-item>
      <el-descriptions-item label="启用日期">{{ asset.enabledDate }}</el-descriptions-item>
      <el-descriptions-item label="总投入">¥ {{ formatNumber(asset.totalInvest) }}</el-descriptions-item>
      <el-descriptions-item label="日均成本">¥ {{ formatNumber(asset.avgCostPerDay) }}</el-descriptions-item>
      <el-descriptions-item label="累计折旧">¥ {{ formatNumber(asset.accumulatedDepreciation) }}</el-descriptions-item>
      <el-descriptions-item label="账面价值">¥ {{ formatNumber(asset.bookValue) }}</el-descriptions-item>
    </el-descriptions>
  </el-card>
</template>

<script setup lang="ts">
import type { AssetSummary } from '@/types'

defineProps<{ asset: AssetSummary }>()

const formatNumber = (value: number | string) => {
  const num = Number(value)
  if (Number.isNaN(num)) return value
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}
</script>

<style scoped>
.asset-card {
  border-radius: 12px;
}

.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.name {
  font-weight: 600;
  font-size: 1.1rem;
}
</style>
