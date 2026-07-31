<template>
  <view
    v-if="purchaseOpen"
    class="sheet-backdrop"
    role="presentation"
    @click.self="closePurchase"
  >
    <view
      class="record-sheet"
      role="dialog"
      aria-modal="true"
      aria-label="添加购买记录"
    >
      <view class="sheet-handle" aria-hidden="true" />
      <view class="sheet-header">
        <view>
          <text class="sheet-title">添加购买记录</text>
          <text class="sheet-subtitle">仅添加配件或服务</text>
        </view>
        <view
          class="sheet-close touch"
          role="button"
          tabindex="0"
          aria-label="关闭"
          @click="closePurchase"
          @keyup.enter="closePurchase"
          ><view aria-hidden="true"
        /></view>
      </view>

      <scroll-view scroll-y class="sheet-scroll">
        <view class="sheet-body">
          <view class="sheet-field">
            <text>类型</text>
            <view class="choice-row" role="radiogroup" aria-label="购买类型">
              <view
                class="choice touch"
                :class="{ active: purchase.type === 'ACCESSORY' }"
                role="radio"
                tabindex="0"
                :aria-checked="purchase.type === 'ACCESSORY'"
                @click="purchase.type = 'ACCESSORY'"
                @keyup.enter="purchase.type = 'ACCESSORY'"
                >配件</view
              >
              <view
                class="choice touch"
                :class="{ active: purchase.type === 'SERVICE' }"
                role="radio"
                tabindex="0"
                :aria-checked="purchase.type === 'SERVICE'"
                @click="purchase.type = 'SERVICE'"
                @keyup.enter="purchase.type = 'SERVICE'"
                >服务</view
              >
            </view>
          </view>

          <view class="sheet-field">
            <text>名称 *</text>
            <input
              v-model="purchase.name"
              maxlength="200"
              :placeholder="
                purchase.type === 'ACCESSORY' ? '例如：保护壳' : '例如：清洁保养'
              "
            />
          </view>

          <view class="sheet-field">
            <text>购买平台</text>
            <picker
              :range="platformOptions"
              range-key="name"
              @change="pickPurchasePlatform"
            >
              <view class="picker-value">{{
                platformName(purchase.platformId)
              }}</view>
            </picker>
          </view>

          <view class="sheet-field">
            <text>金额 *</text>
            <view class="money-input">
              <text>¥</text>
              <input
                :value="purchase.price"
                type="text"
                inputmode="decimal"
                maxlength="12"
                placeholder="0.00"
                @input="updatePurchaseMoney('price', $event)"
              />
            </view>
          </view>

          <view class="field-grid">
            <view class="sheet-field">
              <text>数量</text>
              <input
                :value="purchase.quantity"
                type="text"
                inputmode="numeric"
                maxlength="3"
                @input="updateQuantity"
              />
            </view>
            <view class="sheet-field">
              <text>购买日期 *</text>
              <input v-model="purchase.purchaseDate" type="date" />
            </view>
          </view>

          <view class="sheet-field">
            <text>商品链接</text>
            <input
              v-model="purchase.productLink"
              type="text"
              inputmode="url"
              maxlength="1000"
              placeholder="粘贴商品或订单链接"
            />
          </view>
          <view class="sheet-field">
            <text>备注</text>
            <textarea
              v-model="purchase.notes"
              maxlength="1000"
              placeholder="补充型号、用途或购买说明"
            />
          </view>
          <view class="sheet-field">
            <text>附件</text>
            <view
              class="attachment-picker touch"
              role="button"
              tabindex="0"
              @click="chooseAttachments('purchase')"
              @keyup.enter="chooseAttachments('purchase')"
            >
              <view class="plus-icon" aria-hidden="true" />
              <view>
                <text>添加图片附件</text>
                <text>拍照或从相册选择</text>
              </view>
            </view>
            <view v-if="purchase.attachments.length" class="attachment-list">
              <view
                v-for="(item, index) in purchase.attachments"
                :key="`${item}-${index}`"
                class="attachment-chip"
              >
                <text>附件 {{ index + 1 }}</text>
                <view
                  role="button"
                  tabindex="0"
                  aria-label="移除附件"
                  @click="purchase.attachments.splice(index, 1)"
                  >×</view
                >
              </view>
            </view>
          </view>
        </view>
      </scroll-view>

      <view class="sheet-footer">
        <button class="sheet-button primary" @click="closePurchase">
          取消
        </button>
        <button
          class="sheet-button primary lime"
          :disabled="saving"
          :loading="saving"
          @click="savePurchase"
        >
          保存记录
        </button>
      </view>
    </view>
  </view>

  <view
    v-if="saleOpen"
    class="sheet-backdrop"
    role="presentation"
    @click.self="closeSale"
  >
    <view
      class="record-sheet"
      role="dialog"
      aria-modal="true"
      aria-label="出售向导"
    >
      <view class="sheet-handle" aria-hidden="true" />
      <view class="sheet-header">
        <view>
          <text class="sheet-title">出售向导</text>
          <text class="sheet-subtitle">记录成交与相关费用</text>
        </view>
        <view
          class="sheet-close touch"
          role="button"
          tabindex="0"
          aria-label="关闭"
          @click="closeSale"
          @keyup.enter="closeSale"
          ><view aria-hidden="true"
        /></view>
      </view>

      <scroll-view scroll-y class="sheet-scroll">
        <view class="sheet-body">
          <view class="sheet-field">
            <text>出售范围</text>
            <view class="choice-row" role="radiogroup" aria-label="出售范围">
              <view
                class="choice touch"
                :class="{ active: sale.saleScope === 'ASSET' }"
                :aria-disabled="!canSellMain"
                role="radio"
                tabindex="0"
                :aria-checked="sale.saleScope === 'ASSET'"
                @click="selectSaleScope('ASSET')"
                @keyup.enter="selectSaleScope('ASSET')"
                >主商品</view
              >
              <view
                class="choice touch"
                :class="{ active: sale.saleScope === 'ACCESSORY' }"
                :aria-disabled="!availableAccessories.length"
                role="radio"
                tabindex="0"
                :aria-checked="sale.saleScope === 'ACCESSORY'"
                @click="selectSaleScope('ACCESSORY')"
                @keyup.enter="selectSaleScope('ACCESSORY')"
                >配件</view
              >
            </view>
          </view>

          <view v-if="sale.saleScope === 'ACCESSORY'" class="sheet-field">
            <text>关联配件 *</text>
            <picker
              :range="availableAccessories"
              range-key="name"
              @change="pickAccessory"
            >
              <view class="picker-value">{{
                selectedAccessoryName || "请选择要出售的配件"
              }}</view>
            </picker>
          </view>

          <view class="field-grid">
            <view class="sheet-field">
              <text>出售金额 *</text>
              <view class="money-input">
                <text>¥</text>
                <input
                  :value="sale.salePrice"
                  type="text"
                  inputmode="decimal"
                  maxlength="12"
                  placeholder="0.00"
                  @input="updateSaleMoney('salePrice', $event)"
                />
              </view>
            </view>
            <view class="sheet-field">
              <text>出售日期 *</text>
              <input v-model="sale.saleDate" type="date" />
            </view>
          </view>

          <view class="sheet-field">
            <text>出售平台</text>
            <picker
              :range="platformOptions"
              range-key="name"
              @change="pickSalePlatform"
            >
              <view class="picker-value">{{
                platformName(sale.platformId)
              }}</view>
            </picker>
          </view>
          <view class="sheet-field">
            <text>买家</text>
            <input
              v-model="sale.buyer"
              maxlength="200"
              placeholder="姓名、昵称或回收商"
            />
          </view>

          <view class="field-grid fees-grid">
            <view class="sheet-field">
              <text>手续费</text>
              <view class="money-input compact">
                <text>¥</text>
                <input
                  :value="sale.fee"
                  type="text"
                  inputmode="decimal"
                  maxlength="12"
                  placeholder="0"
                  @input="updateSaleMoney('fee', $event)"
                />
              </view>
            </view>
            <view class="sheet-field">
              <text>运费</text>
              <view class="money-input compact">
                <text>¥</text>
                <input
                  :value="sale.shippingCost"
                  type="text"
                  inputmode="decimal"
                  maxlength="12"
                  placeholder="0"
                  @input="updateSaleMoney('shippingCost', $event)"
                />
              </view>
            </view>
            <view class="sheet-field">
              <text>其他费用</text>
              <view class="money-input compact">
                <text>¥</text>
                <input
                  :value="sale.otherCost"
                  type="text"
                  inputmode="decimal"
                  maxlength="12"
                  placeholder="0"
                  @input="updateSaleMoney('otherCost', $event)"
                />
              </view>
            </view>
          </view>

          <view class="net-preview">
            <text>预计净收入</text>
            <text>{{ money(saleNetIncome) }}</text>
          </view>

          <view class="sheet-field">
            <text>备注</text>
            <textarea
              v-model="sale.notes"
              maxlength="1000"
              placeholder="补充成交情况或物品状态"
            />
          </view>
          <view class="sheet-field">
            <text>附件</text>
            <view
              class="attachment-picker touch"
              role="button"
              tabindex="0"
              @click="chooseAttachments('sale')"
              @keyup.enter="chooseAttachments('sale')"
            >
              <view class="plus-icon" aria-hidden="true" />
              <view>
                <text>添加图片附件</text>
                <text>拍照或从相册选择</text>
              </view>
            </view>
            <view v-if="sale.attachments.length" class="attachment-list">
              <view
                v-for="(item, index) in sale.attachments"
                :key="`${item}-${index}`"
                class="attachment-chip"
              >
                <text>附件 {{ index + 1 }}</text>
                <view
                  role="button"
                  tabindex="0"
                  aria-label="移除附件"
                  @click="sale.attachments.splice(index, 1)"
                  >×</view
                >
              </view>
            </view>
          </view>
        </view>
      </scroll-view>

      <view class="sheet-footer">
        <button class="sheet-button primary" @click="closeSale">
          取消
        </button>
        <button
          class="sheet-button primary lime"
          :disabled="saving"
          :loading="saving"
          @click="saveSale"
        >
          保存出售
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import {
  api,
  type DictionaryPlatform,
  type PurchaseCreatePayload,
  type PurchaseRecord,
  type SaleRecord,
  type SellPayload,
} from "@/services/api";
import { uploadFile } from "@/services/http";

