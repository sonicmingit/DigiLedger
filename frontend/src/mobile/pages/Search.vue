<template>
  <div class="page-assets">
    <!-- Sticky Search Header -->
    <header class="search-header">
      <div class="search-bar">
        <i class="mdi mdi-magnify search-icon"></i>
        <input 
          v-model="searchQuery" 
          type="search" 
          placeholder="搜索物品..." 
          @input="onSearch"
        />
        <i v-if="searchQuery" class="mdi mdi-close-circle clear-icon" @click="clearSearch"></i>
      </div>
      
      <!-- Filter Chips -->
      <div class="filter-scroll">
        <button 
          v-for="filter in filters" 
          :key="filter.value"
          class="filter-chip"
          :class="{ active: currentFilter === filter.value }"
          @click="setFilter(filter.value)"
        >
          {{ filter.label }}
        </button>
      </div>
    </header>

    <!-- Asset List -->
    <div class="asset-list-container">
      <div v-if="loading" class="loading-state">
        <i class="mdi mdi-loading mdi-spin"></i> 加载中...
      </div>
      
      <template v-else-if="filteredAssets.length">
        <div class="list-summary">
          共 {{ filteredAssets.length }} 件
        </div>
        <div class="list-items">
          <MobileAssetCard 
            v-for="asset in filteredAssets" 
            :key="asset.id" 
            :asset="asset" 
            @select="goDetail(asset.id)"
          />
        </div>
      </template>

      <div v-else class="empty-state">
        <div class="empty-icon"><i class="mdi mdi-package-variant"></i></div>
        <p>没有找到相关物品</p>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import MobileAssetCard from '@/mobile/components/MobileAssetCard.vue'
import { fetchAssets } from '@/api/asset'
import type { AssetSummary } from '@/types'

const router = useRouter()
const assets = ref<AssetSummary[]>([])
const loading = ref(false)
const searchQuery = ref('')
const currentFilter = ref('all')

const filters = [
  { label: '全部', value: 'all' },
  { label: '使用中', value: '使用中' },
  { label: '闲置', value: '已闲置' },
  { label: '已售出', value: '已出售' },
  { label: '已处理', value: '已丢弃' }
]

const loadAssets = async () => {
  loading.value = true
  try {
    // Determine status param
    const params: any = {}
    if (currentFilter.value !== 'all') {
      params.status = currentFilter.value
    }
    if (searchQuery.value) {
      params.keyword = searchQuery.value
    }
    
    // Note: For client-side snappy feel we might want to load all and filter client side
    // BUT the API supports filtering. Let's try API filtering first.
    // Actually, to make switching filters fast without loading, let's load ALL first if possible?
    // Given the previous code loaded all, let's stick to client side filtering for small datasets (<1000 items)
    // BUT `fetchAssets` takes params. Let's just use API for now to be safe.
    
    const res = await fetchAssets(params)
    assets.value = res || []
  } catch (e) {
    console.error(e)
  } finally {
    loading.value = false
  }
}

// Optimization: Debounce search
let timeout: any
const onSearch = () => {
  clearTimeout(timeout)
  timeout = setTimeout(() => {
    loadAssets()
  }, 300)
}

const clearSearch = () => {
  searchQuery.value = ''
  loadAssets()
}

const setFilter = (val: string) => {
  currentFilter.value = val
  loadAssets()
}

const filteredAssets = computed(() => assets.value)

const goDetail = (id: number) => {
  router.push({ name: 'mobileAssetDetail', params: { id } })
}

onMounted(() => {
  loadAssets()
})
</script>

<style scoped>
.page-assets {
  padding-top: env(safe-area-inset-top);
  background: var(--dl-bg-base);
  min-height: 100vh;
}

.search-header {
  position: sticky;
  top: 0;
  z-index: 10;
  background: rgba(10, 10, 10, 0.95);
  backdrop-filter: blur(12px);
  padding: 12px 20px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
}

.search-bar {
  position: relative;
  margin-bottom: 12px;
}

.search-bar input {
  width: 100%;
  height: 44px;
  background: var(--dl-bg-surface-light);
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 22px;
  padding: 0 40px;
  color: var(--dl-text-primary);
  font-size: 14px;
  outline: none;
  transition: all 0.3s;
}

.search-bar input:focus {
  border-color: var(--dl-primary);
  box-shadow: 0 0 0 2px var(--dl-primary-dim);
}

.search-icon {
  position: absolute;
  left: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--dl-text-muted);
  font-size: 18px;
}

.clear-icon {
  position: absolute;
  right: 14px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--dl-text-muted);
  font-size: 18px;
}

/* Filters */
.filter-scroll {
  display: flex;
  gap: 8px;
  overflow-x: auto;
  padding-bottom: 4px;
  scrollbar-width: none;
  margin: 0 -20px;
  padding: 0 20px 4px;
}
.filter-scroll::-webkit-scrollbar { display: none; }

.filter-chip {
  padding: 6px 16px;
  border-radius: 20px;
  background: var(--dl-bg-surface-light);
  color: var(--dl-text-muted);
  border: 1px solid rgba(255, 255, 255, 0.1);
  font-size: 12px;
  white-space: nowrap;
  transition: all 0.2s;
}

.filter-chip.active {
  background: var(--dl-primary-dim);
  color: var(--dl-primary);
  border-color: var(--dl-primary);
}

/* List */
.asset-list-container {
  padding: 16px 20px;
}

.list-summary {
  font-size: 12px;
  color: var(--dl-text-muted);
  margin-bottom: 12px;
}

.list-items {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.loading-state, .empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 0;
  color: var(--dl-text-muted);
  font-size: 14px;
}

.empty-icon {
  font-size: 48px;
  opacity: 0.3;
  margin-bottom: 16px;
}
</style>
