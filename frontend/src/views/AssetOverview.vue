<template>
  <div class="overview" v-loading="pageLoading">
    <PageHeader title="资产总览" subtitle="掌握你的数码物品资产与投入趋势，轻松规划下一台设备。" variant="hero">
      <template #actions>
        <el-button type="primary" @click="createAsset">
          <el-icon class="mr-2"><plus /></el-icon>
          创建物品
        </el-button>
        <el-button text @click="refresh" :loading="loading">刷新数据</el-button>
      </template>
    </PageHeader>

    <div class="kpi-grid">
      <el-card class="kpi-card">
        <div class="kpi-label">在库 / 总数</div>
        <div class="kpi-value">{{ activeCount }} / {{ totalCount }}</div>
        <div class="kpi-desc">{{ soldCount }} 件已售出</div>
      </el-card>
      <el-card class="kpi-card">
        <div class="kpi-label">总投入</div>
        <div class="kpi-value">¥ {{ formatNumber(totalInvest) }}</div>
        <div class="kpi-desc">含主购、配件、服务投入</div>
      </el-card>
      <el-card class="kpi-card">
        <div class="kpi-label">近30天趋势</div>
        <div class="kpi-trend">
          <svg viewBox="0 0 100 36" preserveAspectRatio="none">
            <polyline
              :points="trendPoints"
              fill="none"
              class="kpi-trend__line"
              stroke-width="2"
              stroke-linecap="round"
            />
            <polygon :points="trendArea" class="kpi-trend__area" />
          </svg>
          <div class="trend-text">
            <strong>{{ purchasesInWindow }}</strong>
            <span>件新购入</span>
          </div>
        </div>
      </el-card>
    </div>

    <el-card class="category-card">
      <template #header>
        <div class="category-header">
          <div class="tabs">
            <SegmentedTabs v-model="activeTab" :items="tabs" size="small" />
          </div>
          <el-input
            v-model="quickSearch"
            placeholder="快速搜索物品名称/品牌"
            clearable
            class="quick-search"
          >
            <template #prefix>
              <el-icon><search /></el-icon>
            </template>
          </el-input>
        </div>
      </template>
      <div v-if="filteredAssets.length" class="asset-grid">
        <asset-card
          v-for="item in filteredAssets"
          :key="item.id"
          :asset="item"
          status-readonly
          @card-click="viewDetail"
          @view="viewDetail"
          @edit="openEdit"
        />
      </div>
      <el-empty v-else description="暂无物品，尝试切换类别或创建新物品" />
    </el-card>

    <asset-form ref="assetForm" @success="refresh" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { Plus, Search } from '@element-plus/icons-vue'
import { fetchAssets } from '@/api/asset'
import type { AssetSummary } from '@/types'
import AssetForm from '@/views/assets/components/AssetForm.vue'
import AssetCard from '@/components/AssetCard.vue'
import SegmentedTabs from '@/components/SegmentedTabs.vue'
import { useDictionaries } from '@/composables/useDictionaries'
import PageHeader from '@/components/PageHeader.vue'

const router = useRouter()
const assets = ref<AssetSummary[]>([])
const loading = ref(false)
const pageLoading = ref(false)
const assetForm = ref<InstanceType<typeof AssetForm> | null>(null)
const quickSearch = ref('')
const activeTab = ref<'all' | number>('all')
const isMobile = ref(false)

const { load: loadDicts, categoryTree, categoryPathMap } = useDictionaries()

const tabs = computed(() => {
  const root = [{ label: '全部', value: 'all' as const }]
  const topLevels = categoryTree.value.map((node) => ({
    label: node.name,
    value: node.id
  }))
  return [...root, ...topLevels]
})

const categoryTopMap = computed(() => {
  const map = new Map<number, number>()
  const walk = (nodes: any[], topId?: number) => {
    nodes.forEach((node) => {
      const currentTop = topId ?? node.id
      map.set(node.id, currentTop)
      if (node.children?.length) {
        walk(node.children, currentTop)
      }
    })
  }
  walk(categoryTree.value)
  return map
})

const refresh = async () => {
  loading.value = true
  try {
    assets.value = await fetchAssets()
  } finally {
    loading.value = false
  }
}

const formatNumber = (value: number) => value.toFixed(2)

