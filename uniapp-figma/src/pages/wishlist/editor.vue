<template>
  <view class="page editor">
    <view class="top">
      <view class="back touch" @click="back"><view class="back-icon" /></view>
      <text class="title">{{ id ? "编辑心愿" : "新增心愿" }}</text>
      <view class="save touch" @click="submit">保存</view>
    </view>
    <view class="cover" @click="chooseImage">
      <image v-if="form.imageUrl" :src="form.imageUrl" mode="aspectFill" />
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
    <view class="field-grid">
      <view class="field">
        <text>分类</text>
        <picker :range="categories" range-key="pathLabel" @change="pickCategory">
          <view class="picker">{{ categoryName || "请选择" }}</view>
        </picker>
      </view>
      <view class="field">
        <text>品牌</text>
        <picker :range="brands" range-key="name" @change="pickBrand">
          <view class="picker">{{ brandName || "请选择" }}</view>
        </picker>
      </view>
    </view>
    <view class="field">
      <text>型号</text>
      <input v-model="form.model" placeholder="可选" />
    </view>
    <view class="field-grid">
      <view class="field">
        <text>目标价</text>
        <input v-model="expectedPrice" type="digit" placeholder="0.00" />
      </view>
      <view class="field">
        <text>当前价</text>
        <input v-model="currentPrice" type="digit" placeholder="0.00" />
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
      <view class="tag-list">
        <view
          v-for="tag in tags"
          :key="tag.id"
          class="tag-choice"
          :class="{ selected: form.tagIds.includes(tag.id) }"
          @click="toggleTag(tag.id)"
        >
          {{ tag.name }}
        </view>
      </view>
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
import {
  api,
  type CategoryNode,
  type DictionaryBrand,
  type DictionaryTag,
} from "@/services/api";
import { uploadFile } from "@/services/http";
import { flattenTree } from "@/utils/dictionaries";

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

const categories = computed(() => flattenTree(categoryTree.value)),
  tags = computed(() => flattenTree(tagTree.value)),
  categoryName = computed(
    () => categories.value.find((item) => item.id === form.categoryId)?.pathLabel || "",
  ),
  brandName = computed(
    () => brands.value.find((item) => item.id === form.brandId)?.name || "",
  ),
  priorities = [
    { label: "低", value: 1 },
    { label: "中", value: 2 },
    { label: "高", value: 3 },
  ];

const back = () => uni.navigateBack();
function pickCategory(event: any) {
  form.categoryId = categories.value[Number(event.detail.value)]?.id;
}
function pickBrand(event: any) {
  form.brandId = brands.value[Number(event.detail.value)]?.id;
}
function toggleTag(tagId: number) {
  form.tagIds = form.tagIds.includes(tagId)
    ? form.tagIds.filter((value) => value !== tagId)
    : [...form.tagIds, tagId];
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
  margin-left: 10px;
  font-size: 22px;
  font-weight: 700;
}
.save {
  margin-left: auto;
  color: var(--dl-text-secondary);
  font-size: 13px;
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
  border-radius: 12px;
  background: #fff;
  font-size: 14px;
}
.picker {
  display: flex;
  align-items: center;
}
.field textarea {
  height: 88px;
  padding-top: 14px;
}
.states,
.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}
.states .pill {
  flex: 1;
}
.tag-choice {
  min-height: 36px;
  padding: 0 14px;
  border-radius: 999px;
  background: #fff;
  display: flex;
  align-items: center;
  font-size: 12px;
}
.tag-choice.selected {
  background: var(--dl-lime);
  font-weight: 600;
}
.submit {
  width: 100%;
  margin-top: 34px;
}
</style>

