<template>
  <view class="page home"
    ><AppHeader title="我的物品" subtitle="管理物品与使用成本" /><view
      class="summary"
      ><text>物品总值</text
      ><text class="value">{{ money(summary?.totalAssetValue) }}</text
      ><text class="count"
        >共 {{ summary?.assetCount || assets.length }} 件 · 使用中
        {{ summary?.activeCount || 0 }} 件</text
      ><view class="rate">{{
        signed(summary?.monthValueChangeRate)
      }}</view></view
    ><view class="workbench"
      ><view
        class="query-dock card touch"
        role="button"
        tabindex="0"
        @click="openSearch"
        @keyup.enter="openSearch"
        @keyup.space="openSearch"
        ><view class="query-icon"
          ><image src="/static/icons/search.svg" aria-hidden="true" /></view
        ><view class="query-copy"
          ><text>查找物品</text><text>名称、品牌、备注与多条件筛选</text></view
        ><text class="query-action">开始</text></view
      ></view
    ><view class="list-head"
      ><text>物品列表</text
      ><text
        class="list-action touch"
        role="button"
        tabindex="0"
        @click="openSearch"
        @keyup.enter="openSearch"
        @keyup.space="openSearch"
        >查看全部</text
      ></view
    ><view v-if="loading" class="loading">正在整理物品…</view
    ><view
      v-else-if="error"
      class="error touch"
      role="button"
      tabindex="0"
      @click="load"
      @keyup.enter="load"
      @keyup.space="load"
      >{{ error }}，点击重试</view
    ><view v-else-if="!assets.length" class="card empty"
      >还没有物品，先添加第一件吧</view
    ><AssetCard
      v-for="(asset, index) in assets.slice(0, 6)"
      :key="asset.id"
      :asset="asset"
      :index="index"
      @select="detail" /><view
      class="add touch"
      role="button"
      tabindex="0"
      aria-label="录入物品"
      @click="editor"
      @keyup.enter="editor"
      @keyup.space="editor"
    >
      <image src="/static/icons/plus.svg" aria-hidden="true" /></view
    ><BottomNav active="assets"
  /></view>
</template>
<script setup lang="ts">
import { ref } from "vue";
import { onLoad, onPullDownRefresh, onShow } from "@dcloudio/uni-app";
import AppHeader from "@/components/AppHeader.vue";
import BottomNav from "@/components/BottomNav.vue";
import AssetCard from "@/components/AssetCard.vue";
import {
  api,
  type Asset,
  type CategoryNode,
  type Dashboard,
} from "@/services/api";
import { categoryPathLabel } from "@/utils/dictionaries";
const assets = ref<Asset[]>([]),
  summary = ref<Dashboard>(),
  categories = ref<CategoryNode[]>([]),
  loading = ref(false),
  error = ref("");
