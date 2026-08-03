<template>
  <view class="page editor">
    <view class="top">
      <PageBackButton fallback="/pages/assets/home" />
      <text class="title">{{ id ? "编辑物品" : "新增物品" }}</text>
    </view>
    <view
      class="cover touch"
      role="button"
      tabindex="0"
      aria-label="添加或更换封面图"
      @click="chooseImage"
      @keyup.enter="chooseImage"
      @keyup.space="chooseImage"
      ><image
        v-if="form.coverImageUrl"
        :src="resolveMediaUrl(form.coverImageUrl)"
        mode="aspectFill"
      /><template v-else
        ><image
          class="image-icon"
          src="/static/icons/image.svg"
          aria-hidden="true"
        /><text
          class="cover-title"
          >添加封面图</text
        ><text class="hint">拍照或从相册选择</text></template
      ><text v-if="form.coverImageUrl" class="cover-action">更换封面</text
      ></view
    ><text class="section-title">基本资料</text
    ><view class="field"
      ><text>物品名称 *</text
      ><input v-model="form.name" placeholder="请输入物品名称" /></view
    ><view class="field"
      ><text>分类 *</text
      ><TreeSelect
        :model-value="form.categoryId || undefined"
        :nodes="categoryTree"
        placeholder="请选择分类"
        clear-label="清除分类"
        search-placeholder="搜索分类"
        @update:model-value="setCategory"
      /></view
    ><view class="field"
      ><text>品牌</text
      ><picker :range="brands" range-key="name" @change="pickBrand"
        ><view class="picker">{{ brandName || "请选择品牌" }}</view></picker
      ></view
    ><view class="field"
      ><text>型号</text
      ><input v-model="form.model" placeholder="例如 WH-1000XM5" /></view
    ><text class="section-title">购买信息</text
    ><view class="field"
      ><text>购买日期</text
      ><input
        :value="form.purchaseDate"
        type="date"
        aria-label="购买日期"
        @input="form.purchaseDate = $event.detail.value"
      /></view
    ><view class="field"
      ><text>购买金额</text
      ><input
        :value="cost"
        type="text"
        inputmode="decimal"
        maxlength="12"
        placeholder="0.00"
        @input="onCostInput"
        @blur="normalizeCost"
      /></view
    ><view class="field"
      ><text>购买平台</text
      ><picker
        :range="platformOptions"
        range-key="name"
        @change="pickPurchasePlatform"
        ><view class="picker">{{ purchasePlatformName }}</view></picker
      ></view
    ><view class="warranty-grid"
      ><view class="field"
        ><text>质保时间</text
        ><view class="unit-input"
          ><input
            :value="warrantyMonths"
            type="text"
            inputmode="numeric"
            maxlength="3"
            placeholder="0"
            aria-label="质保时间（月）"
            @input="onWarrantyInput"
          /><text>个月</text></view
        ></view
      ><view class="field"
        ><text>质保到期</text
        ><view class="computed-date" aria-live="polite">{{
          warrantyExpireDate || "—"
        }}</view></view
      ></view
    ><view class="field"
      ><text>购买链接</text
      ><input
        v-model="purchaseLink"
        type="text"
        inputmode="url"
        maxlength="1000"
        placeholder="粘贴商品页面链接"
      /></view
    ><text class="section-title">状态与记录</text
    ><view class="field"
      ><text>标签</text
      ><TreeSelect
        :model-value="form.tagIds || []"
        :nodes="tagTree"
        multiple
        placeholder="请选择标签"
        clear-label="清除标签"
        search-placeholder="搜索标签"
        @update:model-value="setTags"
      /></view
    ><view class="field"
      ><text>当前状态</text
      ><view class="states"
        ><view
          v-for="s in states"
          :key="s"
          class="pill"
          :class="{ active: form.status === s }"
          role="button"
          tabindex="0"
          :aria-pressed="form.status === s"
          @click="form.status = s"
          @keyup.enter="form.status = s"
          @keyup.space="form.status = s"
          >{{ s }}</view
        ></view
      ></view
    ><view class="field"
      ><text>备注</text
      ><textarea v-model="form.notes" placeholder="记录保管位置或使用情况" /></view
    ><view class="save-bar"
      ><button
        class="primary lime submit"
        :loading="saving"
        :disabled="saving"
        @click="submit"
      >
        保存物品</button
      ></view
    ></view
  >
