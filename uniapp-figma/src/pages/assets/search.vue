<template>
  <view class="page search-page">
    <AppHeader title="搜索物品" subtitle="筛选并快速找到物品" />
    <view class="search card"
      ><image class="search-icon" src="/static/icons/search.svg" /><input
        v-model="keyword"
        confirm-type="search"
        placeholder="搜索名称、品牌或备注"
        @confirm="load"
        @input="scheduleLoad"
      /><text v-if="keyword" class="clear touch" @click="clearKeyword">清除</text
    ></view>
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
    <view class="filter-toggle touch" @click="filterExpanded = !filterExpanded"
      ><text>{{ activeFilterCount ? `已选 ${activeFilterCount} 项筛选` : "更多筛选" }}</text
      ><text>{{ filterExpanded ? "收起" : "展开" }}</text></view
    >
    <view v-if="filterExpanded" class="filter-panel card">
      <view class="filter-field"
        ><text>分类</text
        ><picker :range="categoryOptions" range-key="pathLabel" @change="pickCategory"
          ><view class="picker">{{ selectedCategory?.pathLabel || "全部分类" }}</view></picker
        ></view
      ><view class="filter-grid"
        ><view class="filter-field"
          ><text>品牌</text
          ><picker :range="brandOptions" range-key="name" @change="pickBrand"
            ><view class="picker">{{ selectedBrand?.name || "全部品牌" }}</view></picker
          ></view
        ><view class="filter-field"
          ><text>来源平台</text
          ><picker :range="platformOptions" range-key="name" @change="pickPlatform"
            ><view class="picker">{{ selectedPlatform?.name || "全部平台" }}</view></picker
          ></view
        ></view
      ><view class="filter-field"
        ><text>标签</text
        ><view class="tag-list"
          ><view
            v-for="tag in tagOptions"
            :key="tag.id"
            class="tag-choice"
            :class="{ selected: tagIds.includes(tag.id) }"
            @click="toggleTag(tag.id)"
            >{{ tag.name }}</view
          ></view
        ></view
      ><button class="reset" @click="resetFilters">清除筛选</button></view
    >
    <view class="results"
      ><text class="found">找到 {{ assets.length }} 件</text
      ><picker :range="sortOptions" range-key="label" @change="pickSort"
        ><text class="sort">排序：{{ currentSort.label }}</text></picker
      ></view
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
import { computed, ref } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import AppHeader from "@/components/AppHeader.vue";
import AssetCard from "@/components/AssetCard.vue";
import {
  api,
  type Asset,
  type CategoryNode,
  type DictionaryBrand,
  type DictionaryPlatform,
  type DictionaryTag,
} from "@/services/api";
import { categoryPathLabel, flattenTree } from "@/utils/dictionaries";
const keyword = ref(""),
  state = ref("全部"),
  states = ["全部", "使用中", "已闲置", "待出售", "已出售", "已丢弃"],
  assets = ref<Asset[]>([]),
  loading = ref(false),
  error = ref(""),
  filterExpanded = ref(false),
  categories = ref<CategoryNode[]>([]),
  brands = ref<DictionaryBrand[]>([]),
  platforms = ref<DictionaryPlatform[]>([]),
  tags = ref<DictionaryTag[]>([]),
  categoryId = ref<number>(),
  brandId = ref<number>(),
  platformId = ref<number>(),
  tagIds = ref<number[]>([]),
  sortKey = ref("purchase-desc");
const categoryOptions = computed(() => flattenTree(categories.value)),
  tagOptions = computed(() => flattenTree(tags.value)),
  brandOptions = computed(() => brands.value),
  platformOptions = computed(() => platforms.value),
  selectedCategory = computed(() =>
    categoryOptions.value.find((item) => item.id === categoryId.value),
  ),
  selectedBrand = computed(() =>
    brands.value.find((item) => item.id === brandId.value),
  ),
  selectedPlatform = computed(() =>
    platforms.value.find((item) => item.id === platformId.value),
  ),
  activeFilterCount = computed(
    () =>
      Number(!!categoryId.value) +
      Number(!!brandId.value) +
      Number(!!platformId.value) +
      tagIds.value.length,
  );
const sortOptions = [
    { label: "最近购买", value: "purchase-desc" },
    { label: "最早购买", value: "purchase-asc" },
    { label: "名称 A-Z", value: "name-asc" },
    { label: "投入从高到低", value: "cost-desc" },
  ],
  currentSort = computed(
    () => sortOptions.find((item) => item.value === sortKey.value) || sortOptions[0],
  );