const props = defineProps<{
  assetId: number;
  assetStatus?: string;
  purchases: PurchaseRecord[];
  sales: SaleRecord[];
}>();
const emit = defineEmits(["saved"]);

type PurchaseMoneyField = "price";
type SaleMoneyField = "salePrice" | "fee" | "shippingCost" | "otherCost";

const today = () => new Date().toISOString().slice(0, 10);
const platforms = ref<DictionaryPlatform[]>([]);
const purchaseOpen = ref(false);
const saleOpen = ref(false);
const saving = ref(false);

const blankPurchase = () => ({
  type: "ACCESSORY" as "ACCESSORY" | "SERVICE",
  name: "",
  platformId: undefined as number | undefined,
  price: "",
  quantity: "1",
  purchaseDate: today(),
  productLink: "",
  attachments: [] as string[],
  notes: "",
});
const blankSale = (scope: "ASSET" | "ACCESSORY" = "ASSET") => ({
  saleScope: scope,
  purchaseId: undefined as number | undefined,
  platformId: undefined as number | undefined,
  buyer: "",
  salePrice: "",
  fee: "",
  shippingCost: "",
  otherCost: "",
  saleDate: today(),
  attachments: [] as string[],
  notes: "",
});

const purchase = reactive(blankPurchase());
const sale = reactive(blankSale());