async function load() {
  loading.value = true;
  error.value = "";
  try {
    if (!categories.value.length)
      categories.value = await api.categories().catch(() => []);
    const [a, s] = await Promise.all([
      api.assets(),
      api.dashboard(),
    ]);
    assets.value = a.map((asset) => ({
      ...asset,
      categoryName:
        categoryPathLabel(categories.value, asset.categoryId) ||
        asset.categoryName,
    }));
    summary.value = s;
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
}
const money = (n?: number) => `¥ ${Number(n || 0).toLocaleString("zh-CN")}`,
  signed = (n?: number) =>
    `${Number(n || 0) >= 0 ? "+" : ""}${Number(n || 0).toFixed(1)}%`;
const detail = (id: number) =>
    uni.navigateTo({ url: `/pages/assets/detail?id=${id}` }),
  editor = () => uni.navigateTo({ url: "/pages/assets/editor" }),
  openSearch = () => uni.navigateTo({ url: "/pages/assets/search" });
onLoad(load);
onShow(() => {
  if (assets.value.length) load();
});
onPullDownRefresh(load);
</script>
<style scoped>
.home {
  padding-left: max(var(--space-md), env(safe-area-inset-left));
  padding-right: max(var(--space-md), env(safe-area-inset-right));
}
.summary {
  min-height: 128px;
  position: relative;
  overflow: hidden;
  padding: var(--space-md) var(--space-lg);
  border: var(--rule-hairline);
  border-radius: var(--radius-card-strong);
  background: var(--color-surface);
  color: var(--color-ink);
  font-size: var(--text-sm);
  box-shadow: var(--shadow-card);
}
.summary::before {
  content: "";
  position: absolute;
  inset: 0 0 auto;
  height: var(--space-xs);
  background: var(--color-accent);
}
.value,
.count {
  display: block;
}
.value {
  margin-top: var(--space-2xs);
  font-family: var(--font-display);
  font-size: var(--text-display);
  font-weight: 700;
  line-height: 1.18;
  letter-spacing: -0.025em;
  font-variant-numeric: tabular-nums;
}
.count {
  margin-top: var(--space-xs);
  font-size: var(--text-xs);
  font-weight: 600;
}
.rate {
  position: absolute;
  right: var(--space-md);
  top: var(--space-md);
  min-width: 64px;
  height: 36px;
  padding: 0 var(--space-sm);
  border-radius: var(--radius-pill);
  background: var(--color-coral-soft);
  color: var(--color-ink);
  font-family: var(--font-label);
  font-size: var(--text-xs);
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.workbench {
  margin-top: var(--space-sm);
}
.query-dock {
  min-height: 76px;
  padding: var(--space-sm);
  justify-content: flex-start;
}
.query-icon {
  width: 48px;
  height: 48px;
  flex: 0 0 auto;
  border-radius: 16px;
  background: var(--color-cyan-soft);
  display: grid;
  place-items: center;
}
.query-icon image {
  width: 22px;
  height: 22px;
}
.query-copy {
  min-width: 0;
  margin-left: var(--space-sm);
}
.query-copy text {
  display: block;
}
.query-copy text:first-child {
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: 700;
}
.query-copy text:last-child {
  margin-top: var(--space-2xs);
  color: var(--color-ink-2);
  font-size: var(--text-xs);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.query-action {
  margin-left: auto;
  padding: 0 var(--space-sm);
  color: var(--color-focus);
  font-size: var(--text-sm);
  font-weight: 700;
  white-space: nowrap;
}
.list-head {
  min-height: 42px;
  margin-top: var(--space-md);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.list-head > text:first-child {
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-weight: 700;
}
.list-action {
  min-height: 44px;
  color: var(--color-focus);
  font-size: var(--text-sm);
  font-weight: 600;
}
.add {
  position: fixed;
  z-index: var(--z-raised);
  right: max(
    var(--space-md),
    calc((100% - var(--app-max)) / 2 + var(--space-md))
  );
  bottom: calc(86px + env(safe-area-inset-bottom));
  width: 48px;
  min-width: 48px;
  height: 48px;
  padding: 0;
  border-radius: var(--radius-pill);
  border: 1px solid var(--color-accent-deep);
  background: var(--color-accent);
  color: var(--color-accent-ink);
  box-shadow: var(--shadow-raised);
}
.add image {
  width: 20px;
  height: 20px;
  filter: invert(1);
  transition: transform var(--dur-short) var(--ease-out);
  animation: add-cue 1.8s var(--ease-in-out) 2;
}
@keyframes add-cue {
  0%,
  66%,
  100% {
    transform: rotate(0);
  }
  82% {
    transform: rotate(90deg);
  }
}
@media (hover: hover) and (pointer: fine) {
  .add:hover {
    transform: none;
  }
  .add:hover image {
    transform: rotate(90deg);
  }
}
@media (prefers-reduced-motion: reduce) {
  .add image {
    animation: none;
    transition: none;
  }
  .add:hover image {
    transform: none;
  }
}
</style>
