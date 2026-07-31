<template>
  <view class="page"
    ><AppHeader title="数据统计" subtitle="查看价值与使用成本" /><view
      v-if="loading"
      class="loading"
      >计算数据中…</view
    ><view
      v-else-if="error"
      class="error touch"
      role="button"
      tabindex="0"
      @click="load"
      @keyup.enter="load"
      @keyup.space="load"
      >{{ error }}</view
    ><template v-else-if="data"
      ><view class="total"
        ><text>当前物品总值</text
        ><text class="total-value">{{ money(data.totalAssetValue) }}</text
        ><text class="rate"
          >较上月 {{ signed(data.monthValueChangeRate) }}</text
        ></view
      ><view class="cost"
        ><text>平均每日使用成本</text
        ><text class="cost-value">{{ money(data.avgDailyCost) }}</text
        ><text class="down">{{ signed(data.monthCostChangeRate) }}</text></view
      ><view class="metrics"
        ><view class="card"
          ><text>{{ data.assetCount }}</text
          ><text>物品数量</text></view
        ><view class="card"
          ><text>{{ data.activeCount }}</text
          ><text>使用中</text></view
        ><view class="card"
          ><text>{{ data.idleCount }}</text
          ><text>已闲置</text></view
        ></view
      ><view class="card distribution"
        ><text class="card-title">分类价值分布</text
        ><view
          v-for="(row, index) in categories"
          :key="row.categoryName"
          class="bar-row"
          ><text>{{ row.categoryName }}</text
          ><view class="track"
            ><view
              class="bar"
              :class="{ lime: index === 0 }"
              :style="{ width: barWidth(row.value) }" /></view></view></view
      ><view class="card status-card"
        ><text class="card-title">状态分布</text
        ><view class="status-proportion"
          ><view
            v-for="(row, index) in statuses"
            :key="`bar-${row.status}`"
            class="status-segment"
            :class="`status-${index}`"
            :style="{ flexGrow: row.count || 0.12 }"
        /></view
        ><view class="status-row"
          ><view
            v-for="(row, index) in statuses"
            :key="row.status"
            :class="`status-${index}`"
            ><text>{{ row.count }}</text><text>{{ row.status }}</text></view
          ></view
        ></view
      ><view class="card trend-card"
        ><text class="card-title">价值趋势</text
        ><view v-if="data.valueTrend?.length" class="trend-list"
          ><view v-for="row in data.valueTrend" :key="row.month"
            ><text>{{ row.month }}</text><text>{{ money(row.value) }}</text></view
          ></view
        ><text v-else class="trend-empty">暂无趋势数据</text></view
      ></template
    ><BottomNav active="statistics"
  /></view>
</template>
<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad, onPullDownRefresh } from "@dcloudio/uni-app";
import AppHeader from "@/components/AppHeader.vue";
import BottomNav from "@/components/BottomNav.vue";
import { api, type Dashboard } from "@/services/api";
const data = ref<Dashboard>(),
  loading = ref(false),
  error = ref("");
const statusOrder = ["使用中", "已闲置", "待出售", "已出售", "已丢弃"];
const categories = computed(
    () => data.value?.categoryDistribution || [],
  ),
  statuses = computed(() =>
    statusOrder.map((status) => ({
      status,
      count:
        data.value?.statusDistribution?.find((row) => row.status === status)
          ?.count || 0,
    })),
  ),
  max = computed(() =>
    Math.max(...categories.value.map((x) => Number(x.value)), 1),
  );
const money = (n?: number) =>
    `¥ ${Number(n || 0).toLocaleString("zh-CN", { minimumFractionDigits: Number(n) % 1 ? 2 : 0, maximumFractionDigits: 2 })}`,
  signed = (n?: number) =>
    `${Number(n || 0) >= 0 ? "+" : ""}${Number(n || 0).toFixed(1)}%`,
  barWidth = (n: number) => `${Math.max(8, (n / max.value) * 100)}%`;
