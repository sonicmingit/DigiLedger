<template>
  <el-drawer
    v-model="visible"
    :title="drawerTitle"
    size="420px"
    destroy-on-close
    append-to-body
  >
    <div v-if="current" class="wishlist-detail">
      <el-image
        v-if="current.imageUrl"
        :src="current.imageUrl"
        fit="cover"
        class="cover"
        :preview-src-list="[current.imageUrl]"
      />
      <el-descriptions :column="1" border>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType">{{ current.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="期望价">
          {{ formatPrice(current.expectedPrice) }}
        </el-descriptions-item>
        <el-descriptions-item label="类别">
          {{ current.category || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="品牌">
          {{ brandLabel }}
        </el-descriptions-item>
        <el-descriptions-item label="型号">
          {{ current.model || '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="优先级">
          {{ current.priority ?? '-' }}
        </el-descriptions-item>
        <el-descriptions-item label="链接">
          <template v-if="current.link">
            <el-link :href="current.link" type="primary" target="_blank">{{ current.link }}</el-link>
          </template>
          <template v-else>-</template>
        </el-descriptions-item>
        <el-descriptions-item label="标签">
          <template v-if="tags.length">
            <div class="tags">
              <el-tag v-for="tag in tags" :key="tag.id" size="small">{{ tag.name }}</el-tag>
            </div>
          </template>
          <template v-else>-</template>
        </el-descriptions-item>
        <el-descriptions-item label="创建时间">
          {{ formatDateTime(current.createdAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="更新时间">
          {{ formatDateTime(current.updatedAt) }}
        </el-descriptions-item>
        <el-descriptions-item label="关联资产">
          <template v-if="current.convertedAssetId">#{{ current.convertedAssetId }}</template>
          <template v-else>-</template>
        </el-descriptions-item>
        <el-descriptions-item label="备注">
          <div v-if="current.notes" class="notes">{{ current.notes }}</div>
          <template v-else>-</template>
        </el-descriptions-item>
      </el-descriptions>
    </div>
    <div v-else class="empty">暂无心愿数据</div>
  </el-drawer>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { WishlistItem } from '@/types'
import { useDictionaries } from '@/composables/useDictionaries'

const visible = ref(false)
const current = ref<WishlistItem | null>(null)
const { brandMap } = useDictionaries()

const tags = computed(() => current.value?.tags ?? [])
const statusTagType = computed(() => (current.value?.status === '已购买' ? 'success' : 'info'))
const drawerTitle = computed(() => current.value?.name ?? '心愿详情')

const brandLabel = computed(() => {
  if (!current.value) return '-'
  if (current.value.brandName && current.value.brandName.trim().length) {
    return current.value.brandName.trim()
  }
  if (current.value.brandId) {
    const brand = brandMap.value.get(current.value.brandId)
    if (brand) {
      const alias = brand.alias?.trim()
      if (alias) return alias
      if (brand.name) return brand.name
    }
  }
  return '-'
})

const formatPrice = (value?: number | null) => {
  if (value === undefined || value === null) {
    return '-'
  }
  const num = Number(value)
  if (Number.isNaN(num)) {
    return '-'
  }
  return `¥ ${num.toFixed(2)}`
}

const formatDateTime = (value?: string) => {
  if (!value) return '-'
  const date = new Date(value)
  if (Number.isNaN(date.getTime())) return value
  return `${date.toLocaleDateString()} ${date.toLocaleTimeString()}`
}

const open = (item: WishlistItem) => {
  current.value = item
  visible.value = true
}

defineExpose({ open })
</script>

<style scoped>
.wishlist-detail {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.cover {
  width: 100%;
  height: 220px;
  border-radius: 12px;
  object-fit: cover;
}


.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.notes {
  white-space: pre-wrap;
}

.empty {
  color: var(--el-text-color-secondary);
}
</style>
