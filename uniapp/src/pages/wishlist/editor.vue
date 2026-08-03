<template>
  <view class="page editor">
    <view class="top">
      <PageBackButton fallback="/pages/wishlist/index" />
      <text class="title">{{ id ? "编辑心愿" : "新增心愿" }}</text>
      <button
        class="save-button"
        :disabled="saving"
        @click="submit"
      >
        {{ saving ? "保存中" : "保存" }}
      </button>
    </view>
    <view class="cover" @click="chooseImage">
      <image v-if="form.imageUrl" :src="resolveMediaUrl(form.imageUrl)" mode="aspectFill" />
      <template v-else>
        <image class="image-icon" src="/static/icons/image.svg" />
        <text class="cover-title">添加图片</text>
        <text class="hint">拍照或从相册选择</text>
      </template>
    </view>
    <view class="field">
      <text>名称 *</text>
      <input v-model="form.name" placeholder="请输入心愿名称" />
    </view>
    <view class="field">
      <text>分类</text>
      <TreeSelect
        :model-value="form.categoryId"
        :nodes="categoryTree"
        placeholder="请选择分类"
        clear-label="清除分类"
        search-placeholder="搜索分类"
        @update:model-value="setCategory"
      />
    </view>
    <view class="field">
      <text>品牌</text>
      <picker :range="brands" range-key="name" @change="pickBrand">
        <view class="picker">{{ brandName || "请选择" }}</view>
      </picker>
    </view>
    <view class="field">
      <text>型号</text>
      <input v-model="form.model" placeholder="可选" />
    </view>
    <view class="field-grid">
      <view class="field">
        <text>目标价</text>
        <input
          :value="expectedPrice"
          type="text"
          inputmode="decimal"
          maxlength="12"
          placeholder="0.00"
          @input="onPriceInput('expectedPrice', $event)"
        />
      </view>
      <view class="field">
        <text>当前价</text>
        <input
          :value="currentPrice"
          type="text"
          inputmode="decimal"
          maxlength="12"
          placeholder="0.00"
          @input="onPriceInput('currentPrice', $event)"
        />
      </view>
    </view>
    <view class="field">
      <text>优先级</text>
      <view class="states">
        <view
          v-for="item in priorities"
          :key="item.value"
          class="pill"
          :class="{ active: form.priority === item.value }"
          @click="form.priority = item.value"
        >
          {{ item.label }}
        </view>
      </view>
    </view>
    <view class="field">
      <text>来源</text>
      <input v-model="form.source" placeholder="例如评测、朋友推荐" />
    </view>
    <view class="field">
      <text>商品链接</text>
      <input v-model="form.link" placeholder="https://" />
    </view>
    <view class="field">
      <text>标签</text>
      <TreeSelect
        :model-value="form.tagIds"
        :nodes="tagTree"
        multiple
        placeholder="请选择标签"
        clear-label="清除标签"
        search-placeholder="搜索标签"
        @update:model-value="setTags"
      />
    </view>
    <view class="field">
      <text>备注</text>
      <textarea v-model="form.notes" placeholder="简单记录购买理由" />
    </view>
    <button class="primary submit" :loading="saving" :disabled="saving" @click="submit">
      保存心愿
    </button>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import TreeSelect from "@/components/TreeSelect.vue";
import PageBackButton from "@/components/PageBackButton.vue";
import {
  api,
  type CategoryNode,
  type DictionaryBrand,
  type DictionaryTag,
} from "@/services/api";
import { uploadFile } from "@/services/http";
import { resolveMediaUrl } from "@/services/media";

const id = ref(0),
  saving = ref(false),
  categoryTree = ref<CategoryNode[]>([]),
  brands = ref<DictionaryBrand[]>([]),
  tagTree = ref<DictionaryTag[]>([]),
  expectedPrice = ref(""),
  currentPrice = ref(""),
  form = reactive({
    name: "",
    categoryId: undefined as number | undefined,
    brandId: undefined as number | undefined,
    model: "",
    priority: 2,
    source: "",
    link: "",
    notes: "",
    imageUrl: "",
    tagIds: [] as number[],
  });

const brandName = computed(
    () => brands.value.find((item) => item.id === form.brandId)?.name || "",
  ),
  priorities = [
    { label: "低", value: 1 },
    { label: "中", value: 2 },
    { label: "高", value: 3 },
  ];

