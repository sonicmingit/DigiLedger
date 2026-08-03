<template>
  <view
    v-if="priceOpen"
    class="sheet-backdrop"
    @click.self="closePrice"
  >
    <view class="action-sheet price-sheet">
      <view class="sheet-handle" />
      <view class="sheet-header">
        <view>
          <text class="sheet-title">更新关注价格</text>
          <text class="sheet-subtitle">保存后会加入价格历史</text>
        </view>
        <view class="close touch" aria-label="关闭" @click="closePrice">×</view>
      </view>
      <view class="sheet-body">
        <view class="field">
          <text>当前价格 *</text>
          <view class="money-input">
            <text>¥</text>
            <input
              :value="priceValue"
              type="text"
              inputmode="decimal"
              maxlength="12"
              placeholder="0.00"
              @input="updatePriceInput"
              @blur="formatPriceValue"
            />
          </view>
        </view>
        <view class="price-context">
          <view>
            <text>上次关注</text>
            <text>{{ money(wishlist.currentPrice) }}</text>
          </view>
          <view>
            <text>目标价</text>
            <text>{{ money(wishlist.expectedPrice) }}</text>
          </view>
        </view>
      </view>
      <view class="sheet-footer">
        <button class="sheet-button secondary" @click="closePrice">取消</button>
        <button
          class="sheet-button primary lime"
          :disabled="saving"
          @click="savePrice"
        >
          {{ saving ? "保存中…" : "保存价格" }}
        </button>
      </view>
    </view>
  </view>

  <view
    v-if="purchaseOpen"
    class="sheet-backdrop"
    @click.self="closePurchase"
  >
    <view class="action-sheet purchase-sheet">
      <view class="sheet-handle" />
      <view class="sheet-header">
        <view>
          <text class="sheet-title">标记已购买</text>
          <text class="sheet-subtitle">创建物品与主购买记录</text>
        </view>
        <view class="close touch" aria-label="关闭" @click="closePurchase">×</view>
      </view>

      <scroll-view class="sheet-scroll" scroll-y>
        <view class="sheet-body purchase-body">
          <view class="purchase-summary">
            <image
              v-if="wishlist.imageUrl"
              :src="resolveMediaUrl(wishlist.imageUrl)"
              mode="aspectFill"
            />
            <view v-else class="summary-fallback">{{
              wishlist.name.slice(0, 2)
            }}</view>
            <view>
              <text>{{ wishlist.name }}</text>
              <text>{{
                [wishlist.brandName, wishlist.model].filter(Boolean).join(" · ") ||
                wishlist.categoryName ||
                "未分类"
              }}</text>
            </view>
          </view>

          <view class="field-grid">
            <view class="field">
              <text>购买日期 *</text>
              <input v-model="purchase.purchaseDate" type="date" />
            </view>
            <view class="field">
              <text>购买价格 *</text>
              <view class="money-input">
                <text>¥</text>
                <input
                  :value="purchase.price"
                  type="text"
                  inputmode="decimal"
                  maxlength="12"
                  placeholder="0.00"
                  @input="updatePurchasePrice"
                  @blur="formatPurchasePrice"
                />
              </view>
            </view>
          </view>

          <view class="field">
            <text>购买平台</text>
            <picker
              :range="platformOptions"
              range-key="name"
              @change="pickPlatform"
            >
              <view class="picker-value">
                <text>{{ platformName }}</text>
                <text class="chevron">⌄</text>
              </view>
            </picker>
          </view>

          <view class="field">
            <text>购买链接</text>
            <input
              v-model="purchase.productLink"
              type="text"
              placeholder="粘贴商品或订单链接"
            />
          </view>

          <view class="field">
            <text>购买附件</text>
            <view class="attachment-row">
              <view class="attachment-add touch" @click="chooseAttachments">
                <text class="attachment-plus">＋</text>
                <view>
                  <text>添加凭证</text>
                  <text>拍照或从相册选择</text>
                </view>
              </view>
              <view
                v-for="(attachment, index) in purchase.attachments"
                :key="`${attachment}-${index}`"
                class="attachment-chip"
              >
                <text>附件 {{ index + 1 }}</text>
                <view
                  class="remove-attachment touch"
                  @click="purchase.attachments.splice(index, 1)"
                  >×</view
                >
              </view>
            </view>
          </view>

          <view class="field">
            <text>购买备注</text>
            <textarea
              v-model="purchase.notes"
              maxlength="500"
              placeholder="补充订单、用途或购买说明"
            />
          </view>
        </view>
      </scroll-view>

      <view class="sheet-footer">
        <button class="sheet-button secondary" @click="closePurchase">取消</button>
        <button
          class="sheet-button primary lime"
          :disabled="saving"
          @click="savePurchase"
        >
          {{ saving ? "处理中…" : "确认已购买" }}
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import {
  api,
  type AssetPayload,
  type DictionaryPlatform,
  type Wishlist,
} from "@/services/api";
import { uploadFile } from "@/services/http";
import { resolveMediaUrl } from "@/services/media";

