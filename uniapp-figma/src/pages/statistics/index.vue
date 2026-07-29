<template>
  <view class="page"
    ><AppHeader title="数据统计" subtitle="查看价值与使用成本" /><view
      v-if="loading"
      class="loading"
      >计算数据中…</view
    ><view v-else-if="error" class="error" @click="load">{{ error }}</view
    ><template v-else-if="data"
      ><view class="total"
        ><text>当前物品总值</text
        ><text class="total-value">{{ money(data.totalAssetValue) }}</text
        ><text class="rate"
          >较上月 {{ signed(data.monthValueChangeRate) }}</text
        ></view
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
        ><scroll-view scroll-x class="status-scroll"
          ><view class="status-row"
            ><view v-for="row in data.statusDistribution || []" :key="row.status"
              ><text>{{ row.count }}</text><text>{{ row.status }}</text></view
            ></view></scroll-view
        ></view
      ><view class="card trend-card"
        ><text class="card-title">价值趋势</text
        ><view v-if="data.valueTrend?.length" class="trend-list"
          ><view v-for="row in data.valueTrend" :key="row.month"
            ><text>{{ row.month }}</text><text>{{ money(row.value) }}</text></view
          ></view
        ><text v-else class="trend-empty">暂无趋势数据</text></view
      ><view class="cost"
        ><text>平均每日使用成本</text
        ><text class="cost-value">{{ money(data.avgDailyCost) }}</text
        ><text class="down">{{ signed(data.monthCostChangeRate) }}</text></view
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
const categories = computed(
    () => data.value?.categoryDistribution || [],
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
.total {
  height: 130px;
  border-radius: 28px;
  background: var(--dl-lime);
  padding: 18px 20px;
  position: relative;
  font-size: 13px;
}
.total-value {
  display: block;
  font-size: 34px;
  font-weight: 700;
  margin-top: 8px;
}
.rate {
  position: absolute;
  right: 20px;
  bottom: 20px;
  font-size: 12px;
}
.metrics {
  display: flex;
  gap: 10px;
  margin: 22px 0 20px;
}
.metrics .card {
  flex: 1;
  height: 98px;
  padding: 17px 16px;
}
.metrics text {
  display: block;
}
.metrics text:first-child {
  font-size: 27px;
  font-weight: 700;
}
.metrics text:last-child {
  margin-top: 7px;
  font-size: 12px;
  color: var(--dl-text-secondary);
}
.distribution {
  padding: 17px 18px 20px;
}
.card-title {
  font-size: 17px;
  font-weight: 700;
}
.bar-row {
  display: flex;
  align-items: center;
  margin-top: 17px;
  font-size: 11px;
}
.bar-row > text {
  width: 52px;
  color: var(--dl-text-secondary);
}
.track {
  flex: 1;
  height: 10px;
  border-radius: 99px;
  background: var(--dl-bg-alt);
  overflow: hidden;
}
.bar {
  height: 100%;
  border-radius: 99px;
  background: var(--dl-black);
}
.bar.lime {
  background: var(--dl-lime);
}
.cost {
  height: 112px;
  margin-top: 18px;
  padding: 17px 18px;
  border-radius: 28px;
  background: var(--dl-black);
  color: #fff;
  position: relative;
}
.status-card,
.trend-card {
  margin-top: 18px;
  padding: 17px 18px 20px;
  box-shadow: none;
}
.status-scroll {
  width: calc(100vw - 58px);
  margin-top: 14px;
}
.status-row {
  display: flex;
  gap: 9px;
  padding-right: 10px;
}
.status-row > view {
  min-width: 92px;
  padding: 12px;
  border-radius: 16px;
  background: var(--dl-bg);
}
.status-row text {
  display: block;
}
.status-row text:first-child {
  font-size: 19px;
  font-weight: 700;
}
.status-row text:last-child {
  margin-top: 5px;
  color: var(--dl-text-secondary);
  font-size: 10px;
}
.trend-list {
  margin-top: 10px;
}
.trend-list view {
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--dl-bg);
  font-size: 11px;
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
  padding: 24px 0 8px;
  color: var(--dl-muted);
  text-align: center;
  font-size: 11px;
}
.cost > text:first-child {
  font-size: 12px;
  color: var(--dl-muted);
}
.cost-value {
  display: block;
  font-size: 28px;
  font-weight: 700;
  margin-top: 10px;
}
.down {
  position: absolute;
  right: 20px;
  bottom: 36px;
  color: var(--dl-lime);
  font-size: 12px;
}
</style>
