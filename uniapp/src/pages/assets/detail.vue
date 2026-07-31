<template>
  <view class="page detail"
    ><view v-if="loading" class="loading">加载物品详情…</view
    ><view v-else-if="error" class="error">{{ error }}</view
    ><template v-else-if="asset"
      ><view class="top"
        ><PageBackButton fallback="/pages/assets/home" />
        <text class="title">物品详情</text
        ><view class="more touch" @click="openMenu">更多</view></view
      ><view class="hero"
        ><image
          v-if="asset.coverImageUrl"
          :src="asset.coverImageUrl"
          mode="aspectFill"
        /><text v-else>{{ heroInitials }}</text
        ><view class="status">{{ asset.status }}</view></view
      ><view class="headline"
        ><view
          ><text class="name">{{ asset.name }}</text
          ><text class="meta"
            >{{ asset.categoryName || asset.categoryPath || "未分类" }} ·
            {{ asset.model || "未填写型号" }}</text
          ></view
        ><text class="price">{{ money(asset.totalInvest ?? asset.totalCost) }}</text></view
      ><view v-if="asset.tags?.length" class="tags"
        ><text
          v-for="tag in asset.tags"
          :key="tag.id"
          :style="{ background: tag.color || 'var(--dl-accent-soft)' }"
          >{{ tag.name }}</text
        ></view
      ><view class="metrics"
        ><view class="card"
          ><text>总投入</text><text>{{ money(asset.totalInvest ?? asset.totalCost) }}</text></view
        ><view class="card"
          ><text>日均成本</text><text>{{ money(asset.avgCostPerDay ?? asset.dailyCost) }}</text></view
        ><view class="card"
          ><text>使用天数</text><text>{{ asset.useDays || 0 }} 天</text></view
        ><view class="card"
          ><text>最近净收入</text><text>{{ money(asset.lastNetIncome) }}</text></view
        ></view
      ><text class="section-title">基础信息</text
      ><view class="card info"
        ><view
          ><text>品牌</text
          ><text>{{ asset.brandName || asset.brand?.name || "—" }}</text></view
        ><view
          ><text>型号</text><text>{{ asset.model || "—" }}</text></view
        ><view
          ><text>序列号</text><text>{{ asset.serialNo || "—" }}</text></view
        ><view
          ><text>购买日期</text
          ><text>{{ asset.purchaseDate || primaryPurchase?.purchaseDate || "—" }}</text></view
        ><view
          ><text>购买平台</text
          ><text>{{ primaryPurchase?.platformName || primaryPurchase?.seller || "—" }}</text></view
        ><view
          ><text>保修到期</text
          ><text>{{ asset.warrantyExpireDate || primaryPurchase?.warrantyExpireDate || "—" }}</text></view
        ><view
          ><text>手动使用时间</text
          ><text>{{ manualUseDuration || "—" }}</text></view
        ></view
      ><view v-if="relatedLinks.length" class="link-list card"
        ><text class="label">相关链接</text
        ><view
          v-for="link in relatedLinks"
          :key="`${link.url}-${link.description || ''}`"
          class="link-row touch"
          @click="copyLink(link.url)"
          ><text>{{ link.description || "链接" }}</text
          ><text>复制</text></view
        ></view
      ><view class="card note"
        ><text class="label">备注</text
        ><text>{{ asset.notes || "暂无备注" }}</text></view
      ><view class="records-heading"
        ><view class="records-heading-copy"
          ><text class="section-title">购买记录</text
          ><text>{{ purchaseRecords.length }} 条</text></view
        ><button class="record-add touch" @click="openPurchase">
          ＋ 添加
        </button></view
      ><view v-if="purchaseRecords.length" class="record-list"
        ><view
          v-for="record in purchaseRecords"
          :key="record.id || `${record.type}-${record.purchaseDate}`"
          class="record card"
          ><view class="record-title"
            ><text>{{ purchaseTypeLabel(record.type) }}</text
            ><text>{{ money(record.price) }}</text></view
          ><text class="record-name">{{
            record.type === "PRIMARY" ? asset.name : record.name || "未命名记录"
          }}</text
          ><view class="record-grid"
            ><text>{{ record.purchaseDate || "—" }}</text
            ><text>{{ record.platformName || record.seller || "—" }}</text
            ><text>数量 {{ record.quantity || 1 }}</text
            ><text>运费 {{ money(record.shippingCost) }}</text></view
          ><view v-if="record.productLink || record.attachments?.length" class="record-actions"
            ><text v-if="record.productLink" @click="copyLink(record.productLink)">复制商品链接</text
            ><text v-if="record.attachments?.length" @click="previewAttachments(record.attachments)"
              >查看附件 {{ record.attachments.length }}</text
            ></view
          ><text v-if="record.notes" class="record-note">{{ record.notes }}</text></view
        ></view
      ><view v-else class="empty-inline card">暂无购买记录</view
      ><view class="records-heading"
        ><view class="records-heading-copy"
          ><text class="section-title">出售记录</text
          ><text>{{ saleRecords.length }} 条</text></view
        ><button
          class="record-add touch"
          :disabled="!canStartSale"
          @click="openSale"
        >
          出售向导
        </button></view
      ><view v-if="saleRecords.length" class="record-list"
        ><view v-for="record in saleRecords" :key="record.id" class="record card"
          ><view class="record-title sale-title"
            ><text>{{ record.saleScope === "ASSET" ? "主商品" : "配件" }}</text
            ><text>{{ money(record.netIncome ?? record.salePrice) }}</text></view
          ><text class="record-name">{{ record.saleDate || "—" }}</text
          ><view class="record-grid"
            ><text>售价 {{ money(record.salePrice) }}</text
            ><text>{{ record.platformName || record.buyer || "—" }}</text
            ><text>费用 {{ money(totalSaleFee(record)) }}</text
            ><text>使用 {{ record.useDays || 0 }} 天</text></view
          ><view class="cost-line"
            ><text>日均 {{ money(record.dailyUsageCost) }}</text
            ><text>月均 {{ money(record.monthlyUsageCost) }}</text></view
          ><text
            v-if="record.attachments?.length"
            class="attachment-link"
            @click="previewAttachments(record.attachments)"
            >查看附件 {{ record.attachments.length }}</text
          ><text v-if="record.notes" class="record-note">{{ record.notes }}</text></view
        ></view
      ><view v-else class="empty-inline card">暂无出售记录</view
      ><view class="actions"
        ><button class="primary action-button" @click="edit">编辑物品</button
        ><button class="primary lime action-button" @click="changeStatus">
          变更状态
        </button></view
      ><AssetRecordSheets
        ref="recordSheets"
        :asset-id="asset.id"
        :asset-status="asset.status"
        :purchases="purchaseRecords"
        :sales="saleRecords"
        @saved="load"
      />
      </template
    ></view
  >
