<template>
  <div class="page dashboard-page">
    <PageHeader title="总览" subtitle="早上好，Sonic。这里是你的物品状态概览。"><PrimaryButton label="新增物品" :icon="plusIcon" @click="workspace.openNewAsset" /></PageHeader>
    <AsyncState :loading="loading" :error="error" :empty="!summary" @retry="load">
      <template v-if="summary">
        <section class="metric-grid">
          <article class="card metric-card"><span>物品总值</span><strong>{{ money(summary.totalAssetValue) }}</strong><small>本月 {{ signed(summary.monthValueChangeRate) }}%</small></article>
          <article class="card metric-card"><span>物品数量</span><strong>{{ summary.assetCount }} 件</strong><small>使用中 {{ summary.activeCount }} 件</small></article>
          <article class="card metric-card"><span>日均成本</span><strong>{{ money(summary.avgDailyCost) }}</strong><small>较上月 {{ signed(summary.monthCostChangeRate) }}%</small></article>
          <article class="card metric-card"><span>闲置物品</span><strong>{{ summary.idleCount }} 件</strong><small>待出售 {{ summary.pendingSaleCount }} 件</small></article>
        </section>
        <section class="dashboard-mid">
          <article class="card trend-card"><div class="card-heading"><h2>价值趋势</h2><span>近一年趋势</span></div><div v-if="yearTrend.length" class="trend-stage"><svg viewBox="0 0 720 180" preserveAspectRatio="none" aria-label="近一年物品价值变化折线图"><polyline :points="trendPoints" fill="none" stroke="#171915" stroke-width="4" stroke-linecap="round" stroke-linejoin="round" /><circle v-for="(p, i) in trendDots" :key="i" :cx="p.x" :cy="p.y" r="5" fill="#b7ff3c" stroke="#171915" stroke-width="3" /></svg><div class="trend-labels"><span v-for="p in yearTrend" :key="p.month">{{ p.month.slice(5) }}月</span></div></div><div v-else class="chart-empty">有更多历史快照后，这里会形成趋势。</div></article>
          <article class="card status-card"><h2>状态分布</h2><div v-for="item in statusDistribution" :key="item.status" class="status-row"><div><span>{{ item.status }}</span><strong>{{ item.count }}</strong></div><div class="progress-track"><div class="progress-fill" :class="{ neutral: item.status !== '使用中', warning: item.status.includes('出售') }" :style="{ width: `${item.count ? Math.max(5, item.count / statusMax * 100) : 0}%` }" /></div></div></article>
        </section>
        <section class="card recent-card"><div class="card-heading"><h2>最近更新</h2><RouterLink to="/assets">查看全部</RouterLink></div><div v-if="summary.recentAssets.length" class="recent-grid"><RouterLink v-for="asset in summary.recentAssets.slice(0, 10)" :key="asset.id" :to="`/assets/${asset.id}`" class="recent-item"><div class="mini-visual"><img v-if="asset.coverImageUrl" :src="asset.coverImageUrl" alt="" /><strong v-else>{{ initials(asset.name) }}</strong></div><div><strong>{{ asset.name }}</strong><span>购买 {{ purchaseDate(asset) }} · 已用 {{ safeNumber(asset.useDays) }} 天 · 日均 {{ money(asset.avgCostPerDay) }} · {{ asset.status }}</span></div><b>{{ money(asset.totalInvest) }}</b></RouterLink></div><div v-else class="chart-empty">暂无最近更新的物品</div></section>
      </template>
    </AsyncState>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchDashboardSummary } from '@/api/dashboard'
