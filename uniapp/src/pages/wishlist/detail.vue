<template>
  <view class="page detail-page">
    <view class="top">
      <PageBackButton fallback="/pages/wishlist/index" />
      <text class="title">心愿详情</text>
      <view class="top-actions">
        <button class="top-action edit-button" @click="edit">编辑</button>
        <button class="top-action delete-top-button" @click="remove">
          删除
        </button>
      </view>
    </view>
    <view v-if="loading" class="loading">加载心愿…</view>
    <view v-else-if="error" class="error">{{ error }}</view>
    <template v-else-if="item">
      <view class="hero">
        <image v-if="item.imageUrl" :src="item.imageUrl" mode="aspectFill" />
        <text v-else>{{ item.name.slice(0, 2) }}</text>
        <view class="status">{{ item.status || "未购买" }}</view>
      </view>
      <view class="headline">
        <view>
          <text class="name">{{ item.name }}</text>
          <text class="meta">{{ [item.brandName, item.model].filter(Boolean).join(" · ") || item.categoryName || "未分类" }}</text>
        </view>
        <text class="price">{{ money(item.currentPrice || item.expectedPrice) }}</text>
      </view>
      <view v-if="item.tags?.length" class="tags">
        <text
          v-for="tag in item.tags"
          :key="tag.id"
          :style="{ background: tag.color || 'var(--dl-accent-soft)' }"
        >
          {{ tag.name }}
        </text>
      </view>
      <view class="card info">
        <view><text>目标价</text><text>{{ money(item.expectedPrice) }}</text></view>
        <view><text>关注价格</text><text>{{ money(item.currentPrice) }}</text></view>
        <view><text>价格更新时间</text><text>{{ formatDate(item.lastPriceAt) }}</text></view>
        <view><text>优先级</text><text>{{ priorityLabel(item.priority) }}</text></view>
        <view><text>来源</text><text>{{ item.source || "—" }}</text></view>
        <view><text>创建时间</text><text>{{ item.createdAt?.slice(0, 10) || "—" }}</text></view>
        <view v-if="item.status === '已购买'"
          ><text>购买时间</text><text>{{ formatDate(item.purchasedAt) }}</text></view
        >
        <view v-if="item.status === '已购买'"
          ><text>实际购买价</text><text>{{ money(item.purchasedPrice) }}</text></view
        >
        <view v-if="item.status === '已购买'"
          ><text>购买价差</text><text>{{ signedMoney(item.purchasePriceDiff) }}</text></view
        >
      </view>
      <view class="history-heading">
        <text>价格历史</text><text>{{ history.length }} 条</text>
      </view>
      <view v-if="history.length" class="history card">
        <view v-for="point in history" :key="`${point.capturedAt}-${point.price}`">
          <text>{{ formatDate(point.capturedAt) }}</text>
          <text>{{ money(point.price) }}</text>
        </view>
      </view>
      <view v-else class="empty-history card">暂无价格记录</view>
      <view v-if="item.link" class="card link-card" @click="copyLink">
        <text>商品链接</text><text>{{ item.link }}</text>
      </view>
      <view class="card note">
        <text>备注</text><text>{{ item.notes || "暂无备注" }}</text>
      </view>
      <button
        v-if="item.convertedAssetId"
        class="action-button converted-button"
        @click="openConvertedAsset"
      >
        查看已购物品
      </button>
      <view v-if="item.status !== '已购买'" class="primary-actions">
        <button class="action-button price-button" @click="openPrice">
          更新关注价格
        </button>
        <button class="action-button purchase-button" @click="openPurchase">
          标记已购买
        </button>
      </view>
      <WishlistActionSheets
        ref="actionSheets"
        :wishlist="item"
        @saved="load"
        @purchased="openPurchasedAsset"
      />
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import { api, type PricePoint, type Wishlist } from "@/services/api";
import WishlistActionSheets from "@/components/WishlistActionSheets.vue";
import PageBackButton from "@/components/PageBackButton.vue";

const id = ref(0),
  item = ref<Wishlist>(),
  history = ref<PricePoint[]>([]),
  actionSheets = ref<InstanceType<typeof WishlistActionSheets>>(),
  loading = ref(true),
  error = ref("");
const money = (value?: number) =>
    `¥ ${Number(value || 0).toLocaleString("zh-CN", { maximumFractionDigits: 2 })}`,
  signedMoney = (value?: number) =>
    value == null
      ? "—"
      : `${value > 0 ? "+" : ""}${money(value)}`,
  formatDate = (value?: string) => value?.slice(0, 10) || "—",
  priorityLabel = (priority?: number) =>
    priority === 3 ? "高" : priority === 1 ? "低" : "中",
  back = () => uni.navigateBack(),
  edit = () => uni.navigateTo({ url: `/pages/wishlist/editor?id=${id.value}` });