const platformOptions = computed(() => [
    { id: 0, name: "未选择" },
    ...platforms.value,
  ]),
  soldAccessoryIds = computed(
    () =>
      new Set(
        props.sales
          .filter(
            (record) =>
              record.saleScope === "ACCESSORY" && record.purchaseId != null,
          )
          .map((record) => record.purchaseId as number),
      ),
  ),
  availableAccessories = computed(() =>
    props.purchases
      .filter(
        (record) =>
          record.type === "ACCESSORY" &&
          record.id != null &&
          !soldAccessoryIds.value.has(record.id),
      )
      .map((record) => ({
        id: record.id as number,
        name: record.name || "未命名配件",
      })),
  ),
  canSellMain = computed(() => props.assetStatus !== "已出售"),
  canOpenSale = computed(
    () => canSellMain.value || availableAccessories.value.length > 0,
  ),
  selectedAccessoryName = computed(
    () =>
      availableAccessories.value.find((item) => item.id === sale.purchaseId)
        ?.name || "",
  ),
  saleNetIncome = computed(
    () =>
      amount(sale.salePrice) -
      amount(sale.fee) -
      amount(sale.shippingCost) -
      amount(sale.otherCost),
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
function money(value: number) {
  return `¥ ${Number(value || 0).toLocaleString("zh-CN", {
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  })}`;
}
function platformName(platformId?: number) {
  return (
    platforms.value.find((item) => item.id === platformId)?.name ||
    "请选择平台"
  );
}
async function ensurePlatforms() {
  if (platforms.value.length) return;
  platforms.value = await api.platforms().catch(() => []);
}
async function openPurchaseSheet() {
  Object.assign(purchase, blankPurchase());
  await ensurePlatforms();
  purchaseOpen.value = true;
}
async function openSaleSheet() {
  if (!canOpenSale.value) {
    return uni.showToast({ title: "当前没有可出售的商品", icon: "none" });
  }
  const scope = canSellMain.value ? "ASSET" : "ACCESSORY";
  Object.assign(sale, blankSale(scope), {
    purchaseId:
      scope === "ACCESSORY" ? availableAccessories.value[0]?.id : undefined,
  });
  await ensurePlatforms();
  saleOpen.value = true;
}
function closePurchase() {
  if (!saving.value) purchaseOpen.value = false;
}
function closeSale() {
  if (!saving.value) saleOpen.value = false;
}
function updatePurchaseMoney(field: PurchaseMoneyField, event: any) {
  purchase[field] = cleanMoney(String(event.detail?.value ?? ""));
  return purchase[field];
}
function updateSaleMoney(field: SaleMoneyField, event: any) {
  sale[field] = cleanMoney(String(event.detail?.value ?? ""));
  return sale[field];
}
function updateQuantity(event: any) {
  purchase.quantity =
    String(event.detail?.value ?? "")
      .replace(/\D/g, "")
      .slice(0, 3) || "1";
  return purchase.quantity;
}
function pickPurchasePlatform(event: any) {
  purchase.platformId =
    platformOptions.value[Number(event.detail.value)]?.id || undefined;
}
function pickSalePlatform(event: any) {
  sale.platformId =
    platformOptions.value[Number(event.detail.value)]?.id || undefined;
}
function pickAccessory(event: any) {
  sale.purchaseId =
    availableAccessories.value[Number(event.detail.value)]?.id;
}
function selectSaleScope(scope: "ASSET" | "ACCESSORY") {
  if (scope === "ASSET" && !canSellMain.value) {
    return uni.showToast({ title: "主商品已出售", icon: "none" });
  }
  if (scope === "ACCESSORY" && !availableAccessories.value.length) {
    return uni.showToast({ title: "没有可出售的配件", icon: "none" });
  }
  sale.saleScope = scope;
  sale.purchaseId =
    scope === "ACCESSORY" ? availableAccessories.value[0]?.id : undefined;
}
async function chooseAttachments(target: "purchase" | "sale") {
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
    count: 3,
    sizeType: ["compressed"],
    sourceType: [source],
  });
  uni.showLoading({ title: "上传中" });
  try {
    const urls: string[] = [];
    for (const path of selected.tempFilePaths) {
      const uploaded = await uploadFile(path);
      urls.push(uploaded.url);
    }
    (target === "purchase" ? purchase.attachments : sale.attachments).push(
      ...urls,
    );
    uni.showToast({ title: `已添加 ${urls.length} 个附件`, icon: "success" });
  } catch (error) {
    uni.showToast({ title: (error as Error).message, icon: "none" });
  } finally {
    uni.hideLoading();
  }
}
async function savePurchase() {
  if (!purchase.name.trim()) {
    return uni.showToast({ title: "请填写名称", icon: "none" });
  }
  if (!purchase.purchaseDate) {
    return uni.showToast({ title: "请选择购买日期", icon: "none" });
  }
  if (!purchase.price) {
    return uni.showToast({ title: "请填写购买金额", icon: "none" });
  }
  const payload: PurchaseCreatePayload = {
    assetId: props.assetId,
    type: purchase.type,
    name: purchase.name.trim(),
    platformId: purchase.platformId,
    price: amount(purchase.price),
    quantity: Math.max(1, Number(purchase.quantity || 1)),
    purchaseDate: purchase.purchaseDate,
    productLink: purchase.productLink.trim() || undefined,
    attachments: [...purchase.attachments],
    notes: purchase.notes.trim() || undefined,
  };
  saving.value = true;
  try {
    await api.createPurchase(payload);
    purchaseOpen.value = false;
    emit("saved");
    uni.showToast({ title: "购买记录已添加", icon: "success" });
  } catch (error) {
    uni.showToast({ title: (error as Error).message, icon: "none" });
  } finally {
    saving.value = false;
  }
}
async function saveSale() {
  if (!sale.salePrice) {
    return uni.showToast({ title: "请填写出售金额", icon: "none" });
  }
  if (!sale.saleDate) {
    return uni.showToast({ title: "请选择出售日期", icon: "none" });
  }
  if (sale.saleScope === "ACCESSORY" && !sale.purchaseId) {
    return uni.showToast({ title: "请选择要出售的配件", icon: "none" });
  }
  const payload: SellPayload = {
    platformId: sale.platformId,
    saleScope: sale.saleScope,
    purchaseId:
      sale.saleScope === "ACCESSORY" ? sale.purchaseId : undefined,
    buyer: sale.buyer.trim() || undefined,
    salePrice: amount(sale.salePrice),
    fee: amount(sale.fee),
    shippingCost: amount(sale.shippingCost),
    otherCost: amount(sale.otherCost),
    saleDate: sale.saleDate,
    attachments: [...sale.attachments],
    notes: sale.notes.trim() || undefined,
  };
  saving.value = true;
  try {
    await api.sellAsset(props.assetId, payload);
    saleOpen.value = false;
    emit("saved");
    uni.showToast({ title: "出售记录已保存", icon: "success" });
  } catch (error) {
    uni.showToast({ title: (error as Error).message, icon: "none" });
  } finally {
    saving.value = false;
  }
}

