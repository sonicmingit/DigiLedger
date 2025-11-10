<template>
  <el-card class="asset-card" shadow="hover">
    <template #header>
      <div class="header">
        <div class="title">
          <span class="name">{{ asset.name }}</span>
          <span v-if="asset.model" class="model">{{ asset.model }}</span>
        </div>
        <el-tag size="small" :type="statusTagType(asset.status)">
          {{ statusLabel(asset.status) }}
        </el-tag>
      </div>
    </template>
    <el-descriptions :column="2" size="small" border>
      <el-descriptions-item label="类别">{{ asset.category }}</el-descriptions-item>
      <el-descriptions-item label="品牌" v-if="asset.brand">{{ asset.brand }}</el-descriptions-item>
      <el-descriptions-item label="启用日期">{{ asset.enabledDate }}</el-descriptions-item>
      <el-descriptions-item label="日均成本">¥ {{ formatNumber(asset.avgCostPerDay) }}</el-descriptions-item>
      <el-descriptions-item label="累计折旧">¥ {{ formatNumber(asset.accumulatedDepreciation) }}</el-descriptions-item>
      <el-descriptions-item label="账面价值">¥ {{ formatNumber(asset.bookValue) }}</el-descriptions-item>
      <el-descriptions-item label="总投入">¥ {{ formatNumber(asset.totalInvest) }}</el-descriptions-item>
      <el-descriptions-item label="年折旧率">{{ formatPercent(asset.annualRate) }}</el-descriptions-item>
    </el-descriptions>
    <div v-if="asset.tags?.length" class="tags">
      <el-tag v-for="tag in asset.tags" :key="tag" size="small" class="tag">{{ tag }}</el-tag>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import type { AssetStatus, AssetSummary } from '@/types'

// 接收父组件传入的资产概要数据
defineProps<{ asset: AssetSummary }>()

// 将数字格式化为中文地区友好的金额展示
const formatNumber = (value: number | string) => {
  const num = Number(value)
  if (Number.isNaN(num)) return value
  return num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

// 百分比格式化，兼容空值
const formatPercent = (value: number | string | undefined) => {
  if (value === undefined) return '--'
  const num = Number(value)
  if (Number.isNaN(num)) return value
  return `${(num * 100).toFixed(1)}%`
}

// 状态中文含义映射
const statusLabel = (status: AssetStatus) => {
  switch (status) {
    case 'IN_USE':
      return '使用中'
    case 'IDLE':
      return '闲置'
    case 'FOR_SALE':
      return '待售'
    case 'SOLD':
      return '已售出'
    case 'RETIRED':
      return '已退役'
    default:
      return status
  }
}

// 不同状态对应的标签颜色
const statusTagType = (status: AssetStatus) => {
  switch (status) {
    case 'IN_USE':
      return 'success'
    case 'IDLE':
      return 'info'
    case 'FOR_SALE':
      return 'warning'
    case 'SOLD':
      return 'danger'
    case 'RETIRED':
      return 'default'
    default:
      return 'info'
  }
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

.title {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.model {
  font-size: 0.85rem;
  color: #6b7280;
}

.tags {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.tag {
  border-radius: 10px;
}
</style>