function sortAssets(rows: Asset[]) {
  const copy = [...rows];
  if (sortKey.value === "purchase-asc")
    return copy.sort(
      (a, b) =>
        new Date(a.purchaseDate || 0).getTime() -
        new Date(b.purchaseDate || 0).getTime(),
    );
  if (sortKey.value === "name-asc")
    return copy.sort((a, b) => a.name.localeCompare(b.name, "zh-CN"));
  if (sortKey.value === "cost-desc")
    return copy.sort(
      (a, b) =>
        Number(b.totalInvest || b.totalCost || 0) -
        Number(a.totalInvest || a.totalCost || 0),
    );
  return copy.sort(
    (a, b) =>
      new Date(b.purchaseDate || 0).getTime() -
      new Date(a.purchaseDate || 0).getTime(),
  );
}
async function load() {
  loading.value = true;
  error.value = "";
  try {
    const rows = await api.assets({
      q: keyword.value.trim(),
      status: state.value === "全部" ? "" : state.value,
      category_id: categoryId.value,
      brand_id: brandId.value,
      platform_id: platformId.value,
      tag_ids: tagIds.value.join(","),
    });
    assets.value = sortAssets(
      rows.map((asset) => ({
        ...asset,
        categoryName:
          categoryPathLabel(categories.value, asset.categoryId) ||
          asset.categoryName,
      })),
    );
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
let timer: ReturnType<typeof setTimeout> | undefined;
function scheduleLoad() {
  clearTimeout(timer);
  timer = setTimeout(load, 280);
}
function clearKeyword() {
  keyword.value = "";
  load();
}
function pickCategory(e: any) {
  categoryId.value = categoryOptions.value[Number(e.detail.value)]?.id;
  load();
}
function pickBrand(e: any) {
  brandId.value = brandOptions.value[Number(e.detail.value)]?.id;
  load();
}
function pickPlatform(e: any) {
  platformId.value = platformOptions.value[Number(e.detail.value)]?.id;
  load();
}
function toggleTag(id: number) {
  tagIds.value = tagIds.value.includes(id)
    ? tagIds.value.filter((value) => value !== id)
    : [...tagIds.value, id];
  load();
}
function pickSort(e: any) {
  sortKey.value = sortOptions[Number(e.detail.value)]?.value || "purchase-desc";
  assets.value = sortAssets(assets.value);
}
function resetFilters() {
  categoryId.value = undefined;
  brandId.value = undefined;
  platformId.value = undefined;
  tagIds.value = [];
  load();
}
const detail = (id: number) =>
  uni.navigateTo({ url: `/pages/assets/detail?id=${id}` });
onLoad(async () => {
  const [categoryRows, brandRows, platformRows, tagRows] = await Promise.all([
    api.categories().catch(() => []),
    api.brands().catch(() => []),
    api.platforms().catch(() => []),
    api.tags().catch(() => []),
  ]);
  categories.value = categoryRows;
  brands.value = brandRows;
  platforms.value = platformRows;
  tags.value = tagRows;
  load();
});
onShow(() => {
  if (assets.value.length) load();
});
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
.search-icon {
  width: 18px;
  height: 18px;
  margin-right: 17px;
  opacity: 0.65;
}
.clear {
  color: var(--dl-text-secondary);
  font-size: 11px;
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
.filter-toggle {
  justify-content: space-between;
  margin-top: 10px;
  padding: 0 4px;
  color: var(--dl-text-secondary);
  font-size: 12px;
}
.filter-panel {
  margin-top: 4px;
  padding: 16px;
  box-shadow: none;
}
.filter-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.filter-field + .filter-field,
.filter-grid + .filter-field {
  margin-top: 14px;
}
.filter-grid .filter-field + .filter-field {
  margin-top: 0;
}
.filter-field > text {
  display: block;
  margin-bottom: 7px;
  color: var(--dl-text-secondary);
  font-size: 11px;
}
.picker {
  min-height: 46px;
  padding: 0 12px;
  border-radius: 12px;
  background: var(--dl-bg);
  display: flex;
  align-items: center;
  font-size: 12px;
}
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.tag-choice {
  min-height: 34px;
  padding: 0 12px;
  border-radius: 999px;
  background: var(--dl-bg);
  display: flex;
  align-items: center;
  font-size: 11px;
}
.tag-choice.selected {
  background: var(--dl-lime);
  font-weight: 600;
}
.reset {
  min-height: 44px;
  margin-top: 16px;
  border-radius: 999px;
  background: var(--dl-black);
  color: #fff;
  font-size: 12px;
}
</style>
