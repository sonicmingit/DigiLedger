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
        v-if="coverUrl"
        :src="coverUrl"
        fit="cover"
        class="cover"
        :preview-src-list="[coverUrl]"
      />
      <el-descriptions :column="1" border>
        <el-descriptions-item label="状态">
          <el-tag :type="statusTagType">{{ current.status }}</el-tag>
        </el-descriptions-item>
        <el-descriptions-item label="期望价">
          {{ formatPrice(current.expectedPrice) }}
        </el-descriptions-item>
        <el-descriptions-item label="类别">
          {{ categoryLabel }}
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
          <template v-if="relatedAssets.length">
            <ul class="related-assets">
              <li v-for="asset in relatedAssets" :key="asset.assetId" class="related-asset">
                <el-link
                  v-if="asset.available"
                  type="primary"
                  @click="navigateAsset(asset)"
                >
                  {{ asset.assetName || `物品 #${asset.assetId}` }}
                </el-link>
                <span v-else class="invalid-asset">
                  {{ asset.assetName || `物品 #${asset.assetId}` }}
                  <el-tag type="danger" size="small">已失效</el-tag>
                </span>
              </li>
            </ul>
          </template>
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { WishlistAssetRef, WishlistItem } from '@/types'
import { useDictionaries } from '@/composables/useDictionaries'
import { buildOssUrl } from '@/utils/storage'

const visible = ref(false)
const current = ref<WishlistItem | null>(null)
const { brandMap, categoryPathMap } = useDictionaries()
const router = useRouter()

const tags = computed(() => current.value?.tags ?? [])
const statusTagType = computed(() => (current.value?.status === '已购买' ? 'success' : 'info'))
const drawerTitle = computed(() => current.value?.name ?? '心愿详情')
const coverUrl = computed(() => buildOssUrl(current.value?.imageUrl))
const relatedAssets = computed(() => current.value?.relatedAssets ?? [])

const categoryLabel = computed(() => {
  if (!current.value) return '-'
  const explicit = current.value.category ?? current.value.categoryName
  if (explicit && explicit.toString().trim().length) {
    return explicit.toString().trim()
  }
  if (current.value.categoryId) {
    const path = categoryPathMap.value.get(current.value.categoryId)
    if (path && path.trim().length) {
      return path.trim()
    }
  }
  return '-'
})

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

const navigateAsset = (asset: WishlistAssetRef) => {
  if (!asset.assetId) {
    ElMessage.warning('关联的物品信息缺失，无法跳转')
    return
  }
  if (!asset.available) {
    ElMessage.warning('关联的物品不存在或已被删除')
    return
  }
  router.push(`/assets/${asset.assetId}`)
  visible.value = false
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

.related-assets {
  margin: 0;
  padding-left: 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.related-asset {
  list-style: disc;
}

.invalid-asset {
  color: var(--el-text-color-secondary);
  display: inline-flex;
  gap: 6px;
  align-items: center;
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
