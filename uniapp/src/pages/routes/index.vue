<template>
  <view class="page route-page">
    <view class="topbar">
      <PageBackButton fallback="/pages/assets/home" />
      <view>
        <text class="page-title">升级路线</text>
        <text class="page-subtitle">查看物品的升级历程</text>
      </view>
    </view>
    <view v-if="loading" class="loading">加载路线…</view>
    <view
      v-else-if="error"
      class="error touch"
      role="button"
      tabindex="0"
      @click="load"
      @keyup.enter="load"
      @keyup.space="load"
      >{{ error }}，点击重试</view
    >
    <view v-else-if="!routes.length" class="empty card">暂无升级路线</view>
    <view
      v-for="route in routes"
      :key="route.id"
      class="route-card card touch"
      role="button"
      tabindex="0"
      @click="openRoute(route.id)"
      @keyup.enter="openRoute(route.id)"
      @keyup.space="openRoute(route.id)"
    >
      <view class="route-covers">
        <image
          v-for="(cover, index) in route.coverImageUrls?.slice(0, 3)"
          :key="cover"
          :src="cover"
          mode="aspectFill"
          aria-hidden="true"
          :style="{ zIndex: 3 - index }"
        />
        <view v-if="!route.coverImageUrls?.length" class="route-fallback">
          <image
            src="/static/icons/upgrade.svg"
            mode="aspectFit"
            aria-hidden="true"
          />
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
    <BottomNav active="routes" />
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad, onPullDownRefresh } from "@dcloudio/uni-app";
import BottomNav from "@/components/BottomNav.vue";
import PageBackButton from "@/components/PageBackButton.vue";
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
  padding-bottom: calc(104px + env(safe-area-inset-bottom));
}
.topbar {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  margin-bottom: var(--space-lg);
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
  margin-bottom: var(--space-sm);
  padding: var(--space-sm);
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
  border: 3px solid var(--color-surface);
  border-radius: var(--radius-input);
}
.route-covers > image:nth-child(2) {
  left: var(--space-sm);
}
.route-covers > image:nth-child(3) {
  left: var(--space-lg);
}
.route-fallback {
  width: 96px;
  height: 96px;
  border-radius: var(--radius-card);
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
  margin-left: var(--space-sm);
}
.route-heading {
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}
.route-heading > text:first-child {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-base);
  font-weight: 700;
}
.route-state {
  padding: var(--space-2xs) var(--space-xs);
  border-radius: var(--radius-pill);
  background: var(--dl-accent-soft);
  font-size: var(--text-xs);
}
.route-period {
  display: block;
  margin-top: var(--space-xs);
  color: var(--dl-text-secondary);
  font-size: var(--text-xs);
}
.route-metrics {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-2xs) var(--space-sm);
  margin-top: var(--space-md);
  font-size: var(--text-xs);
  font-weight: 600;
}
</style>