const totalCount = computed(() => assets.value.length)
const soldCount = computed(() => assets.value.filter((item) => item.status === '已出售').length)
const activeCount = computed(() => totalCount.value - soldCount.value)
const totalInvest = computed(() =>
  assets.value.reduce((sum, item) => sum + (item.totalInvest || 0), 0)
)

const trendWindow = computed(() => {
  const days = 30
  const today = new Date()
  const result: Array<{ label: string; count: number }> = []
  for (let i = days - 1; i >= 0; i -= 1) {
    const date = new Date(today)
    date.setDate(today.getDate() - i)
    result.push({ label: date.toISOString().slice(0, 10), count: 0 })
  }
  const indexMap = new Map(result.map((item, idx) => [item.label, idx]))
  assets.value.forEach((asset) => {
    if (!asset.purchaseDate) return
    if (indexMap.has(asset.purchaseDate)) {
      const idx = indexMap.get(asset.purchaseDate)!
      result[idx].count += 1
    }
  })
  return result
})

const purchasesInWindow = computed(() =>
  trendWindow.value.reduce((sum, item) => sum + item.count, 0)
)

const maxTrendValue = computed(() =>
  Math.max(1, ...trendWindow.value.map((item) => item.count))
)

const trendPoints = computed(() => {
  if (!trendWindow.value.length) return ''
  return trendWindow.value
    .map((item, index) => {
      const x = (index / (trendWindow.value.length - 1)) * 100
      const y = 35 - (item.count / maxTrendValue.value) * 30
      return `${x.toFixed(2)},${y.toFixed(2)}`
    })
    .join(' ')
})

const trendArea = computed(() => {
  if (!trendWindow.value.length) return ''
  const base = `${trendPoints.value} 100,36 0,36`
  return base
})

const filteredAssets = computed(() => {
  const keyword = quickSearch.value.trim().toLowerCase()
  return assets.value
    .filter((item) => {
      if (activeTab.value !== 'all' && item.categoryId) {
        const topId = categoryTopMap.value.get(item.categoryId)
        if (topId !== activeTab.value) return false
      }
      if (!keyword) return true
      const text = `${item.name} ${item.categoryPath ?? ''} ${item.tags
        .map((tag) => tag.name)
        .join(' ')}`.toLowerCase()
      return text.includes(keyword)
    })
    .slice(0, isMobile.value ? 8 : 12)
})

const viewDetail = (id: number) => {
  router.push(`/assets/${id}`)
}

const openEdit = (id: number) => {
  router.push({ path: '/assets', query: { edit: id.toString() } })
}

const createAsset = () => {
  if (isMobile.value) {
    router.push({ path: '/assets', query: { create: '1' } })
    return
  }
  assetForm.value?.open()
}

const handleResize = () => {
  isMobile.value = window.innerWidth <= 768
}

onMounted(async () => {
  pageLoading.value = true
  await loadDicts()
  await refresh()
  handleResize()
  window.addEventListener('resize', handleResize)
  pageLoading.value = false
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
})

watch(activeTab, () => {
  if (activeTab.value !== 'all' && !categoryPathMap.value.size) {
    loadDicts()
  }
})
</script>

<style scoped>
.overview {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.kpi-card {
  background: var(--dl-card);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--dl-radius-lg);
  color: var(--dl-text);
  box-shadow: var(--dl-shadow-md);
}

.kpi-label {
  color: var(--dl-muted);
  font-size: 14px;
}

.kpi-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 600;
  color: var(--dl-accent);
}

.kpi-desc {
  margin-top: 6px;
  color: var(--dl-muted);
}

.kpi-trend {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: 12px;
}

.kpi-trend svg {
  width: 120px;
  height: 36px;
}

.kpi-trend__line {
  stroke: var(--dl-accent);
}

.kpi-trend__area {
  fill: color-mix(in srgb, var(--dl-accent) 16%, transparent);
}

.trend-text {
  display: flex;
  flex-direction: column;
  font-size: 12px;
  color: var(--dl-muted);
}

.trend-text strong {
  font-size: 18px;
  color: var(--el-color-primary-dark-2);
}

.category-card {
  background: var(--dl-card);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--dl-radius-lg);
  box-shadow: var(--dl-shadow-md);
}

.category-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.quick-search {
  width: 260px;
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

@media (max-width: 1024px) {
  .overview {
    gap: 20px;
  }
}

@media (max-width: 768px) {
  .overview {
    gap: 16px;
  }

  .quick-search {
    width: 100%;
  }

  .asset-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  }
}
</style>