defineExpose({
  openPurchase: openPurchaseSheet,
  openSale: openSaleSheet,
  canOpenSale,
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
.record-sheet {
  width: 100%;
  max-width: var(--app-max);
  height: calc(
    100vh - max(var(--space-lg), env(safe-area-inset-top))
  );
  height: calc(
    100dvh - max(var(--space-lg), env(safe-area-inset-top))
  );
  max-height: calc(100vh - max(var(--space-lg), env(safe-area-inset-top)));
  margin: 0 auto;
  overflow: hidden;
  border-radius: var(--radius-card-strong) var(--radius-card-strong) 0 0;
  background: var(--color-paper);
  display: flex;
  flex-direction: column;
  box-shadow: var(--shadow-raised);
  animation: sheet-in var(--dur-long) var(--ease-out);
}
.sheet-handle {
  width: 36px;
  height: 4px;
  margin: var(--space-xs) auto 0;
  border-radius: var(--radius-pill);
  background: var(--color-rule);
}
.sheet-header {
  padding: var(--space-sm) var(--space-md);
  border-bottom: var(--rule-hairline);
  background: var(--color-paper);
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
  font-size: var(--text-md);
  font-weight: 700;
  letter-spacing: -0.03em;
}
.sheet-subtitle {
  margin-top: var(--space-2xs);
  color: var(--color-muted);
  font-size: var(--text-xs);
}
.sheet-close {
  width: 40px;
  min-height: 40px;
  border-radius: 50%;
  background: var(--color-paper-2);
}
.sheet-close > view,
.sheet-close > view::after {
  width: 14px;
  height: 1.5px;
  border-radius: var(--radius-pill);
  background: var(--color-ink);
}
.sheet-close > view {
  position: relative;
  transform: rotate(45deg);
}
.sheet-close > view::after {
  content: "";
  position: absolute;
  transform: rotate(90deg);
}
.sheet-scroll {
  height: 0;
  min-height: 0;
  flex: 1 1 0;
  overscroll-behavior-y: contain;
}
.sheet-body {
  padding: 0 var(--space-md) var(--space-lg);
}
.sheet-field {
  min-width: 0;
  margin-top: var(--space-md);
}
.sheet-field > text {
  display: block;
  margin-bottom: var(--space-xs);
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  font-weight: 600;
}
.sheet-field input,
.picker-value,
.sheet-field textarea,
.money-input {
  width: 100%;
  min-height: 48px;
  border: var(--rule-hairline);
  outline: 2px solid transparent;
  outline-offset: 1px;
  border-radius: var(--radius-input);
  background: var(--color-surface);
  font-size: var(--text-base);
}
.sheet-field input,
.picker-value {
  padding: 0 var(--space-sm);
}
.picker-value {
  display: flex;
  align-items: center;
}
.sheet-field input:focus-within,
.sheet-field textarea:focus-within,
.money-input:focus-within {
  border-color: var(--color-focus);
  outline-color: var(--color-focus);
}
.sheet-field textarea {
  min-height: 92px;
  padding: var(--space-sm);
}
.field-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 0 var(--space-xs);
}
.fees-grid {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}
.choice-row {
  padding: var(--space-3xs);
  border: var(--rule-hairline);
  border-radius: var(--radius-pill);
  background: var(--color-paper-2);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-3xs);
}
.choice {
  min-height: 38px;
  border-radius: var(--radius-pill);
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  font-weight: 700;
}
.choice.active {
  background: var(--color-ink);
  color: var(--color-surface);
}
.choice[aria-disabled="true"] {
  opacity: 0.35;
}
.money-input {
  padding-left: var(--space-sm);
  display: flex;
  align-items: center;
}
.money-input > text {
  flex: 0 0 auto;
  color: var(--color-muted);
  font-size: var(--text-sm);
}
.money-input input {
  min-width: 0;
  flex: 1;
  min-height: 46px;
  padding-left: var(--space-xs);
  border: 0;
  outline: 0;
  background: transparent;
}
.money-input input:focus-within {
  border-color: transparent;
  outline-color: transparent;
}
.money-input input::selection {
  background: var(--color-cyan-soft);
  color: var(--color-ink);
}
.record-sheet :deep(.uni-input-input:focus-visible) {
  outline: none;
}
.fees-grid .sheet-field > text {
  font-size: var(--text-xs);
}
.fees-grid .money-input {
  padding-left: var(--space-xs);
}
.fees-grid .money-input input {
  padding: 0 var(--space-xs);
  font-size: var(--text-sm);
}
.net-preview {
  margin-top: var(--space-md);
  padding: var(--space-sm);
  border-radius: var(--radius-input);
  background: var(--color-accent-soft);
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: var(--text-sm);
}
.net-preview text:last-child {
  font-weight: 700;
}
.attachment-picker {
  min-height: 72px;
  padding: var(--space-sm);
  border: 1px dashed var(--color-muted);
  border-radius: var(--radius-input);
  background: var(--color-surface);
  justify-content: flex-start;
  gap: var(--space-sm);
}
.plus-icon {
  width: 32px;
  height: 32px;
  flex: 0 0 32px;
  position: relative;
  border-radius: 50%;
  background: var(--color-accent-soft);
}
.plus-icon::before,
.plus-icon::after {
  content: "";
  position: absolute;
  top: 50%;
  left: 50%;
  width: 14px;
  height: 2px;
  border-radius: var(--radius-pill);
  background: var(--color-ink-2);
  transform: translate(-50%, -50%);
}
.plus-icon::after {
  transform: translate(-50%, -50%) rotate(90deg);
}
.attachment-picker > view:last-child text {
  display: block;
}
.attachment-picker > view:last-child text:first-child {
  font-size: var(--text-sm);
  font-weight: 700;
}
.attachment-picker > view:last-child text:last-child {
  margin-top: var(--space-2xs);
  color: var(--color-muted);
  font-size: var(--text-xs);
}
.attachment-list {
  margin-top: var(--space-xs);
  display: flex;
  flex-wrap: wrap;
  gap: var(--space-xs);
}
.attachment-chip {
  min-height: 32px;
  padding: 0 var(--space-2xs) 0 var(--space-sm);
  border-radius: var(--radius-pill);
  background: var(--color-paper-2);
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  font-size: var(--text-xs);
}
.attachment-chip > view {
  width: 28px;
  min-height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
}
.sheet-footer {
  padding: var(--space-sm) var(--space-md)
    calc(var(--space-sm) + env(safe-area-inset-bottom));
  border-top: var(--rule-hairline);
  background: var(--color-paper);
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-xs);
}
.sheet-button {
  min-height: 44px;
  margin: 0;
  border-radius: var(--radius-pill);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-sm);
  font-weight: 700;
  line-height: 1;
  text-align: center;
}
@keyframes backdrop-in {
  from {
    opacity: 0;
  }
}
@keyframes sheet-in {
  from {
    transform: translateY(32px);
    opacity: 0;
  }
}
</style>
