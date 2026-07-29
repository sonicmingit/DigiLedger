<template>
  <view class="page route-page">
    <view class="topbar">
      <view class="back touch" @click="back"><view class="back-icon" /></view>
      <view>
        <text class="page-title">升级路线</text>
        <text class="page-subtitle">查看物品的升级历程</text>
      </view>
    </view>
    <view v-if="loading" class="loading">加载路线…</view>
    <view v-else-if="error" class="error" @click="load">{{ error }}，点击重试</view>
    <view v-else-if="!routes.length" class="empty card">暂无升级路线</view>
    <view
      v-for="route in routes"
      :key="route.id"
      class="route-card card touch"
      @click="openRoute(route.id)"
    >
      <view class="route-covers">
        <image
          v-for="(cover, index) in route.coverImageUrls?.slice(0, 3)"
          :key="cover"
          :src="cover"
          mode="aspectFill"
          :style="{ zIndex: 3 - index }"
        />
        <view v-if="!route.coverImageUrls?.length" class="route-fallback">
          <image src="/static/icons/upgrade.svg" mode="aspectFit" />
        </view>
      </view>
      <view class="route-copy">
        <view class="route-heading">
          <text>{{ route.name }}</text>
          <text class="route-state">{{ statusLabel(route.status) }}</text>
        </view>
        <text class="route-period">{{ period(route.periodStart, route.periodEnd) }}</text>
        <view class="route-metrics">
          <text>{{ route.actualSummary?.assetCount || 0 }} 件物品</text>
          <text>净投入 {{ money(route.actualSummary?.netInvestment) }}</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad, onPullDownRefresh } from "@dcloudio/uni-app";
import { api, type UpgradeRoute } from "@/services/api";

const routes = ref<UpgradeRoute[]>([]),
  loading = ref(false),
  error = ref("");

const money = (value?: number) =>
    `¥ ${Number(value || 0).toLocaleString("zh-CN", { maximumFractionDigits: 2 })}`,
  statusLabel = (status?: UpgradeRoute["status"]) =>
    status === "COMPLETED" ? "已完成" : status === "ARCHIVED" ? "已归档" : "进行中",
  period = (start?: string | null, end?: string | null) => {
    if (!start && !end) return "尚未形成时间跨度";
    return `${start?.slice(0, 7) || "—"} 至 ${end?.slice(0, 7) || "现在"}`;
  };

async function load() {
  loading.value = true;
  error.value = "";
  try {
    routes.value = (await api.upgradeRoutes()).sort(
      (a, b) =>
        new Date(b.updatedAt || 0).getTime() - new Date(a.updatedAt || 0).getTime(),
    );
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
}
const back = () => uni.navigateBack(),
  openRoute = (id: number) =>
    uni.navigateTo({ url: `/pages/routes/detail?id=${id}` });

onLoad(load);
onPullDownRefresh(load);
</script>

<style scoped>
.route-page {
  padding-bottom: 36px;
}
.topbar {
  display: flex;
  align-items: flex-start;
  margin-bottom: 22px;
}
.back {
  width: 34px;
  justify-content: flex-start;
}
.back-icon {
  width: 10px;
  height: 10px;
  border-left: 2px solid var(--dl-text);
  border-bottom: 2px solid var(--dl-text);
  transform: rotate(45deg);
}
.page-subtitle {
  display: block;
}
.route-card {
  min-height: 132px;
  margin-bottom: 14px;
  padding: 14px;
  justify-content: flex-start;
}
.route-covers {
  width: 96px;
  min-width: 96px;
  height: 96px;
  position: relative;
}
.route-covers > image {
  position: absolute;
  width: 72px;
  height: 96px;
  border: 3px solid #fff;
  border-radius: 18px;
}
.route-covers > image:nth-child(2) {
  left: 10px;
}
.route-covers > image:nth-child(3) {
  left: 20px;
}
.route-fallback {
  width: 96px;
  height: 96px;
  border-radius: 20px;
  background: var(--dl-accent-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}
.route-fallback image {
  width: 34px;
  height: 34px;
}
.route-copy {
  min-width: 0;
  flex: 1;
  margin-left: 14px;
}
.route-heading {
  display: flex;
  align-items: center;
  gap: 8px;
}
.route-heading > text:first-child {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 700;
}
.route-state {
  padding: 4px 8px;
  border-radius: 999px;
  background: var(--dl-accent-soft);
  font-size: 9px;
}
.route-period {
  display: block;
  margin-top: 8px;
  color: var(--dl-text-secondary);
  font-size: 11px;
}
.route-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: 6px 12px;
  margin-top: 18px;
  font-size: 11px;
  font-weight: 600;
}
</style>

