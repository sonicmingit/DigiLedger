<template>
  <div
    class="asset-card"
    :class="{ selected, clickable: true, 'is-compact': compact }"
    @click="handleCardClick"
  >
    <div class="card-header">
      <div class="status-label">
        <el-tag
          size="small"
          round
          disable-transitions
          :style="statusStyle"
        >
          {{ asset.status }}
        </el-tag>
      </div>
      <el-checkbox v-if="selectable" :model-value="selected" @change="toggleSelect" />
    </div>
    <div class="cover" @click.stop="handleCoverClick">
      <img :src="coverUrl || fallback" alt="缩略图" />
      <div v-if="!coverUrl" class="cover-empty">
        <el-button type="text" size="mini" @click.stop="suggestCover">设置封面</el-button>
      </div>
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
        <IconRenderer :icon="tag.icon" />
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
import IconRenderer from '@/components/IconRenderer.vue'
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
  (e: 'suggest-cover', asset: AssetSummary): void
}>()

const fallback = computed(
  () =>
    `https://dummyimage.com/600x400/1f2937/38bdf8.png&text=${encodeURIComponent(
      props.asset.name.slice(0, 8)
    )}`
)

const coverUrl = computed(() => buildOssUrl(props.asset.coverImageUrl))

const statusStyle = computed(() => {
  const palette: Record<string, { backgroundColor: string; color: string; borderColor: string }> = {
    使用中: { backgroundColor: '#d1fae5', color: '#065f46', borderColor: '#bbf7d0' },
    已闲置: { backgroundColor: '#ecfeff', color: '#0ea5e9', borderColor: '#bae6fd' },
    待出售: { backgroundColor: '#fef9c3', color: '#92400e', borderColor: '#fef08a' },
    已出售: { backgroundColor: '#ffe4e6', color: '#be123c', borderColor: '#fecdd3' },
    已丢弃: { backgroundColor: '#f3f4f6', color: '#374151', borderColor: '#e5e7eb' }
  }
  return palette[props.asset.status] || { backgroundColor: '#ecfeff', color: '#0f172a', borderColor: '#bae6fd' }
})

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

const suggestCover = () => {
  emit('suggest-cover', props.asset)
}

const handleCoverClick = () => {
  emit('view', props.asset.id)
}
</script>

<style scoped>

.asset-card {
  position: relative;
  background: var(--dl-card);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--dl-radius-lg);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 12px;
  transition: transform 0.25s ease, box-shadow 0.25s ease, border-color 0.25s ease;
  box-shadow: var(--dl-shadow-md);
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
  transform: translateY(-3px);
  border-color: color-mix(in srgb, var(--dl-accent) 36%, var(--el-border-color-lighter));
  box-shadow: var(--dl-shadow-lg);
}

.asset-card.selected {
  border-color: color-mix(in srgb, var(--dl-accent) 50%, var(--el-border-color-lighter));
  box-shadow: 0 0 0 2px color-mix(in srgb, var(--dl-accent) 22%, transparent), var(--dl-shadow-md);
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
  background: linear-gradient(145deg, var(--dl-bg-alt), color-mix(in srgb, var(--dl-accent) 8%, var(--dl-bg-alt)));
  border: 1px dashed color-mix(in srgb, var(--dl-accent) 36%, transparent);
  cursor: pointer;
  position: relative;
}

.cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-empty {
  position: absolute;
  inset: 0;
  background: color-mix(in srgb, var(--dl-card) 78%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 14px;
}

.info {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: var(--dl-text);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.price {
  color: var(--dl-accent);
  font-size: 18px;
  font-weight: 700;
}

.meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--dl-muted);
}

.tags {
  display: flex;
  gap: 6px;
  flex-wrap: wrap;
}

.tag {
  border-radius: 18px;
}

.tag-icon {
  margin-right: 4px;
}

.tag-icon-svg {
  width: 16px;
  height: 16px;
  vertical-align: text-bottom;
  margin-right: 6px;
}

.actions {
  display: flex;
  justify-content: flex-end;
  gap: 4px;
}

.actions :deep(.el-button.is-text) {
  color: var(--dl-accent);
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
