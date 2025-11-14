<template>
  <div class="mobile-scroll">
    <header class="mobile-topbar">
      <div class="mobile-topbar-header">
        <h1>DigiLedger</h1>
        <button type="button" class="icon-btn" @click="goSearch">🔍</button>
      </div>
      <p class="subtitle">资产总览</p>
      <div class="mobile-summary-card">
        <div class="mobile-summary-grid">
          <div class="mobile-summary-item">
            <h3>总资产</h3>
            <strong>￥{{ totalValue }}</strong>
          </div>
          <div class="mobile-summary-item">
            <h3>日均成本</h3>
            <strong>￥{{ avgCost }}</strong>
          </div>
        </div>
        <div class="mobile-status-group">
          <span class="mobile-status-chip">
            <span class="dot" style="background:#0ea15c"></span>
            服役中 {{ statusStats.active }}
          </span>
          <span class="mobile-status-chip">
            <span class="dot" style="background:#64748b"></span>
            已退役 {{ statusStats.retired }}
          </span>
          <span class="mobile-status-chip">
            <span class="dot" style="background:#475569"></span>
            已卖出 {{ statusStats.sold }}
          </span>
        </div>
      </div>
    </header>

    <section class="mobile-tab-strip" aria-label="分类筛选">
      <button
        v-for="tab in categoryTabs"
        :key="tab.id"
        class="mobile-tab-button"
        :class="{ active: tab.id === activeCategory }"
        @click="changeCategory(tab.id)"
      >
        {{ tab.name }}
      </button>
    </section>

    <section class="mobile-chip-group" aria-label="状态筛选">
      <button
        v-for="chip in statusChips"
        :key="chip"
        class="mobile-chip"
        :class="{ active: chip === activeStatus }"
        @click="changeStatus(chip)"
      >
        {{ chip }}
      </button>
    </section>

    <div class="mobile-list-header">
      <span>共 {{ assets.length }} 件资产</span>
      <button type="button" @click="toggleSort">{{ sortLabel }}</button>
    </div>

    <section v-if="filteredAssets.length" class="mobile-card-list">
      <MobileAssetCard
        v-for="asset in filteredAssets"
        :key="asset.id"
        :asset="asset"
        @select="goAsset(asset.id)"
      />
    </section>
    <MobileEmptyState v-else description="暂无资产，点击右下角 + 快速添加" />

    <div v-if="toast" class="mobile-toast">{{ toast }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import MobileAssetCard from '@/mobile/components/MobileAssetCard.vue'
import MobileEmptyState from '@/mobile/components/MobileEmptyState.vue'
import { fetchAssets } from '@/api/asset'
import { fetchCategoryTree, type CategoryNode } from '@/api/dict'
import type { AssetSummary } from '@/types'

const router = useRouter()
const assets = ref<AssetSummary[]>([])
const sortLabel = ref('按最新添加')
const activeCategory = ref<number | 'all'>('all')
const activeStatus = ref<'全部' | '服役中' | '已退役' | '已卖出'>('全部')
const categoryTabs = ref<Array<{ id: number | 'all'; name: string }>>([{ id: 'all', name: '全部' }])
const statusChips = ['全部', '服役中', '已退役', '已卖出'] as const
const toast = ref('')

const statusMap: Record<(typeof statusChips)[number], string | undefined> = {
  全部: undefined,
  服役中: '使用中',
  已退役: '已闲置',
  已卖出: '已出售'
}

const loadCategories = async () => {
  const data = await fetchCategoryTree()
  const flattened = flattenCategories(data)
  categoryTabs.value = [{ id: 'all', name: '全部' }, ...flattened]
}

const loadAssets = async () => {
  try {
    const params: Record<string, unknown> = {}
    const status = statusMap[activeStatus.value]
    if (status) params.status = status
    if (typeof activeCategory.value === 'number') params.categoryId = activeCategory.value
    const data = await fetchAssets(params)
    assets.value = data
  } catch (error) {
    toast.value = '获取资产数据失败，请稍后重试'
    setTimeout(() => (toast.value = ''), 2600)
  }
}

const totalValue = computed(() => assets.value.reduce((sum, item) => sum + (item.totalInvest || 0), 0).toFixed(2))
const avgCost = computed(() => {
  if (!assets.value.length) return '0.00'
  const sum = assets.value.reduce((acc, item) => acc + (item.avgCostPerDay || 0), 0)
  return (sum / assets.value.length).toFixed(2)
})

const statusStats = computed(() => {
  const active = assets.value.filter((item) => item.status === '使用中').length
  const retired = assets.value.filter((item) => item.status === '已闲置').length
  const sold = assets.value.filter((item) => item.status === '已出售').length
  return { active, retired, sold }
})

const filteredAssets = computed(() => {
  const list = [...assets.value]
  if (sortLabel.value === '按价值排序') {
    return list.sort((a, b) => b.totalInvest - a.totalInvest)
  }
  return list.sort((a, b) => new Date(b.enabledDate).getTime() - new Date(a.enabledDate).getTime())
})

const flattenCategories = (nodes: CategoryNode[]) => {
  const result: Array<{ id: number; name: string }> = []
  const traverse = (list: CategoryNode[], prefix = '') => {
    list.forEach((node) => {
      result.push({ id: node.id, name: prefix ? `${prefix} / ${node.name}` : node.name })
      if (node.children?.length) {
        traverse(node.children, prefix ? `${prefix} / ${node.name}` : node.name)
      }
    })
  }
  traverse(nodes)
  return result
}

const changeCategory = (id: number | 'all') => {
  activeCategory.value = id
}

const changeStatus = (status: (typeof statusChips)[number]) => {
  activeStatus.value = status
}

const toggleSort = () => {
  sortLabel.value = sortLabel.value === '按最新添加' ? '按价值排序' : '按最新添加'
}

const goAsset = (id: number) => {
  router.push({ name: 'assetDetail', params: { id } })
}

const goSearch = () => {
  router.push({ name: 'mobileSearch' })
}

watch([activeCategory, activeStatus], loadAssets)

onMounted(async () => {
  await loadCategories()
  await loadAssets()
})
</script>
