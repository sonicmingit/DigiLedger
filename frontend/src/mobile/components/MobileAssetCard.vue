<template>
  <div class="asset-card" @click="emit('select', asset)">
    <div class="asset-cover">
      <img v-if="asset.coverImageUrl" :src="asset.coverImageUrl" :alt="asset.name" />
      <div v-else class="placeholder">📦</div>
      <span class="asset-status" :class="statusClass">
        <span class="dot"></span>
        {{ statusLabel }}
      </span>
    </div>
    <div class="asset-content">
      <h3>{{ asset.name }}</h3>
      <p class="asset-meta">
        <span>￥{{ asset.totalInvest.toFixed(2) }}</span>
        <span>日均：￥{{ asset.avgCostPerDay.toFixed(2) }}</span>
      </p>
      <div class="asset-tags" v-if="asset.tags && asset.tags.length">
        <span v-for="tag in asset.tags" :key="tag.id" class="tag" :style="getTagStyle(tag)">
          <span class="dot" :style="getDotStyle(tag)"></span>
          {{ tag.name }}
        </span>
      </div>
      <footer>
        <small>已使用 {{ asset.useDays }} 天</small>
        <button class="detail-btn" type="button">查看详情</button>
      </footer>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AssetSummary } from '@/types'

const props = defineProps<{ asset: AssetSummary }>()
const emit = defineEmits<{ (e: 'select', asset: AssetSummary): void }>()

const statusMap: Record<string, { label: string; color: string; dot: string }> = {
  使用中: { label: '服役中', color: 'rgba(46, 204, 113, 0.2)', dot: '#0ea15c' },
  已闲置: { label: '已退役', color: 'rgba(148, 163, 184, 0.15)', dot: '#64748b' },
  待出售: { label: '待出售', color: 'rgba(249, 115, 22, 0.18)', dot: '#f97316' },
  已出售: { label: '已卖出', color: 'rgba(148, 163, 184, 0.2)', dot: '#475569' },
  已丢弃: { label: '已处理', color: 'rgba(148, 163, 184, 0.2)', dot: '#94a3b8' }
}

const statusInfo = computed(() => statusMap[props.asset.status] || statusMap['使用中'])
const statusLabel = computed(() => statusInfo.value.label)
const statusClass = computed(() => ({
  sold: props.asset.status === '已出售',
  retired: props.asset.status === '已闲置'
}))

const getTagStyle = (tag: { color?: string }) => ({
  background: tag.color ? `${tag.color}1A` : 'rgba(148, 163, 184, 0.15)'
})

const getDotStyle = (tag: { color?: string }) => ({
  background: tag.color || '#94a3b8'
})
</script>

<style scoped>
.asset-card {
  display: flex;
  flex-direction: column;
  border-radius: 24px;
  background: #fff;
  box-shadow: var(--mobile-card-shadow);
  overflow: hidden;
  position: relative;
}

.asset-cover {
  position: relative;
  height: 140px;
  overflow: hidden;
}

.asset-cover img {
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
  background: rgba(148, 163, 184, 0.12);
  font-size: 32px;
}

.asset-status {
  position: absolute;
  right: 12px;
  top: 12px;
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border-radius: 999px;
  background: v-bind('statusInfo.color');
  color: #0f172a;
  font-size: 12px;
}

.asset-status .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: v-bind('statusInfo.dot');
}

.asset-content {
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.asset-content h3 {
  margin: 0;
  font-size: 16px;
}

.asset-meta {
  display: flex;
  justify-content: space-between;
  color: var(--mobile-muted);
  font-size: 12px;
  margin: 0;
}

.asset-tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.tag {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
}

.tag .dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
}

footer {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 12px;
  color: var(--mobile-muted);
}

.detail-btn {
  border: none;
  background: transparent;
  color: #0ea15c;
  font-size: 12px;
}
</style>
