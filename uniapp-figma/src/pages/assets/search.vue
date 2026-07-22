<template>
  <view class="page search-page">
    <AppHeader title="搜索物品" subtitle="按名称、品牌、标签快速定位" />
    <view class="search card"
      ><view class="glass" /><input
        v-model="keyword"
        confirm-type="search"
        placeholder="搜索名称、品牌或备注"
        @confirm="load"
    /></view>
    <text class="section-label">状态</text
    ><scroll-view scroll-x
      ><view class="pills"
        ><view
          v-for="item in states"
          :key="item"
          class="pill"
          :class="{ active: state === item }"
          @click="
            state = item;
            load();
          "
          >{{ item }}</view
        ></view
      ></scroll-view
    >
    <view class="results"
      ><text class="found">找到 {{ assets.length }} 件</text
      ><text class="sort">排序：最近更新</text></view
    >
    <view v-if="loading" class="loading">搜索中…</view
    ><view v-else-if="error" class="error" @click="load">{{ error }}</view
    ><view v-else-if="!assets.length" class="empty card">没有匹配的物品</view>
    <AssetCard
      v-for="(asset, index) in assets"
      :key="asset.id"
      :asset="asset"
      :index="index"
      @select="detail"
    />
  </view>
</template>
<script setup lang="ts">
import { ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import AppHeader from "@/components/AppHeader.vue";
import AssetCard from "@/components/AssetCard.vue";
import { api, type Asset } from "@/services/api";
const keyword = ref(""),
  state = ref("全部"),
  states = ["全部", "使用中", "已闲置", "待出售"],
  assets = ref<Asset[]>([]),
  loading = ref(false),
  error = ref("");
async function load() {
  loading.value = true;
  error.value = "";
  try {
    assets.value = await api.assets({
      keyword: keyword.value,
      status: state.value === "全部" ? "" : state.value,
    });
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
const detail = (id: number) =>
  uni.navigateTo({ url: `/pages/assets/detail?id=${id}` });
onLoad(load);
</script>
<style scoped>
.search-page {
  padding-bottom: 30px;
}
.search {
  height: 52px;
  padding: 0 18px;
  display: flex;
  align-items: center;
  box-shadow: none;
}
.search input {
  flex: 1;
  height: 52px;
  font-size: 13px;
}
.glass {
  position: relative;
  width: 14px;
  height: 14px;
  border: 2px solid var(--dl-text-secondary);
  border-radius: 50%;
  margin-right: 17px;
}
.glass::after {
  content: "";
  position: absolute;
  width: 7px;
  height: 2px;
  background: var(--dl-text-secondary);
  right: -6px;
  bottom: -3px;
  transform: rotate(45deg);
  border-radius: 2px;
}
.section-label {
  display: block;
  margin: 22px 0 9px;
  font-size: 13px;
  color: var(--dl-text-secondary);
}
.pills {
  display: flex;
  gap: 10px;
}
.results {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin: 20px 0 14px;
}
.found {
  font-size: 18px;
  font-weight: 700;
}
.sort {
  font-size: 12px;
  color: var(--dl-text-secondary);
}
</style>
