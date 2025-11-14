<template>
  <div class="mobile-scroll">
    <div class="mobile-search-input">
      <input
        v-model="keyword"
        type="search"
        placeholder="名称、分类、标签、备注、状态等"
        @keyup.enter="search"
      />
      <button type="button" @click="search">搜索</button>
    </div>
    <div class="mobile-search-cancel">
      <button type="button" class="text" @click="goBack">取消</button>
    </div>

    <section v-if="results.length" class="mobile-card-list tight">
      <MobileAssetCard
        v-for="asset in results"
        :key="asset.id"
        :asset="asset"
        @select="goAsset(asset.id)"
      />
    </section>
    <MobileEmptyState v-else description="暂无匹配结果，尝试其他关键词" />

    <div v-if="toast" class="mobile-toast">{{ toast }}</div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import MobileAssetCard from '@/mobile/components/MobileAssetCard.vue'
import MobileEmptyState from '@/mobile/components/MobileEmptyState.vue'
import { fetchAssets } from '@/api/asset'
import type { AssetSummary } from '@/types'

const router = useRouter()
const keyword = ref('')
const results = ref<AssetSummary[]>([])
const toast = ref('')

const search = async () => {
  if (!keyword.value.trim()) {
    toast.value = '请输入搜索关键词'
    setTimeout(() => (toast.value = ''), 1800)
    return
  }
  try {
    const data = await fetchAssets({ keyword: keyword.value })
    results.value = data
  } catch (error) {
    toast.value = '搜索失败，请稍后再试'
    setTimeout(() => (toast.value = ''), 1800)
  }
}

const goBack = () => router.back()
const goAsset = (id: number) => router.push({ name: 'assetDetail', params: { id } })
</script>

<style scoped>
.text {
  border: none;
  background: transparent;
  color: var(--mobile-muted);
}
</style>
