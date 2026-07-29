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
    ><view class="mobile-tools"
      ><view class="tool-card card touch" @click="openSearch"
        ><image src="/static/icons/search.svg" /><view
          ><text>完整查询</text><text>多条件筛选</text></view
        ></view
      ><view class="tool-card card touch" @click="openRoutes"
        ><image src="/static/icons/upgrade.svg" /><view
          ><text>升级路线</text><text>查看设备历程</text></view
        ></view
      ></view
    ><scroll-view scroll-x class="filters"
      ><view class="filter-row"
        ><view
          v-for="f in filters"
          :key="f.value"
          class="pill"
          :class="{ active: status === f.value }"
          @click="
            status = f.value;
            load();
          "
          >{{ f.label }}</view
        ><view class="more touch" @click="openSearch">查看全部</view></view
      ></scroll-view
    ><view v-if="loading" class="loading">正在整理物品…</view
    ><view v-else-if="error" class="error" @click="load"
      >{{ error }}，点击重试</view
    ><view v-else-if="!assets.length" class="card empty"
      >还没有物品，先添加第一件吧</view
    ><AssetCard
      v-for="(asset, index) in assets.slice(0, 6)"
      :key="asset.id"
      :asset="asset"
      :index="index"
      @select="detail" /><view class="add touch" @click="editor"
      ><image src="/static/icons/plus.svg" /></view
    ><BottomNav active="assets"
  /></view>
</template>
<script setup lang="ts">
import { computed, ref } from "vue";
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
  error = ref(""),
  status = ref<string | number>("");
const filters = computed(() => [
  { label: "全部", value: "" },
  ...categories.value.slice(0, 3).map((category) => ({
    label: category.name,
    value: category.id,
  })),
  { label: "闲置", value: "已闲置" },
]);
async function load() {
  loading.value = true;
  error.value = "";
  try {
    if (!categories.value.length)
      categories.value = await api.categories().catch(() => []);
    const [a, s] = await Promise.all([
      api.assets(
        typeof status.value === "number"
          ? { category_id: status.value }
          : status.value
            ? { status: status.value }
            : {},
      ),
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
  openSearch = () => uni.navigateTo({ url: "/pages/assets/search" }),
  openRoutes = () => uni.navigateTo({ url: "/pages/routes/index" });
onLoad(load);
onShow(() => {
  if (assets.value.length) load();
});
onPullDownRefresh(load);
</script>
<style scoped>
.home {
  padding-left: 20px;
  padding-right: 20px;
}
.summary {
  height: 142px;
  position: relative;
  padding: 18px 20px;
  border-radius: 28px;
  background: var(--dl-lime);
  font-size: 13px;
}
.value,
.count {
  display: block;
}
.value {
  margin-top: 5px;
  font-size: 34px;
  font-weight: 700;
  line-height: 45px;
}
.count {
  margin-top: 8px;
  font-size: 12px;
  font-weight: 600;
}
.rate {
  position: absolute;
  right: 19px;
  top: 18px;
  width: 76px;
  height: 76px;
  border-radius: 20px;
  background: var(--dl-black);
  color: #fff;
  font-size: 14px;
  font-weight: 700;
  display: flex;
  align-items: center;
  justify-content: center;
}
.filters {
  margin: 14px 0;
  white-space: nowrap;
}
.mobile-tools {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin-top: 12px;
}
.tool-card {
  min-height: 66px;
  padding: 0 14px;
  justify-content: flex-start;
  box-shadow: none;
}
.tool-card image {
  width: 22px;
  height: 22px;
  margin-right: 10px;
}
.tool-card text {
  display: block;
}
.tool-card text:first-child {
  font-size: 13px;
  font-weight: 700;
}
.tool-card text:last-child {
  margin-top: 3px;
  color: var(--dl-text-secondary);
  font-size: 10px;
}
.filter-row {
  display: flex;
  gap: 10px;
  align-items: center;
}
.more {
  margin-left: auto;
  color: var(--dl-text-secondary);
  font-size: 11px;
}
.add {
  position: fixed;
  right: 20px;
  bottom: calc(92px + env(safe-area-inset-bottom));
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--dl-black);
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
}
.add image {
  width: 24px;
  height: 24px;
}
</style>
