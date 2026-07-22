<template>
  <view class="page home"
    ><AppHeader title="我的物品" subtitle="让每件物品都清楚、有去处" /><view
      class="summary"
      ><text>物品总值</text
      ><text class="value">{{ money(summary?.totalAssetValue) }}</text
      ><text class="count"
        >共 {{ summary?.assetCount || assets.length }} 件 · 使用中
        {{ summary?.activeCount || 0 }} 件</text
      ><view class="rate">{{
        signed(summary?.monthValueChangeRate)
      }}</view></view
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
import { ref } from "vue";
import { onLoad, onPullDownRefresh, onShow } from "@dcloudio/uni-app";
import AppHeader from "@/components/AppHeader.vue";
import BottomNav from "@/components/BottomNav.vue";
import AssetCard from "@/components/AssetCard.vue";
import { api, type Asset, type Dashboard } from "@/services/api";
const assets = ref<Asset[]>([]),
  summary = ref<Dashboard>(),
  loading = ref(false),
  error = ref(""),
  status = ref("");
const filters = [
  { label: "全部", value: "" },
  { label: "数码", value: "数码" },
  { label: "家居", value: "家居" },
  { label: "闲置", value: "已闲置" },
];
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const [a, s] = await Promise.all([
      api.assets(
        status.value === "数码" || status.value === "家居"
          ? { category_name: status.value }
          : status.value
            ? { status: status.value }
            : {},
      ),
      api.dashboard(),
    ]);
    assets.value = a;
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
  margin: 18px 0 14px;
  white-space: nowrap;
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
