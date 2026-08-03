<template>
  <div class="asset-card" @click="emit('select', asset)">
    <div class="card-thumb">
      <img v-if="buildOssUrl(asset.coverImageUrl)" :src="buildOssUrl(asset.coverImageUrl)" :alt="asset.name" loading="lazy" />
      <div v-else class="placeholder">
        <i class="mdi mdi-cube-outline"></i>
      </div>
    </div>
    
    <div class="card-content">
      <div class="card-header">
        <span class="brand" v-if="asset.brandName">{{ asset.brandName }}</span>
        <h3 class="name">{{ asset.name }}</h3>
      </div>
      
      <div class="card-body">
        <span class="price">￥{{ formatPrice(asset.totalInvest) }}</span>
      </div>
      
      <div class="card-footer">
        <span class="usage-days" :class="{ 'text-neon': asset.status === '使用中' }">
          <i class="mdi mdi-clock-outline"></i>
          {{ asset.useDays }}天
        </span>
        <span class="status-badge" :class="statusClass">{{ statusLabel }}</span>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AssetSummary } from '@/types'
import { buildOssUrl } from '@/utils/storage'

const props = defineProps<{ asset: AssetSummary }>()
const emit = defineEmits<{ (e: 'select', asset: AssetSummary): void }>()

const formatPrice = (val?: number) => (val || 0).toFixed(2)

const statusInfo = computed(() => {
  switch (props.asset.status) {
    case '使用中': return { label: '使用中', class: 'status-active' }
    case '已闲置': return { label: '闲置', class: 'status-idle' }
    case '已出售': return { label: '已售', class: 'status-sold' }
    default: return { label: props.asset.status, class: 'status-default' }
  }
})

const statusLabel = computed(() => statusInfo.value.label)
const statusClass = computed(() => statusInfo.value.class)
</script>

<style scoped>
.asset-card {
  display: flex;
  background: var(--dl-bg-surface-light);
  border-radius: var(--dl-radius-lg);
  padding: 12px;
  gap: 16px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  transition: transform 0.2s;
}

.asset-card:active {
  transform: scale(0.98);
  background: rgba(255, 255, 255, 0.08);
}

.card-thumb {
  width: 80px;
  height: 80px;
  border-radius: var(--dl-radius-md);
  overflow: hidden;
  flex-shrink: 0;
  background: var(--dl-bg-base);
}

.card-thumb img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: var(--dl-text-muted);
}

.card-content {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.card-header {
  display: flex;
  flex-direction: column;
  gap: 2px;
}

.brand {
  font-size: 10px;
  color: var(--dl-text-muted);
  text-transform: uppercase;
  letter-spacing: 0.5px;
}

.name {
  font-size: 15px;
  font-weight: 600;
  color: var(--dl-text-primary);
  margin: 0;
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.price {
  font-size: 16px;
  font-family: var(--dl-font-mono);
  color: var(--dl-primary);
  font-weight: 700;
  text-shadow: 0 0 5px var(--dl-primary-dim);
}

.card-footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
}

.usage-days {
  color: var(--dl-text-muted);
  display: flex;
  align-items: center;
  gap: 4px;
}

.status-badge {
  padding: 2px 8px;
  border-radius: 4px;
  font-size: 10px;
  font-weight: 600;
}

.status-active {
  background: rgba(0, 255, 157, 0.1);
  color: var(--dl-success);
  border: 1px solid rgba(0, 255, 157, 0.2);
}

.status-idle {
  background: rgba(255, 215, 0, 0.1);
  color: var(--dl-warning);
  border: 1px solid rgba(255, 215, 0, 0.2);
}

.status-sold {
  background: rgba(255, 71, 87, 0.1);
  color: var(--dl-danger);
  border: 1px solid rgba(255, 71, 87, 0.2);
}

.status-default {
  background: rgba(255, 255, 255, 0.1);
  color: var(--dl-text-muted);
}
</style>