const props = defineProps<{ wishlist: Wishlist }>();
const emit = defineEmits<{
  saved: [];
  purchased: [assetId: number];
}>();

const today = () => new Date().toISOString().slice(0, 10);
const priceOpen = ref(false);
const purchaseOpen = ref(false);
const saving = ref(false);
const priceValue = ref("");
const platforms = ref<DictionaryPlatform[]>([]);
const purchase = reactive({
  purchaseDate: today(),
  price: "",
  platformId: undefined as number | undefined,
  productLink: "",
  attachments: [] as string[],
  notes: "",
});

const platformOptions = computed(() => [
  { id: 0, name: "未选择" },
  ...platforms.value,
]);
const platformName = computed(
  () =>
    platforms.value.find((item) => item.id === purchase.platformId)?.name ||
    "请选择平台",
);

function cleanMoney(value: string) {
  const cleaned = value.replace(/[^\d.]/g, "");
  const point = cleaned.indexOf(".");
  const integer = (point === -1 ? cleaned : cleaned.slice(0, point))
    .replace(/^0+(?=\d)/, "")
    .slice(0, 9);
  if (point === -1) return integer;
  return `${integer || "0"}.${cleaned
    .slice(point + 1)
    .replace(/\./g, "")
    .slice(0, 2)}`;
}
function amount(value: string) {
  const parsed = Number(value || 0);
  return Number.isFinite(parsed) ? parsed : 0;
}
function fixedMoney(value?: number) {
  return value == null ? "" : Number(value).toFixed(2);
}
function money(value?: number) {
  return `¥ ${Number(value || 0).toLocaleString("zh-CN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}
function updatePriceInput(event: any) {
  priceValue.value = cleanMoney(String(event.detail?.value ?? ""));
  return priceValue.value;
}
function formatPriceValue() {
  if (priceValue.value !== "") {
    priceValue.value = amount(priceValue.value).toFixed(2);
  }
}
function updatePurchasePrice(event: any) {
  purchase.price = cleanMoney(String(event.detail?.value ?? ""));
  return purchase.price;
}
function formatPurchasePrice() {
  if (purchase.price !== "") {
    purchase.price = amount(purchase.price).toFixed(2);
  }
}
async function ensurePlatforms() {
  if (platforms.value.length) return;
  platforms.value = await api.platforms().catch(() => []);
}
function openPriceSheet() {
  priceValue.value = fixedMoney(props.wishlist.currentPrice);
  priceOpen.value = true;
}
async function openPurchaseSheet() {
  if (!props.wishlist.categoryId) {
    return uni.showModal({
      title: "需要补充分类",
      content: "标记已购买前，请先编辑心愿并选择分类。",
      showCancel: false,
    });
  }
  Object.assign(purchase, {
    purchaseDate: today(),
    price: fixedMoney(
      props.wishlist.currentPrice ?? props.wishlist.expectedPrice,
    ),
    platformId: undefined,
    productLink: props.wishlist.link || "",
    attachments: [],
    notes: "",
  });
  await ensurePlatforms();
  purchaseOpen.value = true;
}
function closePrice() {
  if (!saving.value) priceOpen.value = false;
}
function closePurchase() {
  if (!saving.value) purchaseOpen.value = false;
}
function pickPlatform(event: any) {
  purchase.platformId =
    platformOptions.value[Number(event.detail.value)]?.id || undefined;
}
async function chooseAttachments() {
  const source = await new Promise<"camera" | "album" | null>((resolve) => {
    uni.showActionSheet({
      itemList: ["拍照", "从相册选择"],
      success: (result) =>
        resolve(result.tapIndex === 0 ? "camera" : "album"),
      fail: () => resolve(null),
    });
  });
  if (!source) return;
  const selected = await uni.chooseImage({
    count: Math.max(1, 3 - purchase.attachments.length),
    sizeType: ["compressed"],
    sourceType: [source],
  });
  uni.showLoading({ title: "上传中" });
  try {
    for (const path of selected.tempFilePaths) {
      const uploaded = await uploadFile(path);
      purchase.attachments.push(uploaded.url);
    }
    uni.showToast({ title: "附件已添加", icon: "success" });
  } catch (error) {
    uni.showToast({ title: (error as Error).message, icon: "none" });
  } finally {
    uni.hideLoading();
  }
}
async function savePrice() {
  if (priceValue.value === "") {
    return uni.showToast({ title: "请填写当前价格", icon: "none" });
  }
  saving.value = true;
  try {
    await api.updateWishlistPrice(props.wishlist.id, amount(priceValue.value));
    priceOpen.value = false;
    emit("saved");
    uni.showToast({ title: "关注价格已更新", icon: "success" });
  } catch (error) {
    uni.showToast({ title: (error as Error).message, icon: "none" });
  } finally {
    saving.value = false;
  }
}
async function savePurchase() {
  if (!props.wishlist.categoryId) return;
  if (!purchase.purchaseDate) {
    return uni.showToast({ title: "请选择购买日期", icon: "none" });
  }
  if (purchase.price === "") {
    return uni.showToast({ title: "请填写购买价格", icon: "none" });
  }
  const payload: AssetPayload = {
    name: props.wishlist.name,
    categoryId: props.wishlist.categoryId,
    brandId: props.wishlist.brandId,
    model: props.wishlist.model,
    status: "使用中",
    purchaseDate: purchase.purchaseDate,
    coverImageUrl: props.wishlist.imageUrl,
    notes: props.wishlist.notes,
    tagIds: props.wishlist.tags?.map((tag) => tag.id),
    purchases: [
      {
        type: "PRIMARY",
        platformId: purchase.platformId,
        price: amount(purchase.price),
        purchaseDate: purchase.purchaseDate,
        quantity: 1,
        productLink: purchase.productLink.trim() || undefined,
        attachments: [...purchase.attachments],
        notes: purchase.notes.trim() || undefined,
      },
    ],
  };
  saving.value = true;
  try {
    const assetId = await api.markPurchased(props.wishlist.id, payload);
    purchaseOpen.value = false;
    emit("purchased", assetId);
    uni.showToast({ title: "已标记为已购买", icon: "success" });
  } catch (error) {
    uni.showToast({ title: (error as Error).message, icon: "none" });
  } finally {
    saving.value = false;
  }
}

defineExpose({
  openPrice: openPriceSheet,
  openPurchase: openPurchaseSheet,
});
</script>

<style scoped>
.sheet-backdrop {
  position: fixed;
  z-index: var(--z-modal);
  inset: 0;
  padding-top: max(var(--space-lg), env(safe-area-inset-top));
  background: var(--color-overlay);
  display: flex;
  align-items: flex-end;
  animation: backdrop-in var(--dur-short) var(--ease-out);
}
.action-sheet {
  width: 100%;
  max-width: var(--app-max);
  max-height: calc(100dvh - max(var(--space-lg), env(safe-area-inset-top)));
  margin: 0 auto;
  overflow: hidden;
  border-radius: var(--radius-card-strong) var(--radius-card-strong) 0 0;
  background: var(--color-paper);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-raised);
  animation: sheet-in var(--dur-long) var(--ease-out);
}
.price-sheet {
  max-height: 540px;
}
.purchase-sheet {
  height: calc(100dvh - max(var(--space-lg), env(safe-area-inset-top)));
}
.sheet-handle {
  width: 36px;
  height: 4px;
  margin: var(--space-xs) auto 0;
  border-radius: var(--radius-pill);
  background: var(--color-rule);
}
.sheet-header {
  flex: 0 0 auto;
  padding: var(--space-sm) var(--space-md);
  border-bottom: var(--rule-hairline);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.sheet-title,
.sheet-subtitle {
  display: block;
}
.sheet-title {
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 700;
  letter-spacing: -0.02em;
}
.sheet-subtitle {
  margin-top: var(--space-2xs);
  color: var(--color-muted);
  font-size: var(--text-xs);
}
.close {
  width: 44px;
  min-height: 44px;
  border-radius: 50%;
  background: var(--color-paper-2);
  font-size: 26px;
  font-weight: 300;
}
.sheet-scroll {
  height: 0;
  min-height: 0;
  flex: 1 1 0;
  overscroll-behavior-y: contain;
}
.sheet-body {
  padding: var(--space-md);
}
.purchase-body {
  padding-bottom: var(--space-lg);
}
.purchase-summary {
  min-height: 72px;
  margin-bottom: var(--space-md);
  padding: var(--space-sm);
  border: var(--rule-hairline);
  border-radius: var(--radius-card);
  background: var(--color-surface);
  display: flex;
  align-items: center;
  gap: var(--space-sm);
}
.purchase-summary image,
.summary-fallback {
  width: 54px;
  height: 54px;
  flex: 0 0 54px;
  border-radius: var(--radius-input);
}
.summary-fallback {
  background: var(--color-accent-soft);
  display: grid;
  place-items: center;
  font-weight: 700;
}
.purchase-summary > view:last-child {
  min-width: 0;
}
.purchase-summary text {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.purchase-summary text:first-child {
  font-weight: 700;
}
.purchase-summary text:last-child {
  margin-top: var(--space-2xs);
  color: var(--color-muted);
  font-size: var(--text-xs);
}
.field,
.field-grid {
  margin-top: var(--space-md);
}
.field:first-child {
  margin-top: 0;
}
.field-grid {
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(0, 1fr);
  gap: var(--space-sm);
}
.field-grid > .field {
  margin-top: 0;
}
.field > text {
  display: block;
  margin-bottom: var(--space-xs);
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  font-weight: 600;
}
.field input,
.picker-value,
.money-input {
  width: 100%;
  min-height: 54px;
  border: var(--rule-hairline);
  border-radius: var(--radius-input);
  background: var(--color-surface);
}
.field input {
  padding: 0 var(--space-md);
}
.field input:focus-within,
.money-input:focus-within {
  border-color: var(--color-focus);
  outline: none;
}
.money-input :deep(uni-input),
.money-input :deep(.uni-input-input),
.money-input :deep(.uni-input-wrapper) {
  outline: none !important;
}
.money-input {
  padding: 0 var(--space-sm);
  display: flex;
  align-items: center;
}
.money-input > text {
  color: var(--color-muted);
  font-size: var(--text-sm);
}
.money-input input {
  min-width: 0;
  min-height: 52px;
  padding: 0 0 0 var(--space-xs);
  border: 0;
  outline: 0;
}
.picker-value {
  padding: 0 var(--space-md);
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.chevron {
  color: var(--color-muted);
  font-size: var(--text-lg);
}
.field textarea {
  width: 100%;
  min-height: 112px;
  padding: var(--space-sm) var(--space-md);
  border: var(--rule-hairline);
  border-radius: var(--radius-input);
  background: var(--color-surface);
  line-height: 1.55;
}
.price-context {
  margin-top: var(--space-md);
  padding: var(--space-sm) var(--space-md);
  border-radius: var(--radius-input);
  background: var(--color-paper-2);
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: var(--space-sm);
}
.price-context view {
  min-width: 0;
}
.price-context text {
  display: block;
}
.price-context text:first-child {
  color: var(--color-muted);
  font-size: var(--text-xs);
}
.price-context text:last-child {
  margin-top: var(--space-2xs);
  font-weight: 700;
}
.attachment-row {
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
}
.attachment-add {
  width: 100%;
  min-height: 72px;
  padding: var(--space-sm);
  border: 1px dashed var(--color-rule-strong);
  border-radius: var(--radius-input);
  background: var(--color-surface);
  justify-content: flex-start;
  gap: var(--space-sm);
}
.attachment-plus {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: var(--color-accent-soft);
  display: grid;
  place-items: center;
  color: var(--color-accent-deep);
  font-size: var(--text-lg);
}
.attachment-add view text {
  display: block;
}
.attachment-add view text:first-child {
  font-weight: 700;
}
.attachment-add view text:last-child {
  margin-top: var(--space-2xs);
  color: var(--color-muted);
  font-size: var(--text-xs);
}
.attachment-chip {
  min-height: 36px;
  padding-left: var(--space-sm);
  border: var(--rule-hairline);
  border-radius: var(--radius-pill);
  background: var(--color-paper-2);
  display: inline-flex;
  align-items: center;
  gap: var(--space-2xs);
  font-size: var(--text-xs);
}
.remove-attachment {
  width: 36px;
  min-height: 36px;
  color: var(--color-danger);
}
.sheet-footer {
  flex: 0 0 auto;
  padding: var(--space-sm) var(--space-md)
    calc(var(--space-sm) + env(safe-area-inset-bottom));
  border-top: var(--rule-hairline);
  background: var(--color-paper);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 140px));
  justify-content: center;
  gap: var(--space-sm);
}
.sheet-button {
  width: 100%;
  min-height: 42px;
  height: 42px;
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
.sheet-button.secondary {
  border: var(--rule-hairline);
  background: var(--color-surface);
  color: var(--color-ink);
}

@keyframes backdrop-in {
  from {
    opacity: 0;
  }
}
@keyframes sheet-in {
  from {
    transform: translateY(24px);
    opacity: 0;
  }
}
</style>