const back = () => uni.navigateBack();
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
function onPriceInput(
  field: "expectedPrice" | "currentPrice",
  event: any,
) {
  const value = cleanMoney(String(event.detail?.value ?? ""));
  if (field === "expectedPrice") expectedPrice.value = value;
  else currentPrice.value = value;
  return value;
}
function setCategory(value: number | number[] | undefined) {
  form.categoryId = typeof value === "number" ? value : undefined;
}
function pickBrand(event: any) {
  form.brandId = brands.value[Number(event.detail.value)]?.id;
}
function setTags(value: number | number[] | undefined) {
  form.tagIds = Array.isArray(value) ? value : [];
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
  const result = await uni.chooseImage({
    count: 1,
    sizeType: ["compressed"],
    sourceType: [source],
  });
  uni.showLoading({ title: "上传中" });
  try {
    const uploaded = await uploadFile(result.tempFilePaths[0]);
    form.imageUrl = uploaded.url;
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: "none" });
  } finally {
    uni.hideLoading();
  }
}
async function submit() {
  if (!form.name.trim())
    return uni.showToast({ title: "请输入心愿名称", icon: "none" });
  saving.value = true;
  const payload = {
    ...form,
    name: form.name.trim(),
    expectedPrice: Number(expectedPrice.value || 0),
    currentPrice: Number(currentPrice.value || 0),
  };
  try {
    id.value
      ? await api.updateWishlist(id.value, payload)
      : await api.createWishlist(payload);
    uni.showToast({ title: "已保存", icon: "success" });
    setTimeout(back, 450);
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: "none" });
  } finally {
    saving.value = false;
  }
}
onLoad(async (query) => {
  id.value = Number(query?.id || 0);
  const [categoryRows, brandRows, tagRows] = await Promise.all([
    api.categories().catch(() => []),
    api.brands().catch(() => []),
    api.tags().catch(() => []),
  ]);
  categoryTree.value = categoryRows;
  brands.value = brandRows;
  tagTree.value = tagRows;
  if (!id.value) return;
  const item = await api.wishlistDetail(id.value);
  Object.assign(form, {
    name: item.name,
    categoryId: item.categoryId,
    brandId: item.brandId,
    model: item.model || "",
    priority: item.priority || 2,
    source: item.source || "",
    link: item.link || "",
    notes: item.notes || "",
    imageUrl: item.imageUrl || "",
    tagIds: item.tags?.map((tag) => tag.id) || [],
  });
  expectedPrice.value = String(item.expectedPrice || "");
  currentPrice.value = String(item.currentPrice || "");
});
</script>

<style scoped>
.editor {
  padding-bottom: 42px;
}
.top {
  height: 48px;
  display: flex;
  align-items: center;
  gap: var(--space-sm);
  margin-bottom: 10px;
}
.back {
  width: 30px;
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
  margin-left: 0;
  font-size: 22px;
  font-weight: 700;
}
.save-button {
  width: 62px;
  min-height: 36px;
  height: 36px;
  margin: 0 0 0 auto;
  padding: 0 14px;
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
.save-button[disabled] {
  color: var(--color-muted);
  opacity: 0.6;
}
.cover {
  height: 130px;
  overflow: hidden;
  border-radius: 28px;
  background: var(--dl-accent-soft);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
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
  margin-top: 8px;
  font-weight: 700;
}
.hint {
  margin-top: 5px;
  color: var(--dl-text-secondary);
  font-size: 11px;
}
.field,
.field-grid {
  margin-top: 18px;
}
.field-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 10px;
}
.field-grid .field {
  margin-top: 0;
}
.field > text {
  display: block;
  margin-bottom: 7px;
  color: var(--dl-text-secondary);
  font-size: 13px;
}
.field input,
.field textarea,
.picker {
  width: 100%;
  min-height: 48px;
  padding: 0 16px;
  border: var(--rule-hairline);
  outline: 2px solid transparent;
  outline-offset: 1px;
  border-radius: 12px;
  background: #fff;
  font-size: 14px;
}
.field input:focus-within {
  border-color: var(--color-focus);
  outline-color: var(--color-focus);
}
.field input::selection {
  background: var(--color-cyan-soft);
  color: var(--color-ink);
}
.editor :deep(.uni-input-input:focus-visible) {
  outline: none;
}
.picker {
  display: flex;
  align-items: center;
}
.field textarea {
  height: 88px;
  padding-top: 14px;
}
.states {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.states .pill {
  flex: 1;
}
.submit {
  width: min(calc(100% - (var(--space-xl) * 2)), 296px);
  min-height: 44px;
  height: 44px;
  margin: 34px auto 0;
  padding: 0 var(--space-lg);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-sm);
  line-height: 1;
  text-align: center;
}
</style>