</template>
<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import AssetRecordSheets from "@/components/AssetRecordSheets.vue";
import PageBackButton from "@/components/PageBackButton.vue";
import {
  api,
  type Asset,
  type PurchaseRecord,
  type SaleRecord,
} from "@/services/api";
import { categoryPathLabel } from "@/utils/dictionaries";
const id = ref(0),
  asset = ref<Asset>(),
  loading = ref(true),
  error = ref(""),
  recordSheets = ref<{
    openPurchase: () => void;
    openSale: () => void;
  }>();
const heroInitials = computed(() => {
  const last = asset.value?.name.trim().split(/\s+/).pop() || "";
  const latin = last.replace(/[^A-Za-z]/g, "");
  return (latin || asset.value?.name || "--").slice(0, 2).toUpperCase();
});
const primaryPurchase = computed(() =>
    asset.value?.purchases?.find((record) => record.type === "PRIMARY"),
  ),
  purchaseRecords = computed(() =>
    [...(asset.value?.purchases || [])].sort(
      (a, b) =>
        new Date(a.purchaseDate || 0).getTime() -
        new Date(b.purchaseDate || 0).getTime(),
    ),
  ),
  saleRecords = computed(() =>
    [...(asset.value?.sales || [])].sort(
      (a, b) =>
        new Date(a.saleDate || 0).getTime() -
        new Date(b.saleDate || 0).getTime(),
    ),
  ),
  relatedLinks = computed(() => [
    ...(primaryPurchase.value?.productLink
      ? [{ url: primaryPurchase.value.productLink, description: "购买链接" }]
      : []),
    ...(asset.value?.relatedLinks || []),
  ]),
  manualUseDuration = computed(() => {
    const total = Number(asset.value?.manualUseMonths || 0);
    if (!total) return "";
    const years = Math.floor(total / 12),
      months = total % 12;
    return `${years ? `${years} 年` : ""}${months ? `${months} 个月` : ""}`;
  }),
  canStartSale = computed(() => {
    if (!asset.value) return false;
    if (asset.value.status !== "已出售") return true;
    const soldPurchaseIds = new Set(
      saleRecords.value
        .filter((record) => record.saleScope === "ACCESSORY")
        .map((record) => record.purchaseId),
    );
    return purchaseRecords.value.some(
      (record) =>
        record.type === "ACCESSORY" &&
        record.id != null &&
        !soldPurchaseIds.has(record.id),
    );
  });