const openPrice = () => actionSheets.value?.openPrice();
const openPurchase = () => actionSheets.value?.openPurchase();
async function load() {
  loading.value = true;
  try {
    const [detail, priceRows] = await Promise.all([
      api.wishlistDetail(id.value),
      api.wishlistPriceHistory(id.value).catch(() => []),
    ]);
    item.value = detail;
    history.value = [...priceRows].sort(
      (a, b) =>
        new Date(b.capturedAt || 0).getTime() -
        new Date(a.capturedAt || 0).getTime(),
    );
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
function copyLink() {
  if (!item.value?.link) return;
  uni.setClipboardData({ data: item.value.link });
}
function openConvertedAsset() {
  if (!item.value?.convertedAssetId) return;
  uni.navigateTo({
    url: `/pages/assets/detail?id=${item.value.convertedAssetId}`,
  });
}
function openPurchasedAsset(assetId: number) {
  uni.redirectTo({ url: `/pages/assets/detail?id=${assetId}` });
}
function remove() {
  uni.showModal({
    title: "删除心愿",
    content: "确认删除这条心愿？",
    confirmColor: "#c33",
    success: async (result) => {
      if (!result.confirm) return;
      await api.deleteWishlist(id.value);
      back();
    },
  });
}
onLoad((query) => {
  id.value = Number(query?.id || 0);
  load();
});
onShow(() => {
  if (item.value) load();
});
</script>

<style scoped>
.detail-page {
  padding-bottom: 40px;
}
.top {
  height: 48px;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.back {
  width: 38px;
  justify-content: flex-start;
}
.back-icon {
  width: 10px;
  height: 10px;
  border-left: 2px solid var(--dl-text);
  border-bottom: 2px solid var(--dl-text);
  transform: rotate(45deg);
}
.title {
  min-width: 0;
  font-size: 22px;
  font-weight: 700;
}
.top-actions {
  margin-left: auto;
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}
.top-action {
  width: 56px;
  min-height: 36px;
  height: 36px;
  margin: 0;
  padding: 0 var(--space-sm);
  border: var(--rule-hairline);
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  color: var(--color-ink);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-sm);
  font-weight: 700;
  line-height: 1;
  text-align: center;
}
.delete-top-button {
  color: var(--color-danger);
}
.hero {
  height: 245px;
  position: relative;
  overflow: hidden;
  border-radius: 0 0 28px 28px;
  background: var(--dl-accent-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 48px;
  font-weight: 700;
}
.hero image {
  width: 100%;
  height: 100%;
}
.status {
  position: absolute;
  top: 18px;
  right: 18px;
  padding: 0 16px;
  min-height: 36px;
  border-radius: 999px;
  background: var(--dl-black);
  color: #fff;
  display: flex;
  align-items: center;
  font-size: 11px;
}
.headline {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 24px 0 20px;
}
.name,
.meta {
  display: block;
}
.name {
  font-size: 23px;
  font-weight: 700;
}
.meta {
  margin-top: 5px;
  color: var(--dl-text-secondary);
  font-size: 12px;
}
.price {
  font-size: 18px;
  font-weight: 700;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin: -8px 0 16px;
}
.tags text {
  min-height: 30px;
  padding: 0 11px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  font-size: 10px;
}
.info,
.note,
.link-card {
  padding: 15px 18px;
}
.history-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 22px 0 10px;
}
.history-heading text:first-child {
  font-size: 17px;
  font-weight: 700;
}
.history-heading text:last-child {
  color: var(--dl-text-secondary);
  font-size: 10px;
}
.history {
  padding: 7px 16px;
  box-shadow: none;
}
.history view {
  min-height: 48px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid var(--dl-bg);
  font-size: 12px;
}
.history view:last-child {
  border-bottom: 0;
}
.history view text:first-child {
  color: var(--dl-text-secondary);
}
.history view text:last-child {
  font-weight: 700;
}
.empty-history {
  padding: 24px;
  color: var(--dl-muted);
  text-align: center;
  font-size: 11px;
  box-shadow: none;
}
.info view {
  display: flex;
  justify-content: space-between;
  padding: 7px 0;
  font-size: 12px;
}
.info view text:first-child,
.note text:first-child,
.link-card text:first-child {
  color: var(--dl-text-secondary);
}
.note,
.link-card {
  margin-top: 14px;
  font-size: 12px;
}
.note text,
.link-card text {
  display: block;
}
.note text:last-child,
.link-card text:last-child {
  margin-top: 6px;
  overflow-wrap: anywhere;
  line-height: 1.55;
}
.primary-actions {
  margin-top: var(--space-lg);
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--space-sm);
}
.action-button {
  min-height: 44px;
  height: 44px;
  margin: 0;
  padding: 0 var(--space-sm);
  border-radius: var(--radius-pill);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-sm);
  font-weight: 700;
  line-height: 1;
  text-align: center;
}
.converted-button {
  border: var(--rule-hairline);
  background: var(--color-surface);
  color: var(--color-ink);
}
.price-button,
.purchase-button {
  border: 1px solid var(--color-accent-deep);
  background: var(--color-accent);
  color: var(--color-accent-ink);
  box-shadow: 0 3px 0 var(--color-accent-deep);
}
.converted-button {
  width: min(100%, 296px);
  margin: var(--space-lg) auto 0;
}
</style>
