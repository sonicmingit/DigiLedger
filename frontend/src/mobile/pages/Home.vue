<template>
  <div class="mobile-scroll">
    <header class="mobile-topbar mobile-home-top">
      <div class="mobile-topbar-header">
        <div>
          <p class="mobile-home-eyebrow">资产看板</p>
          <h1>DigiLedger</h1>
        </div>
        <button type="button" class="icon-btn" @click="goSearch">搜</button>
      </div>

      <div class="mobile-home-hero">
        <div>
          <p class="mobile-home-subtitle">掌握装备资产、心愿与趋势数据</p>
        </div>
        <div class="mobile-home-hero-stat">
          <span>当前资产</span>
          <strong>￥{{ totalValue }}</strong>
          <button type="button" class="link-button" @click="goStats">查看统计</button>
        </div>
      </div>

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

    <section class="mobile-home-actions">
      <button type="button" class="mobile-action-card" @click="goAssets">
        <span class="action-title">查看全部物品</span>
        <small>{{ assets.length }} 件资产</small>
      </button>
      <button type="button" class="mobile-action-card primary" @click="openEditor('asset')">
        <span class="action-title">新增物品</span>
        <small>立即记录装备和采购</small>
      </button>
      <button type="button" class="mobile-action-card" @click="goWishlist">
        <span class="action-title">心愿清单</span>
        <small>{{ wishlistCount }} 条心愿</small>
      </button>
      <button type="button" class="mobile-action-card primary" @click="openEditor('wishlist')">
        <span class="action-title">新增心愿</span>
        <small>留住灵感，下次再入手</small>
      </button>
    </section>

    <section class="mobile-card-section mobile-home-wishlist">
      <div class="mobile-section-header">
        <div>
          <h4>心愿快照</h4>
          <p>总价值 ￥{{ wishlistTotalValue }} · 共 {{ wishlistCount }} 条</p>
        </div>
        <button type="button" class="link-button" @click="goWishlist">查看全部</button>
      </div>
      <div v-if="wishlistPreview.length" class="mobile-wishlist-preview">
        <article v-for="item in wishlistPreview" :key="item.id" class="mobile-wishlist-preview-item">
          <div class="mobile-wishlist-preview-heading">
            <span class="mobile-wishlist-preview-title">{{ item.name }}</span>
            <span class="mobile-wishlist-pill">{{ item.status }}</span>
          </div>
          <div class="mobile-wishlist-preview-meta">
            <span>预期 ￥{{ (item.expectedPrice || 0).toFixed(2) }}</span>
            <small>{{ item.categoryName || '未分类' }}</small>
          </div>
          <p v-if="item.notes" class="mobile-wishlist-preview-notes">{{ item.notes }}</p>
        </article>
      </div>
      <MobileEmptyState
        v-else
        description="心愿列表还空着，先添加一条吧"
        actionLabel="新增心愿"
        @action="openEditor('wishlist')"
      />
    </section>

    <section class="mobile-card-section mobile-status-panels">
      <div class="mobile-section-header">
        <div>
          <h4>状态速览</h4>
          <p>分布信息一目了然</p>
        </div>
        <button type="button" class="link-button" @click="goStats">查看趋势</button>
      </div>
      <div class="mobile-status-grid">
        <article v-for="status in quickStatus" :key="status.label" class="mobile-status-card">
          <div class="mobile-status-label">
            <span class="dot" :style="{ background: status.color }"></span>
            <span>{{ status.label }}</span>
          </div>
          <strong>{{ status.count }} 件</strong>
          <p>{{ status.description }}</p>
        </article>
      </div>
    </section>

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

    <section class="mobile-card-section mobile-assets-section">
      <div class="mobile-list-header">
        <span>共 {{ filteredAssets.length }} 件资产</span>
        <div class="mobile-list-actions">
          <button type="button" @click="toggleSort">{{ sortLabel }}</button>
          <button type="button" @click="goStats">统计</button>
        </div>
      </div>
      <div v-if="filteredAssets.length" class="mobile-card-list tight">
        <MobileAssetCard
          v-for="asset in filteredAssets"
          :key="asset.id"
          :asset="asset"
          @select="goAsset(asset.id)"
        />
      </div>
      <MobileEmptyState
        v-else
        description="暂无资产，点击上方按钮或右下角 + 快速添加"
        actionLabel="新增资产"
        @action="openEditor('asset')"
      />
    </section>

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
import { fetchWishlist } from '@/api/wishlist'
import type { AssetSummary, WishlistItem } from '@/types'

const router = useRouter()
const assets = ref<AssetSummary[]>([])
const wishlist = ref<WishlistItem[]>([])
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

const showToast = (message: string, duration = 2200) => {
  toast.value = message
  setTimeout(() => {
    toast.value = ''
  }, duration)
}

const loadCategories = async () => {
  const data = await fetchCategoryTree()
  categoryTabs.value = [{ id: 'all', name: '全部' }, ...flattenCategories(data)]
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
    showToast('获取资产数据失败，请稍后重试')
  }
}

const loadWishlist = async () => {
  try {
    const data = await fetchWishlist()
    wishlist.value = data
  } catch (error) {
    showToast('加载心愿列表失败')
  }
}

const totalValue = computed(() =>
  assets.value.reduce((sum, item) => sum + (item.totalInvest || 0), 0).toFixed(2)
)

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
  const toTimestamp = (value?: string) => (value ? new Date(value).getTime() : 0)
  return list.sort((a, b) => toTimestamp(b.purchaseDate) - toTimestamp(a.purchaseDate))
})

const wishlistPreview = computed(() => wishlist.value.slice(0, 3))
const wishlistCount = computed(() => wishlist.value.length)
const wishlistTotalValue = computed(() =>
  wishlist.value.reduce((sum, item) => sum + (item.expectedPrice || 0), 0).toFixed(2)
)

const quickStatus = computed(() => [
  { label: '服役中', count: statusStats.value.active, color: '#0ea15c', description: '正在服役中的资产' },
  { label: '已退役', count: statusStats.value.retired, color: '#64748b', description: '暂未配置的资产' },
  { label: '已卖出', count: statusStats.value.sold, color: '#475569', description: '已完成出售记录' }
])

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

const goWishlist = () => {
  router.push({ name: 'mobileWishlist' })
}

const goStats = () => {
  router.push({ name: 'mobileStats' })
}

const openEditor = (type: 'asset' | 'wishlist') => {
  router.push({ name: 'mobileEditor', query: { type } })
}

const goAssets = () => {
  router.push({ name: 'assets' })
}

watch([activeCategory, activeStatus], loadAssets)

onMounted(async () => {
  await loadCategories()
  await Promise.all([loadAssets(), loadWishlist()])
})
</script>