const money = (n?: number) => `¥ ${Number(n || 0).toLocaleString("zh-CN")}`;
const back = () => uni.navigateBack(),
  edit = () => uni.navigateTo({ url: `/pages/assets/editor?id=${id.value}` });
async function load() {
  try {
    const [detail, categories] = await Promise.all([
      api.asset(id.value),
      api.categories().catch(() => []),
    ]);
    asset.value = {
      ...detail,
      categoryName:
        categoryPathLabel(categories, detail.categoryId) ||
        detail.categoryName,
    };
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
const purchaseTypeLabel = (type: PurchaseRecord["type"]) =>
    type === "PRIMARY" ? "主商品" : type === "ACCESSORY" ? "配件" : "服务",
  totalSaleFee = (record: SaleRecord) =>
    Number(record.fee || 0) +
    Number(record.shippingCost || 0) +
    Number(record.otherCost || 0);
function copyLink(url: string) {
  uni.setClipboardData({ data: url });
}
function previewAttachments(attachments: string[]) {
  const images = attachments.filter((url) =>
    /\.(png|jpe?g|gif|webp)(\?|$)/i.test(url),
  );
  if (images.length)
    return uni.previewImage({ urls: images, current: images[0] });
  uni.showActionSheet({
    itemList: attachments.map((_, index) => `复制附件 ${index + 1} 地址`),
    success: (result) => copyLink(attachments[result.tapIndex]),
  });
}
function changeStatus() {
  uni.showActionSheet({
    itemList: ["使用中", "已闲置", "待出售", "已出售", "已丢弃"],
    success: async (r) => {
      if (!asset.value) return;
      const nextStatus = [
        "使用中",
        "已闲置",
        "待出售",
        "已出售",
        "已丢弃",
      ][r.tapIndex];
      await api.updateAssetStatus(id.value, nextStatus);
      asset.value.status = nextStatus;
    },
  });
}
function openMenu() {
  uni.showActionSheet({
    itemList: ["出售向导", "删除物品"],
    success: (r) => {
      if (r.tapIndex === 0) return openSale();
      if (r.tapIndex === 1) remove();
    },
  });
}
function openPurchase() {
  recordSheets.value?.openPurchase();
}
function openSale() {
  recordSheets.value?.openSale();
}
function remove() {
  uni.showModal({
    title: "删除物品",
    content: "删除后无法恢复，确认继续？",
    confirmColor: "#d33",
    success: async (r) => {
      if (r.confirm) {
        await api.deleteAsset(id.value);
        back();
      }
    },
  });
}
onLoad((q) => {
  id.value = Number(q?.id);
  load();
});
onShow(() => {
  if (asset.value) load();
});
</script>
<style scoped>
.detail {
  padding-bottom: 38px;
}
.top {
  height: 48px;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.back {
  width: 40px;
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
.more {
  margin-left: auto;
  color: var(--dl-text-secondary);
  font-size: 12px;
}
.hero {
  height: 245px;
  position: relative;
  border-radius: 0 0 28px 28px;
  background: var(--dl-accent-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-size: 52px;
  font-weight: 700;
}
.hero > image {
  width: 100%;
  height: 100%;
}
.status {
  position: absolute;
  right: 19px;
  top: 19px;
  min-width: 92px;
  height: 36px;
  padding: 0 16px;
  border-radius: 999px;
  background: var(--dl-black);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.headline {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 26px 0 22px;
}
.name,
.meta {
  display: block;
}
.name {
  font-size: 24px;
  font-weight: 700;
}
.meta {
  font-size: 12px;
  color: var(--dl-text-secondary);
  margin-top: 5px;
}
.price {
  font-size: 18px;
  font-weight: 700;
}
.info {
  padding: 14px 18px;
}
.info view {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 12px;
}
.info view text:first-child {
  color: var(--dl-text-secondary);
}
.note {
  margin-top: 18px;
  padding: 16px 18px;
  font-size: 12px;
}
.tags {
  display: flex;
  flex-wrap: wrap;
  gap: 7px;
  margin-top: -10px;
}
.tags text {
  min-height: 30px;
  padding: 0 11px;
  border-radius: 999px;
  display: flex;
  align-items: center;
  font-size: 10px;
}
.metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
  margin: 18px 0 22px;
}
.metrics .card {
  padding: 14px 16px;
  box-shadow: none;
}
.metrics text {
  display: block;
}
.metrics text:first-child {
  color: var(--dl-text-secondary);
  font-size: 10px;
}
.metrics text:last-child {
  margin-top: 7px;
  font-size: 16px;
  font-weight: 700;
}
.section-title {
  display: block;
  margin: 22px 0 10px;
  font-size: 17px;
  font-weight: 700;
}
.link-list {
  margin-top: 14px;
  padding: 14px 16px;
}
.link-row {
  min-height: 44px;
  justify-content: space-between;
  border-top: 1px solid var(--dl-bg);
  font-size: 11px;
}
.link-row text:last-child {
  color: var(--dl-text-secondary);
}
.records-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
  margin: var(--space-lg) 0 var(--space-sm);
}
.records-heading-copy {
  min-width: 0;
  display: flex;
  align-items: baseline;
  gap: var(--space-xs);
}
.records-heading .section-title {
  margin: 0;
}
.records-heading-copy > text:last-child {
  color: var(--dl-text-secondary);
  font-size: var(--text-xs);
}
.record-add {
  min-height: 36px;
  margin: 0;
  padding: 0 var(--space-sm);
  border: var(--rule-hairline);
  border-radius: var(--radius-pill);
  background: var(--color-surface);
  color: var(--color-ink);
  font-size: var(--text-xs);
  font-weight: 700;
  line-height: 1;
}
.record-add[disabled] {
  opacity: 0.4;
}
.record {
  margin-bottom: 12px;
  padding: 15px 16px;
  box-shadow: none;
}
.record-title {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.record-title text:first-child {
  padding: 4px 8px;
  border-radius: 999px;
  background: var(--dl-accent-soft);
  font-size: 9px;
}
.record-title text:last-child {
  font-size: 15px;
  font-weight: 700;
}
.sale-title text:first-child {
  background: var(--dl-black);
  color: #fff;
}
.record-name {
  display: block;
  margin-top: 10px;
  font-size: 14px;
  font-weight: 700;
}
.record-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 7px 12px;
  margin-top: 9px;
  color: var(--dl-text-secondary);
  font-size: 10px;
}
.record-actions,
.cost-line {
  display: flex;
  flex-wrap: wrap;
  gap: 8px 14px;
  margin-top: 12px;
  color: #4d7212;
  font-size: 10px;
  font-weight: 600;
}
.cost-line {
  color: var(--dl-text-secondary);
}
.attachment-link {
  display: block;
  margin-top: 11px;
  color: #4d7212;
  font-size: 10px;
  font-weight: 600;
}
.record-note {
  display: block;
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px solid var(--dl-bg);
  color: var(--dl-text-secondary);
  font-size: 10px;
  line-height: 1.55;
}
.empty-inline {
  padding: 24px;
  color: var(--dl-muted);
  text-align: center;
  font-size: 11px;
  box-shadow: none;
}
.label {
  display: block;
  color: var(--dl-text-secondary);
  margin-bottom: 5px;
}
.actions {
  display: flex;
  justify-content: center;
  gap: 12px;
  margin-top: 22px;
}
.action-button {
  width: min(42%, 148px);
  min-height: 44px;
  height: 44px;
  margin: 0;
  padding: 0 var(--space-md);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-sm);
  line-height: 1;
  text-align: center;
}
</style>