</template>
<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import TreeSelect from "@/components/TreeSelect.vue";
import PageBackButton from "@/components/PageBackButton.vue";
import {
  api,
  type AssetPayload,
  type CategoryNode,
  type DictionaryBrand,
  type DictionaryPlatform,
  type DictionaryTag,
  type PurchaseRecord,
} from "@/services/api";
import { uploadFile } from "@/services/http";
import { resolveMediaUrl } from "@/services/media";
const id = ref(0),
  saving = ref(false),
  cost = ref(""),
  warrantyMonths = ref(""),
  purchaseLink = ref(""),
  purchasePlatformId = ref<number>(),
  categoryTree = ref<CategoryNode[]>([]),
  brands = ref<DictionaryBrand[]>([]),
  platforms = ref<DictionaryPlatform[]>([]),
  tagTree = ref<DictionaryTag[]>([]);
const existingPurchases = ref<PurchaseRecord[]>([]);
const today = new Date().toISOString().slice(0, 10);
const form = reactive<AssetPayload>({
  name: "",
  categoryId: 0,
  status: "使用中",
  purchaseDate: today,
  coverImageUrl: "",
  model: "",
  notes: "",
  tagIds: [],
});
const states = computed(() =>
    id.value
      ? ["使用中", "已闲置", "待出售", "已出售", "已丢弃"]
      : ["使用中", "已闲置", "待出售"],
  );
const brandName = computed(
    () => brands.value.find((x) => x.id === form.brandId)?.name || "",
  ),
  platformOptions = computed(() => [
    { id: 0, name: "未选择" },
    ...platforms.value,
  ]),
  purchasePlatformName = computed(
    () =>
      platforms.value.find((item) => item.id === purchasePlatformId.value)
        ?.name || "请选择平台",
  ),
  warrantyExpireDate = computed(() =>
    calculateWarrantyExpireDate(form.purchaseDate || "", warrantyMonths.value),
  );
