<template>
  <view class="page search-page">
    <view class="search-page__header">
      <PageBackButton fallback="/pages/assets/home" />
      <AppHeader title="搜索物品" subtitle="筛选并快速找到物品" />
    </view>
    <view class="search card"
      ><image
        class="search-icon"
        src="/static/icons/search.svg"
        aria-hidden="true"
      /><input
        v-model="keyword"
        confirm-type="search"
        placeholder="搜索名称、品牌或备注"
        @confirm="load"
        @input="scheduleLoad"
      /><text
        v-if="keyword"
        class="clear touch"
        role="button"
        tabindex="0"
        @click="clearKeyword"
        @keyup.enter="clearKeyword"
        @keyup.space="clearKeyword"
        >清除</text
    ></view>
    <text class="section-label">状态</text>
    <view class="pills">
      <view
        v-for="item in states"
        :key="item"
        class="pill"
        :class="{ active: state === item }"
        role="button"
        tabindex="0"
        :aria-pressed="state === item"
        @click="
          state = item;
          load();
        "
        @keyup.enter="
          state = item;
          load();
        "
        @keyup.space="
          state = item;
          load();
        "
        >{{ item }}</view
      >
    </view>
    <view
      class="filter-toggle touch"
      role="button"
      tabindex="0"
      :aria-expanded="filterExpanded"
      @click="filterExpanded = !filterExpanded"
      @keyup.enter="filterExpanded = !filterExpanded"
      @keyup.space="filterExpanded = !filterExpanded"
      ><text>{{ activeFilterCount ? `已选 ${activeFilterCount} 项筛选` : "更多筛选" }}</text
      ><text>{{ filterExpanded ? "收起" : "展开" }}</text></view
    >
    <view v-if="filterExpanded" class="filter-panel card">
      <view class="filter-field"
        ><text>分类</text
        ><TreeSelect
          v-model="categoryId"
          :nodes="categories"
          placeholder="全部分类"
          clear-label="全部分类"
          search-placeholder="搜索分类"
          @change="load()"
        /></view
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
        ><TreeSelect
          v-model="tagIds"
          :nodes="tags"
          multiple
          placeholder="不限标签"
          clear-label="不限标签"
          search-placeholder="搜索标签"
          @change="load()"
        /></view
      ><view class="reset-row"
        ><view
          class="reset touch"
          role="button"
          tabindex="0"
          :aria-disabled="!activeFilterCount"
          @click="activeFilterCount && resetFilters()"
          @keyup.enter="activeFilterCount && resetFilters()"
          @keyup.space="activeFilterCount && resetFilters()"
          >清除筛选</view
        ></view
      ></view
    >
    <view class="results">
      <text class="found" aria-live="polite">找到 {{ assets.length }} 件</text>
      <view class="result-actions">
        <picker :range="sortOptions" range-key="label" @change="pickSort">
          <text class="sort">排序：{{ currentSort.label }}</text>
        </picker>
        <view
          class="view-toggle touch"
          role="button"
          tabindex="0"
          :aria-label="
            viewMode === 'list' ? '切换为图片模式' : '切换为列表模式'
          "
          :aria-pressed="viewMode === 'grid'"
          @click="toggleView"
          @keyup.enter="toggleView"
          @keyup.space="toggleView"
        >
          <view
            class="mode-icon"
            :class="viewMode === 'list' ? 'grid-cue' : 'list-cue'"
            aria-hidden="true"
          >
            <view /><view /><view /><view />
          </view>
        </view>
      </view>
    </view>
    <view v-if="loading" class="loading">搜索中…</view
    ><view
      v-else-if="error"
      class="error touch"
      role="button"
      tabindex="0"
      @click="load"
      @keyup.enter="load"
      @keyup.space="load"
      >{{ error }}</view
    ><view v-else-if="!assets.length" class="empty card">没有匹配的物品</view>
    <view v-if="viewMode === 'grid'" class="asset-grid">
      <view
        v-for="asset in assets"
        :key="asset.id"
        class="asset-tile touch"
        role="button"
        tabindex="0"
        :aria-label="`${asset.name}，${money(asset.totalInvest ?? asset.totalCost ?? asset.currentValue)}`"
        @click="detail(asset.id)"
        @keyup.enter="detail(asset.id)"
        @keyup.space="detail(asset.id)"
      >
        <image
          v-if="asset.coverImageUrl"
          :src="resolveMediaUrl(asset.coverImageUrl)"
          mode="aspectFill"
          aria-hidden="true"
        />
        <view v-else class="tile-fallback" aria-hidden="true">{{
          asset.name.slice(0, 2)
        }}</view>
        <view class="tile-copy">
          <text>{{ asset.name }}</text>
          <text>{{
            money(asset.totalInvest ?? asset.totalCost ?? asset.currentValue)
          }}</text>
        </view>
      </view>
    </view>
    <template v-else>
      <AssetCard
        v-for="(asset, index) in assets"
        :key="asset.id"
        :asset="asset"
        :index="index"
        @select="detail"
      />
    </template>
  </view>