import type { AssetSummary, DashboardSummary } from '@/types'
import { useWorkspaceStore } from '@/stores/workspace'
import PageHeader from '@/components/PageHeader.vue'; import PrimaryButton from '@/components/PrimaryButton.vue'; import AsyncState from '@/components/AsyncState.vue'
import plusIcon from '@/assets/icons/plus.svg'
const workspace = useWorkspaceStore(); const summary = ref<DashboardSummary>(); const loading = ref(true); const error = ref('')
const safeNumber = (value: unknown) => { const parsed = Number(value); return Number.isFinite(parsed) ? parsed : 0 }
const money = (value: unknown = 0) => `¥ ${safeNumber(value).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}`
const signed = (value: unknown = 0) => { const parsed = safeNumber(value); return `${parsed > 0 ? '+' : ''}${parsed.toLocaleString('zh-CN', { maximumFractionDigits: 1 })}` }
const initials = (name: string) => name.slice(0, 2)
const purchaseDate = (asset: AssetSummary) => asset.primaryPurchaseDate || asset.purchaseDate || '未记录'
const statusOrder = ['使用中', '已闲置', '待出售', '已出售', '已丢弃']
const statusDistribution = computed(() => {
  const counts = new Map(summary.value?.statusDistribution.map(item => [item.status, safeNumber(item.count)]) || [])
  return statusOrder.map(status => ({ status, count: counts.get(status) || 0 }))
})
const statusMax = computed(() => Math.max(1, ...statusDistribution.value.map(v => v.count)))
const yearTrend = computed(() => summary.value?.valueTrend.slice(-12) || [])
const trendDots = computed(() => { const values = yearTrend.value.map(v => safeNumber(v.value)); const min = Math.min(...values), max = Math.max(...values); return values.map((value, index) => ({ x: 20 + index * (680 / Math.max(1, values.length - 1)), y: 155 - ((value - min) / Math.max(1, max - min)) * 125 })) })
const trendPoints = computed(() => trendDots.value.map(p => `${p.x},${p.y}`).join(' '))
async function load() { loading.value = true; error.value = ''; try { summary.value = await fetchDashboardSummary() } catch (e) { error.value = (e as Error).message } finally { loading.value = false } }
onMounted(load)
</script>

<style scoped>
.dashboard-mid { display: grid; grid-template-columns: minmax(0, 2.4fr) minmax(280px, 1fr); gap: 20px; margin-top: 28px; }.trend-card, .status-card { height: 276px; padding: 20px 22px; }.card-heading { display: flex; justify-content: space-between; align-items: center; }.card-heading h2, .status-card h2 { margin: 0; font-size: 17px; }.card-heading span, .card-heading a { color: var(--dl-text-secondary); font-size: 12px; text-decoration: none; }.trend-stage { height: 210px; }.trend-stage svg { width: 100%; height: 170px; margin-top: 10px; }.trend-labels { display: flex; justify-content: space-between; color: var(--dl-muted); font-size: 10px; }.status-card h2 { margin-bottom: 24px; }.status-row { margin: 0 0 17px; }.status-row > div:first-child { display: flex; justify-content: space-between; margin-bottom: 8px; color: var(--dl-text-secondary); font-size: 12px; }.status-row strong { color: var(--dl-text); }.status-card .progress-track { background: #e7eae4; }.status-row .neutral { background: #cbd0c7; }.status-row .warning { background: var(--dl-warning); }.recent-card { min-height: 230px; margin-top: 28px; padding: 20px 22px; }.recent-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 14px 40px; margin-top: 20px; }.recent-item { display: grid; grid-template-columns: 52px minmax(0, 1fr) auto; gap: 12px; align-items: center; color: inherit; text-decoration: none; }.mini-visual { width: 52px; height: 52px; display: grid; place-items: center; overflow: hidden; border-radius: 16px; background: var(--dl-accent-soft); }.mini-visual img { width: 100%; height: 100%; object-fit: cover; }.recent-item > div:nth-child(2) { min-width: 0; display: flex; flex-direction: column; gap: 4px; }.recent-item strong { font-size: 13px; }.recent-item span { overflow: hidden; color: var(--dl-muted); font-size: 10px; line-height: 16px; text-overflow: ellipsis; white-space: nowrap; }.recent-item b { font-size: 13px; white-space: nowrap; }.chart-empty { height: 180px; display: grid; place-items: center; color: var(--dl-muted); font-size: 12px; }
.trend-card, .status-card { height: 338px; }
</style>
