<template>
  <div class="overview" v-loading="pageLoading">
    <section class="hero">
      <div class="hero-body">
        <h1>资产总览</h1>
        <p>掌握你的数码物品资产与投入趋势，轻松规划下一台设备。</p>
      </div>
      <div class="hero-actions">
        <el-button type="primary" size="large" @click="createAsset">
          <el-icon class="mr-2"><plus /></el-icon>
          创建物品
        </el-button>
        <el-button text @click="refresh" :loading="loading">刷新数据</el-button>
      </div>
    </section>

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
              stroke="#059669"
              stroke-width="2"
              stroke-linecap="round"
            />
            <polygon :points="trendArea" fill="rgba(5, 150, 105, 0.14)" />
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
            <el-button-group>
              <el-button
                v-for="tab in tabs"
                :key="tab.value"
                :type="tab.value === activeTab ? 'primary' : 'default'"
                size="small"
                @click="changeTab(tab.value)"
              >
                {{ tab.label }}
              </el-button>
            </el-button-group>
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
import { useDictionaries } from '@/composables/useDictionaries'

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

const changeTab = (value: 'all' | number) => {
  activeTab.value = value
}

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

.hero {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: flex-start;
  gap: 16px;
  padding: 28px;
  border-radius: 24px;
  background: linear-gradient(140deg, #ffffff, #ecfdf3);
  border: 1px solid #e5e7eb;
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.08);
}

.hero h1 {
  margin: 0 0 8px;
  font-size: 32px;
  font-weight: 700;
  color: #065f46;
}

.hero p {
  margin: 0;
  color: #4b5563;
}

.hero-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.kpi-grid {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(240px, 1fr));
  gap: 16px;
}

.kpi-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 18px;
  color: #0f172a;
  box-shadow: 0 10px 30px rgba(15, 23, 42, 0.06);
}

.kpi-label {
  color: #6b7280;
  font-size: 14px;
}

.kpi-value {
  margin-top: 8px;
  font-size: 28px;
  font-weight: 600;
  color: #059669;
}

.kpi-desc {
  margin-top: 6px;
  color: #6b7280;
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

.trend-text {
  display: flex;
  flex-direction: column;
  font-size: 12px;
  color: #6b7280;
}

.trend-text strong {
  font-size: 18px;
  color: #065f46;
}

.category-card {
  background: #ffffff;
  border: 1px solid #e5e7eb;
  border-radius: 22px;
  box-shadow: 0 12px 32px rgba(15, 23, 42, 0.06);
}

.category-header {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.tabs :deep(.el-button--primary) {
  background: linear-gradient(130deg, #059669, #10b981);
  border-color: transparent;
}

.quick-search {
  width: 260px;
}

.quick-search :deep(.el-input__wrapper) {
  background: #f3f4f6;
  border: 1px solid #e5e7eb;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.asset-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 16px;
}

@media (max-width: 1024px) {
  .hero {
    flex-direction: column;
  }

  .hero-actions {
    width: 100%;
    justify-content: space-between;
  }
}

@media (max-width: 768px) {
  .overview {
    gap: 16px;
  }

  .hero {
    padding: 20px;
  }

  .hero h1 {
    font-size: 26px;
  }

  .hero-actions {
    flex-wrap: wrap;
  }

  .quick-search {
    width: 100%;
  }

  .asset-grid {
    grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  }
}
</style>
