<template>
  <div
    class="asset-card"
    :class="{ selected, clickable: true, 'is-compact': compact }"
    @click="handleCardClick"
  >
    <div class="card-header">
      <div class="status-label">
        <el-tag size="small">{{ asset.status }}</el-tag>
      </div>
      <el-checkbox v-if="selectable" :model-value="selected" @change="toggleSelect" />
    </div>
    <div class="cover" @click.stop="handleCoverClick">
      <img :src="coverUrl || fallback" alt="缩略图" />
    </div>
    <div class="info">
      <div class="title" :title="asset.name">{{ asset.name }}</div>
      <div class="price">¥ {{ formatNumber(asset.primaryPrice ?? asset.totalInvest) }}</div>
      <div class="meta">
        <span>购买 {{ asset.purchaseDate ? formatDate(asset.purchaseDate) : '未知' }}</span>
        <span>已用 {{ asset.useDays }} 天</span>
      </div>
      <div class="meta">
        <span>日均成本 ¥ {{ formatNumber(asset.avgCostPerDay) }}</span>
      </div>
    </div>
    <div class="tags" v-if="asset.tags?.length">
      <el-tag
        v-for="tag in asset.tags"
        :key="tag.id"
        size="small"
        class="tag"
        :style="tag.color ? { backgroundColor: tag.color, borderColor: tag.color, color: '#0f172a' } : undefined"
      >
        <i v-if="tag.icon" :class="tag.icon" class="tag-icon" />
        {{ tag.name }}
      </el-tag>
    </div>
    <div class="actions">
      <slot name="actions">
        <el-button text size="small" type="primary" @click.stop="emit('view', asset.id)">详情</el-button>
        <el-button text size="small" @click.stop="emit('edit', asset.id)">编辑</el-button>
      </slot>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AssetSummary } from '@/types'
import { buildOssUrl } from '@/utils/storage'

const props = withDefaults(
  defineProps<{
    asset: AssetSummary
    selectable?: boolean
    selected?: boolean
    compact?: boolean
  }>(),
  {
    selectable: false,
    selected: false,
    compact: false
  }
)

const emit = defineEmits<{
  (e: 'view', id: number): void
  (e: 'edit', id: number): void
  (e: 'toggle-select', value: boolean): void
  (e: 'card-click', id: number): void
}>()

const fallback = computed(
  () =>
    `https://dummyimage.com/600x400/1f2937/38bdf8.png&text=${encodeURIComponent(
      props.asset.name.slice(0, 8)
    )}`
)

const coverUrl = computed(() => buildOssUrl(props.asset.coverImageUrl))

const formatNumber = (value: number | undefined) => {
  if (!value && value !== 0) return '0.00'
  return value.toFixed(2)
}

const formatDate = (value: string) => value

const toggleSelect = (value: boolean) => {
  emit('toggle-select', value)
}

const handleCardClick = () => {
  emit('card-click', props.asset.id)
}

const handleCoverClick = () => {
  emit('view', props.asset.id)
}
</script>

<style scoped>
.asset-card {
  position: relative;
  background: linear-gradient(160deg, rgba(30, 64, 175, 0.35), rgba(8, 47, 73, 0.65));
  border: 1px solid rgba(56, 189, 248, 0.24);
  border-radius: 18px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  box-shadow: 0 12px 24px rgba(15, 23, 42, 0.45);
  cursor: default;
}

.asset-card.clickable {
  cursor: pointer;
}

.asset-card.is-compact {
  padding: 12px;
  gap: 8px;
}

.asset-card.is-compact .title {
  font-size: 16px;
}

.asset-card.is-compact .info {
  gap: 4px;
}

.asset-card.is-compact .meta {
  font-size: 12px;
}

.asset-card.is-compact .price {
  font-size: 16px;
}

.asset-card:hover {
  transform: translateY(-4px);
  border-color: rgba(94, 234, 212, 0.6);
  box-shadow: 0 20px 40px rgba(15, 23, 42, 0.55);
}

.asset-card.selected {
  border-color: #22d3ee;
  box-shadow: 0 0 0 2px rgba(34, 211, 238, 0.45);
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.status-label {
  min-width: 80px;
}

.cover {
  width: 100%;
  aspect-ratio: 4 / 3;
  border-radius: 14px;
  overflow: hidden;
  background: rgba(15, 23, 42, 0.75);
  border: 1px solid rgba(148, 163, 184, 0.2);
  cursor: pointer;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #f8fafc;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.price {
  color: #22d3ee;
  font-size: 16px;
  font-weight: 500;
}

.meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #94a3b8;
}

.tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  border-radius: 20px;
}

.tag-icon {
  margin-right: 4px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}

.actions :deep(.el-button.is-text) {
  color: #38bdf8;
}

@media (max-width: 768px) {
  .asset-card {
    padding: 12px;
  }

  .status-toggle {
    min-width: 0;
  }

  .title {
    font-size: 16px;
  }
}
</style>
