<template>
  <div class="mobile-scroll">
    <header class="mobile-topbar">
      <div class="mobile-topbar-header">
        <h1>统计趋势</h1>
      </div>
      <p class="subtitle">财富曲线、状态分布与热门分类一览</p>
    </header>

    <template v-if="assets.length">
      <section class="mobile-card-section mobile-stats-overview">
        <article class="mobile-stats-card">
          <h3>资产总价值</h3>
          <strong>￥{{ totalValue }}</strong>
          <p>{{ assetCount }} 件 · 日均成本 ￥{{ avgCost }}</p>
        </article>
        <article class="mobile-stats-card">
          <h4>当前概况</h4>
          <p>
            其中最多的是 {{ mostFrequentStatus.label }} 状态，有 {{ mostFrequentStatus.count }} 件，占比
            {{ mostFrequentStatus.percent.toFixed(0) }}%
          </p>
        </article>
      </section>

      <section class="mobile-card-section">
        <div class="mobile-section-header">
          <h4>状态分布</h4>
        </div>
        <div class="mobile-status-grid">
          <article v-for="item in statusDistribution" :key="item.label" class="mobile-status-card">
            <div class="mobile-status-label">
              <span class="dot" :style="{ background: item.color }"></span>
              <span>{{ item.label }}</span>
            </div>
            <strong>{{ item.count }} 件</strong>
            <div class="mobile-progress-track">
              <span class="mobile-progress-fill" :style="{ width: `${item.percent}%`, background: item.color }"></span>
            </div>
            <small>{{ item.percent.toFixed(0) }}%</small>
          </article>
        </div>
      </section>

      <section v-if="categoryStats.length" class="mobile-card-section">
        <div class="mobile-section-header">
          <h4>热度分类</h4>
        </div>
        <ul class="mobile-category-stats">
          <li v-for="item in categoryStats" :key="item.name">
            <div>
              <strong>{{ item.name }}</strong>
              <small>{{ item.count }} 件</small>
            </div>
            <span>{{ item.percent.toFixed(1) }}%</span>
          </li>
        </ul>
      </section>
    </template>

    <MobileEmptyState
      v-else
      description="暂无资产数据，添加后即可查看统计"
      actionLabel="新增资产"
      @action="openEditor"
    />

    <div v-if="toast" class="mobile-toast">{{ toast }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MobileEmptyState from '@/mobile/components/MobileEmptyState.vue'
import { fetchAssets } from '@/api/asset'
import type { AssetSummary } from '@/types'

const router = useRouter()
const assets = ref<AssetSummary[]>([])
const toast = ref('')

const totalValue = computed(() =>
  assets.value.reduce((sum, item) => sum + (item.totalInvest || 0), 0).toFixed(2)
)

const assetCount = computed(() => assets.value.length)

const avgCost = computed(() => {
  if (!assets.value.length) return '0.00'
  const sum = assets.value.reduce((acc, item) => acc + (item.avgCostPerDay || 0), 0)
  return (sum / assets.value.length).toFixed(2)
})

const statusColors: Record<'使用中' | '已闲置' | '已出售', string> = {
  使用中: '#0ea15c',
  已闲置: '#64748b',
  已出售: '#475569'
}

const statusDistribution = computed(() => {
  const total = assetCount.value
  const list: Array<{ label: '使用中' | '已闲置' | '已出售'; count: number; percent: number; color: string }> =
    ['使用中', '已闲置', '已出售'].map((label) => {
      const count = assets.value.filter((item) => item.status === label).length
      const percent = total ? (count / total) * 100 : 0
      return { label, count, percent, color: statusColors[label] }
    })
  return list
})

const mostFrequentStatus = computed(() => {
  if (!assets.value.length) {
    return { label: '无', count: 0, percent: 0 }
  }
  return statusDistribution.value.reduce((prev, curr) => (curr.count > prev.count ? curr : prev))
})

const categoryStats = computed(() => {
  const map = new Map<string, number>()
  assets.value.forEach((item) => {
    const name = item.categoryPath || '未分类'
    map.set(name, (map.get(name) ?? 0) + 1)
  })
  const list = Array.from(map.entries())
    .sort((a, b) => b[1] - a[1])
    .slice(0, 3)
  return list.map(([name, count]) => ({
    name,
    count,
    percent: assetCount.value ? (count / assetCount.value) * 100 : 0
  }))
})

const openEditor = () => {
  router.push({ name: 'mobileEditor', query: { type: 'asset' } })
}

const loadAssets = async () => {
  try {
    const data = await fetchAssets()
    assets.value = data
  } catch (error) {
    toast.value = '统计数据加载失败'
    setTimeout(() => (toast.value = ''), 2200)
  }
}

onMounted(loadAssets)
</script>