async function load() {
  loading.value = true;
  error.value = "";
  try {
    data.value = await api.dashboard();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
}
onLoad(load);
onPullDownRefresh(load);
</script>
<style scoped>
/* Hallmark · asymmetric personal inventory metrics */
.total {
  min-height: 128px;
  position: relative;
  overflow: hidden;
  border: var(--rule-hairline);
  border-radius: var(--radius-card-strong);
  background: var(--color-surface);
  color: var(--color-ink);
  padding: var(--space-md) var(--space-lg);
  font-size: var(--text-sm);
  box-shadow: var(--shadow-card);
}
.total::before {
  content: "";
  position: absolute;
  inset: 0 0 auto;
  height: var(--space-xs);
  background: var(--color-accent);
}
.total-value {
  display: block;
  font-family: var(--font-display);
  font-size: var(--text-display);
  font-weight: 700;
  margin-top: var(--space-xs);
  letter-spacing: -0.025em;
  font-variant-numeric: tabular-nums;
}
.rate {
  position: absolute;
  right: var(--space-lg);
  bottom: var(--space-lg);
  font-size: var(--text-sm);
}
.metrics {
  display: grid;
  grid-template-columns: minmax(0, 1.25fr) minmax(0, 0.75fr);
  grid-template-rows: repeat(2, 64px);
  gap: var(--space-xs);
  margin: var(--space-md) 0;
}
.metrics .card {
  min-width: 0;
  padding: var(--space-sm) var(--space-md);
  box-shadow: none;
}
.metrics .card:first-child {
  grid-row: 1 / span 2;
  background: var(--color-cyan-soft);
  display: flex;
  flex-direction: column;
  justify-content: center;
}
.metrics .card:nth-child(2) {
  background: var(--color-accent-soft);
}
.metrics .card:nth-child(3) {
  background: var(--color-coral-soft);
}
.metrics text {
  display: block;
}
.metrics text:first-child {
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 700;
  line-height: 1;
  font-variant-numeric: tabular-nums;
}
.metrics .card:first-child text:first-child {
  font-size: var(--text-xl);
}
.metrics text:last-child {
  margin-top: var(--space-xs);
  color: var(--color-ink-2);
  font-size: var(--text-xs);
}
.distribution {
  padding: var(--space-md) var(--space-md) var(--space-lg);
}
.card-title {
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: 700;
}
.bar-row {
  display: flex;
  align-items: center;
  margin-top: var(--space-md);
  font-size: var(--text-xs);
}
.bar-row > text {
  width: 52px;
  color: var(--dl-text-secondary);
}
.track {
  flex: 1;
  height: 10px;
  border-radius: var(--radius-pill);
  background: var(--dl-bg-alt);
  overflow: hidden;
}
.bar {
  height: 100%;
  border-radius: var(--radius-pill);
  background: var(--dl-black);
}
.bar.lime {
  background: var(--dl-lime);
}
.cost {
  height: 112px;
  margin-top: var(--space-md);
  padding: var(--space-md);
  border-radius: var(--radius-card-strong);
  background: var(--color-ink);
  color: var(--color-paper);
  position: relative;
}
.status-card,
.trend-card {
  margin-top: var(--space-md);
  padding: var(--space-md) var(--space-md) var(--space-lg);
  box-shadow: none;
}
.status-proportion {
  width: 100%;
  height: 8px;
  margin-top: var(--space-sm);
  overflow: hidden;
  border-radius: var(--radius-pill);
  background: var(--color-paper-2);
  display: flex;
  gap: 2px;
}
.status-segment {
  min-width: 2px;
  flex-basis: 0;
  border-radius: var(--radius-pill);
}
.status-row {
  margin-top: var(--space-sm);
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 5px;
}
.status-row > view {
  min-width: 0;
  padding: var(--space-sm) 6px;
  border-top: 3px solid transparent;
  border-radius: var(--radius-input);
  background: var(--color-paper-2);
}
.status-row text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.status-row text:first-child {
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: 700;
}
.status-row text:last-child {
  margin-top: var(--space-2xs);
  color: var(--dl-text-secondary);
  font-size: 10px;
}
.status-0 {
  border-color: var(--color-accent-deep) !important;
  background: var(--color-accent-soft);
}
.status-1 {
  border-color: var(--color-coral) !important;
  background: var(--color-coral-soft);
}
.status-2 {
  border-color: var(--color-warning) !important;
  background: var(--color-paper-2);
}
.status-3 {
  border-color: var(--color-cyan) !important;
  background: var(--color-cyan-soft);
}
.status-4 {
  border-color: var(--color-muted) !important;
  background: var(--color-paper-3);
}
.trend-list {
  margin-top: var(--space-sm);
}
.trend-list view {
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--dl-bg);
  font-size: var(--text-xs);
}
.trend-list view:last-child {
  border-bottom: 0;
}
.trend-list text:first-child {
  color: var(--dl-text-secondary);
}
.trend-list text:last-child {
  font-weight: 700;
}
.trend-empty {
  display: block;
  padding: var(--space-xl) 0 var(--space-xs);
  color: var(--dl-muted);
  text-align: center;
  font-size: var(--text-xs);
}
.cost > text:first-child {
  font-size: var(--text-xs);
  color: var(--dl-muted);
}
.cost-value {
  display: block;
  font-family: var(--font-display);
  font-size: var(--text-xl);
  font-weight: 700;
  margin-top: var(--space-sm);
}
.down {
  position: absolute;
  right: var(--space-lg);
  bottom: var(--space-xl);
  color: var(--dl-lime);
  font-size: var(--text-xs);
}
</style>