function setCategory(value: number | number[] | undefined) {
  form.categoryId = typeof value === "number" ? value : 0;
}
function pickBrand(e: any) {
  form.brandId = brands.value[Number(e.detail.value)]?.id;
}
function pickPurchasePlatform(e: any) {
  purchasePlatformId.value =
    platformOptions.value[Number(e.detail.value)]?.id || undefined;
}
function setTags(value: number | number[] | undefined) {
  form.tagIds = Array.isArray(value) ? value : [];
}
function cleanMoney(value: string) {
  const cleaned = value.replace(/[^\d.]/g, "");
  const point = cleaned.indexOf(".");
  const integer = (point === -1 ? cleaned : cleaned.slice(0, point))
    .replace(/^0+(?=\d)/, "")
    .slice(0, 9);
  if (point === -1) return integer;
  const decimal = cleaned.slice(point + 1).replace(/\./g, "").slice(0, 2);
  return `${integer || "0"}.${decimal}`;
}
function onCostInput(event: any) {
  cost.value = cleanMoney(String(event.detail?.value ?? ""));
  return cost.value;
}
function normalizeCost() {
  cost.value = cleanMoney(cost.value);
}
function onWarrantyInput(event: any) {
  warrantyMonths.value = String(event.detail?.value ?? "")
    .replace(/\D/g, "")
    .slice(0, 3);
  return warrantyMonths.value;
}
function calculateWarrantyExpireDate(date: string, monthsValue: string) {
  const months = Number(monthsValue);
  const matched = /^(\d{4})-(\d{2})-(\d{2})$/.exec(date);
  if (!matched || !Number.isInteger(months) || months <= 0) return "";
  const year = Number(matched[1]);
  const monthIndex = Number(matched[2]) - 1;
  const day = Number(matched[3]);
  const targetStart = new Date(Date.UTC(year, monthIndex + months, 1));
  const targetYear = targetStart.getUTCFullYear();
  const targetMonth = targetStart.getUTCMonth();
  const lastDay = new Date(
    Date.UTC(targetYear, targetMonth + 1, 0),
  ).getUTCDate();
  return `${targetYear}-${String(targetMonth + 1).padStart(2, "0")}-${String(
    Math.min(day, lastDay),
  ).padStart(2, "0")}`;
}
function inferWarrantyMonths(startDate: string, expireDate: string) {
  const start = /^(\d{4})-(\d{2})-(\d{2})$/.exec(startDate);
  const expire = /^(\d{4})-(\d{2})-(\d{2})$/.exec(expireDate);
  if (!start || !expire) return "";
  const months =
    (Number(expire[1]) - Number(start[1])) * 12 +
    Number(expire[2]) -
    Number(start[2]);
  return months > 0 ? String(months) : "";
}
function back() {
  uni.navigateBack();
}
async function chooseImage() {
  const source = await new Promise<"camera" | "album" | null>((resolve) => {
    uni.showActionSheet({
      itemList: ["拍照", "从相册选择"],
      success: (result) => resolve(result.tapIndex === 0 ? "camera" : "album"),
      fail: () => resolve(null),
    });
  });
  if (!source) return;
  const r = await uni.chooseImage({
    count: 1,
    sizeType: ["compressed"],
    sourceType: [source],
  });
  uni.showLoading({ title: "上传中" });
  try {
    const out = await uploadFile(r.tempFilePaths[0]);
    form.coverImageUrl = out.url;
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: "none" });
  } finally {
    uni.hideLoading();
  }
}
async function submit() {
  if (!form.name.trim() || !form.categoryId)
    return uni.showToast({ title: "请填写名称并选择分类", icon: "none" });
  saving.value = true;
  const amount = Number(cost.value || 0);
  const originalPrimary = existingPurchases.value.find(
    (purchase) => purchase.type === "PRIMARY",
  );
  const serializePurchase = (
    purchase: PurchaseRecord,
  ): NonNullable<AssetPayload["purchases"]>[number] => ({
    type: purchase.type,
    name: purchase.name || undefined,
    platformId: purchase.platformId,
    seller: purchase.seller || undefined,
    price: Number(purchase.price || 0),
    shippingCost: purchase.shippingCost,
    purchaseDate: purchase.purchaseDate,
    quantity: purchase.quantity || 1,
    warrantyMonths: purchase.warrantyMonths,
    warrantyExpireDate: purchase.warrantyExpireDate || undefined,
    productLink: purchase.productLink || undefined,
    attachments: purchase.attachments,
    notes: purchase.notes || undefined,
  });
  const primaryPurchase: NonNullable<AssetPayload["purchases"]>[number] = {
    ...(originalPrimary
      ? serializePurchase(originalPrimary)
      : {
          type: "PRIMARY",
          price: 0,
          purchaseDate: form.purchaseDate || today,
          quantity: 1,
        }),
    platformId: purchasePlatformId.value,
    price: amount,
    purchaseDate: form.purchaseDate || today,
    warrantyMonths: warrantyMonths.value
      ? Number(warrantyMonths.value)
      : undefined,
    warrantyExpireDate: warrantyExpireDate.value || undefined,
    productLink: purchaseLink.value.trim() || undefined,
  };
  const payload: AssetPayload = {
    ...form,
    targetCostValue: amount,
    targetCostStrategy: "CUSTOM",
    purchases: [
      primaryPurchase,
      ...existingPurchases.value
        .filter((purchase) => purchase.type !== "PRIMARY")
        .map(serializePurchase),
    ],
  };
  try {
    id.value
      ? await api.updateAsset(id.value, payload)
      : await api.createAsset(payload);
    uni.showToast({ title: "保存成功", icon: "success" });
    setTimeout(back, 500);
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: "none" });
  } finally {
    saving.value = false;
  }
}
onLoad(async (q) => {
  id.value = Number(q?.id || 0);
  const [categoryRows, brandRows, tagRows, platformRows] = await Promise.all([
    api.categories().catch(() => []),
    api.brands().catch(() => []),
    api.tags().catch(() => []),
    api.platforms().catch(() => []),
  ]);
  categoryTree.value = categoryRows;
  brands.value = brandRows;
  tagTree.value = tagRows;
  platforms.value = platformRows;
  if (id.value) {
    const a = await api.asset(id.value);
    existingPurchases.value = a.purchases || [];
    const primaryPurchase = existingPurchases.value.find(
      (purchase) => purchase.type === "PRIMARY",
    );
    Object.assign(form, a, {
      categoryId: a.categoryId || 0,
      brandId: a.brandId || a.brand?.id,
      purchaseDate: primaryPurchase?.purchaseDate || a.purchaseDate || today,
      tagIds: a.tags?.map((tag) => tag.id) || [],
    });
    cost.value = String(
      primaryPurchase?.price ?? a.totalInvest ?? a.totalCost ?? "",
    );
    purchasePlatformId.value = primaryPurchase?.platformId;
    warrantyMonths.value = String(
      primaryPurchase?.warrantyMonths ||
        inferWarrantyMonths(
          primaryPurchase?.purchaseDate || a.purchaseDate || today,
          primaryPurchase?.warrantyExpireDate || a.warrantyExpireDate || "",
        ),
    );
    purchaseLink.value = primaryPurchase?.productLink || "";
  }
});
</script>
<style scoped>
/* Hallmark · task-first maintenance form */
.editor {
  padding-bottom: calc(112px + env(safe-area-inset-bottom));
}
.top {
  height: 48px;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: var(--space-sm);
}
.back {
  width: 44px;
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
  font-family: var(--font-display);
  font-size: var(--text-lg);
  font-weight: 700;
  margin-left: 0;
  letter-spacing: -0.025em;
}
.cover {
  height: 132px;
  position: relative;
  border: 1px solid var(--color-accent-deep);
  border-radius: var(--radius-card-strong);
  background: var(--color-accent-soft);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.cover > image:not(.image-icon) {
  width: 100%;
  height: 100%;
}
.image-icon {
  width: 24px;
  height: 24px;
}
.cover-title {
  font-weight: 700;
  margin-top: var(--space-xs);
}
.hint {
  margin-top: var(--space-2xs);
  color: var(--color-ink-2);
  font-size: var(--text-xs);
}
.cover-action {
  position: absolute;
  right: var(--space-sm);
  bottom: var(--space-sm);
  min-height: 36px;
  padding: 0 var(--space-sm);
  border-radius: var(--radius-pill);
  background: var(--color-overlay);
  color: var(--color-paper);
  display: flex;
  align-items: center;
  font-size: var(--text-xs);
  font-weight: 700;
  white-space: nowrap;
}
.section-title {
  display: block;
  margin-top: var(--space-lg);
  padding-bottom: var(--space-xs);
  border-bottom: var(--rule-hairline);
  color: var(--color-ink);
  font-family: var(--font-display);
  font-size: var(--text-base);
  font-weight: 700;
}
.field {
  margin-top: var(--space-md);
}
.warranty-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-xs);
}
.warranty-grid .field {
  min-width: 0;
}
.field > text {
  display: block;
  margin-bottom: var(--space-xs);
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  font-weight: 500;
}
.field input,
.picker,
.field textarea {
  width: 100%;
  min-height: 48px;
  padding: 0 var(--space-md);
  border: var(--rule-hairline);
  outline: 2px solid transparent;
  outline-offset: 1px;
  border-radius: var(--radius-input);
  background: var(--color-surface);
  font-size: var(--text-base);
  transition: background-color var(--dur-short) var(--ease-out);
}
.picker {
  display: flex;
  align-items: center;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.field input::selection {
  background: var(--color-cyan-soft);
  color: var(--color-ink);
}
.field input:focus-within {
  border-color: var(--color-focus);
  outline-color: var(--color-focus);
}
.editor :deep(.uni-input-input:focus-visible) {
  outline: none;
}
.unit-input {
  min-height: 48px;
  border: var(--rule-hairline);
  outline: 2px solid transparent;
  outline-offset: 1px;
  border-radius: var(--radius-input);
  background: var(--color-surface);
  display: flex;
  align-items: center;
  overflow: hidden;
}
.unit-input input {
  min-width: 0;
  min-height: 46px;
  flex: 1;
  padding: 0 0 0 var(--space-md);
  border: 0;
  outline: 0;
  border-radius: 0;
  background: transparent;
}
.unit-input input:focus-within {
  border-color: transparent;
  outline-color: transparent;
}
.unit-input > text {
  flex: 0 0 auto;
  padding: 0 var(--space-sm);
  color: var(--color-muted);
  font-size: var(--text-sm);
  white-space: nowrap;
}
.unit-input:focus-within {
  border-color: var(--color-focus);
  outline-color: var(--color-focus);
}
.computed-date {
  min-height: 48px;
  padding: 0 var(--space-sm);
  border: var(--rule-hairline);
  border-radius: var(--radius-input);
  background: var(--color-paper-2);
  color: var(--color-ink-2);
  display: flex;
  align-items: center;
  overflow: hidden;
  font-size: var(--text-sm);
  font-variant-numeric: tabular-nums;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.field textarea {
  min-height: 104px;
  padding-top: var(--space-sm);
  line-height: 1.55;
  resize: vertical;
}
.states {
  display: flex;
  gap: var(--space-xs);
  overflow-x: auto;
}
.states .pill {
  flex: 0 0 auto;
  min-width: 72px;
}
.save-bar {
  position: fixed;
  z-index: var(--z-sticky);
  left: 50%;
  right: auto;
  width: min(calc(100% - (var(--space-xl) * 2)), 296px);
  bottom: calc(var(--space-sm) + env(safe-area-inset-bottom));
  padding: 0;
  border: 0;
  background: transparent;
  box-shadow: none;
  transform: translateX(-50%);
}
.submit {
  width: 100%;
  min-height: 44px;
  height: 44px;
  margin: 0;
  padding: 0 var(--space-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-sm);
  line-height: 1;
  text-align: center;
}
@media (hover: hover) and (pointer: fine) {
  .field input:hover,
  .picker:hover,
  .field textarea:hover {
    background: var(--color-paper-2);
  }
}
</style>