</template>
<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import AppHeader from "@/components/AppHeader.vue";
import PageBackButton from "@/components/PageBackButton.vue";
import AssetCard from "@/components/AssetCard.vue";
import TreeSelect from "@/components/TreeSelect.vue";
import {
  api,
  type Asset,
  type CategoryNode,
  type DictionaryBrand,
  type DictionaryPlatform,
  type DictionaryTag,
} from "@/services/api";
import { categoryPathLabel } from "@/utils/dictionaries";
import { resolveMediaUrl } from "@/services/media";
const keyword = ref(""),
  state = ref("全部"),
  states = ["全部", "使用中", "已闲置", "待出售", "已出售", "已丢弃"],
  assets = ref<Asset[]>([]),
  viewMode = ref<"list" | "grid">("list"),
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
const brandOptions = computed(() => brands.value),
  platformOptions = computed(() => platforms.value),
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
function pickBrand(e: any) {
  brandId.value = brandOptions.value[Number(e.detail.value)]?.id;
  load();
}
function pickPlatform(e: any) {
  platformId.value = platformOptions.value[Number(e.detail.value)]?.id;
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
function toggleView() {
  viewMode.value = viewMode.value === "list" ? "grid" : "list";
}
const money = (value?: number) =>
  `¥ ${Number(value || 0).toLocaleString("zh-CN", {
    maximumFractionDigits: 2,
  })}`;
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
  padding-bottom: var(--space-xl);
}
.search-page__header {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
}
.search-page__header :deep(.header) {
  flex: 1;
  min-width: 0;
}
.search {
  min-height: 56px;
  padding: 0 var(--space-md);
  display: flex;
  align-items: center;
  border-color: var(--color-rule);
  box-shadow: var(--shadow-card);
}
.search input {
  flex: 1;
  height: 54px;
  min-width: 0;
  font-size: var(--text-base);
}
.search-icon {
  width: 18px;
  height: 18px;
  margin-right: var(--space-sm);
  opacity: 0.78;
}
.clear {
  color: var(--color-focus);
  font-size: var(--text-sm);
  font-weight: 600;
  white-space: nowrap;
}
.section-label {
  display: block;
  margin: var(--space-lg) 0 var(--space-xs);
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  font-weight: 600;
}
.pills {
  display: flex;
  width: 100%;
  gap: var(--space-2xs);
}
.pills .pill {
  min-width: 0;
  flex: 1;
  padding: 0 var(--space-2xs);
  font-size: var(--text-xs);
  white-space: nowrap;
}
.results {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: var(--space-xs);
  margin: var(--space-lg) 0 var(--space-sm);
}
.found {
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-weight: 700;
}
.sort {
  color: var(--color-focus);
  font-size: var(--text-sm);
  white-space: nowrap;
}
.result-actions {
  min-width: 0;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: var(--space-xs);
}
.view-toggle {
  width: 44px;
  min-width: 44px;
  min-height: 44px;
  border: var(--rule-hairline);
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  color: var(--color-ink);
}
.mode-icon {
  width: 14px;
  height: 14px;
}
.mode-icon.grid-cue {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  grid-template-rows: repeat(2, 1fr);
  gap: var(--space-3xs);
}
.mode-icon.grid-cue > view {
  border-radius: var(--space-3xs);
  background: currentColor;
}
.mode-icon.list-cue {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: var(--space-3xs);
}
.mode-icon.list-cue > view {
  width: 100%;
  height: 2px;
  border-radius: var(--radius-pill);
  background: currentColor;
}
.mode-icon.list-cue > view:last-child {
  display: none;
}
.asset-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-sm);
}
.asset-tile {
  position: relative;
  min-width: 0;
  min-height: 0;
  aspect-ratio: 1;
  padding: 0;
  border: var(--rule-hairline);
  border-radius: var(--radius-card);
  background: var(--color-paper-2);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.asset-tile > image,
.tile-fallback {
  width: 100%;
  height: 100%;
}
.tile-fallback {
  background: var(--color-accent-soft);
  color: var(--color-accent-ink);
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 700;
}
.tile-copy {
  position: absolute;
  inset: auto 0 0;
  min-width: 0;
  min-height: 56px;
  padding: var(--space-xs) var(--space-sm);
  background: var(--color-ink);
  color: var(--color-paper);
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  justify-content: center;
}
.tile-copy text {
  display: block;
  max-width: 100%;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.tile-copy text:first-child {
  font-size: var(--text-sm);
  font-weight: 700;
}
.tile-copy text:last-child {
  margin-top: var(--space-2xs);
  font-family: var(--font-label);
  font-size: var(--text-xs);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
.filter-toggle {
  justify-content: space-between;
  min-height: 48px;
  margin-top: var(--space-xs);
  padding: 0 var(--space-sm);
  border-radius: var(--radius-input);
  background: var(--color-cyan-soft);
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  font-weight: 600;
}
.filter-panel {
  margin-top: var(--space-xs);
  padding: var(--space-md);
  box-shadow: none;
}
.filter-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--space-xs);
}
.filter-field + .filter-field,
.filter-grid + .filter-field {
  margin-top: var(--space-sm);
}
.filter-grid .filter-field + .filter-field {
  margin-top: 0;
}
.filter-field > text {
  display: block;
  margin-bottom: var(--space-xs);
  color: var(--color-ink-2);
  font-size: var(--text-sm);
}
.picker {
  min-height: 48px;
  padding: 0 var(--space-sm);
  border: var(--rule-hairline);
  border-radius: var(--radius-input);
  background: var(--color-paper-2);
  display: flex;
  align-items: center;
  font-size: var(--text-sm);
}
.reset {
  width: auto;
  min-height: 44px;
  padding: 0 var(--space-sm);
  border-radius: var(--radius-input);
  color: var(--color-muted);
  font-size: var(--text-sm);
  font-weight: 600;
  white-space: nowrap;
}
.reset-row {
  margin-top: var(--space-md);
  padding-top: var(--space-xs);
  border-top: var(--rule-hairline);
  display: flex;
  align-items: center;
  justify-content: flex-end;
}
.reset:active {
  background: var(--color-paper-2);
  transform: none;
}
@media (hover: hover) and (pointer: fine) {
  .reset:hover {
    background: var(--color-paper-2);
    color: var(--color-ink-2);
    transform: none;
  }
}
</style>
