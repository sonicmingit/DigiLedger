<template>
  <view class="page dictionary-page">
    <view class="topbar">
      <PageBackButton fallback="/pages/settings/index" />
      <view>
        <text class="page-title">{{ config.title }}</text>
        <text class="page-subtitle">{{ items.length }} 项</text>
      </view>
      <button class="add-button touch" @click="openEditor()">新增</button>
    </view>
    <view v-if="loading" class="loading">加载中…</view>
    <view v-else-if="error" class="error" @click="load">{{ error }}，点击重试</view>
    <view v-else-if="!items.length" class="empty card">暂无内容</view>
    <view v-else class="dictionary-list card">
      <view
        v-for="item in items"
        :key="item.id"
        class="dictionary-row touch"
        @click="openEditor(item)"
      >
        <view
          v-if="type === 'tags'"
          class="tag-dot"
          :style="{ background: item.color || 'var(--dl-lime)' }"
        />
        <view v-else class="item-icon">
          <image :src="config.icon" mode="aspectFit" />
        </view>
        <view class="item-copy" :style="{ paddingLeft: `${item.depth * 12}px` }">
          <text>{{ item.name }}</text>
          <text>{{ itemSubtitle(item) }}</text>
        </view>
        <text class="edit-label">编辑</text>
      </view>
    </view>

    <view v-if="editorOpen" class="sheet-mask" @click="closeEditor">
      <view class="editor-sheet" @click.stop>
        <view class="sheet-heading">
          <text>{{ editingId ? `编辑${config.shortTitle}` : `新增${config.shortTitle}` }}</text>
          <text class="touch" @click="closeEditor">关闭</text>
        </view>
        <view class="field">
          <text>名称 *</text>
          <input v-model="form.name" maxlength="40" placeholder="请输入名称" />
        </view>
        <view v-if="type !== 'brands'" class="field">
          <text>上级</text>
          <TreeSelect
            :model-value="form.parentId || undefined"
            :nodes="parentTree"
            placeholder="无上级"
            clear-label="无上级"
            :search-placeholder="`搜索${config.shortTitle}`"
            @update:model-value="setParent"
          />
        </view>
        <template v-if="type === 'brands'">
          <view class="field">
            <text>别名</text>
            <input v-model="form.alias" maxlength="80" placeholder="可选" />
          </view>
          <view class="field">
            <text>首字母</text>
            <input v-model="form.initial" maxlength="8" placeholder="例如 S" />
          </view>
        </template>
        <view v-if="type === 'tags'" class="field">
          <text>颜色</text>
          <input v-model="form.color" maxlength="16" placeholder="#B7FF3C" />
        </view>
        <view class="sheet-actions">
          <button
            v-if="editingId"
            class="delete-button"
            :disabled="saving"
            @click="remove"
          >
            删除
          </button>
          <button class="primary" :loading="saving" :disabled="saving" @click="saveItem">
            保存
          </button>
        </view>
      </view>
    </view>
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
import { flattenTree } from "@/utils/dictionaries";

type DictionaryType = "categories" | "brands" | "tags";
type DisplayItem = (CategoryNode | DictionaryBrand | DictionaryTag) & {
  pathLabel: string;
  depth: number;
};

const type = ref<DictionaryType>("categories"),
  rawItems = ref<Array<CategoryNode | DictionaryBrand | DictionaryTag>>([]),
  loading = ref(false),
  error = ref(""),
  editorOpen = ref(false),
  editingId = ref(0),
  saving = ref(false),
  form = reactive({
    name: "",
    parentId: null as number | null,
    alias: "",
    initial: "",
    color: "#B7FF3C",
    sort: 0,
  });

const configs = {
    categories: {
      title: "物品分类",
      shortTitle: "分类",
      icon: "/static/icons/folder.svg",
    },
    brands: {
      title: "品牌管理",
      shortTitle: "品牌",
      icon: "/static/icons/tag.svg",
    },
    tags: {
      title: "标签管理",
      shortTitle: "标签",
      icon: "/static/icons/tag.svg",
    },
  },
  config = computed(() => configs[type.value]),
  items = computed<DisplayItem[]>(() => {
    if (type.value === "brands")
      return (rawItems.value as DictionaryBrand[]).map((item) => ({
        ...item,
        pathLabel: item.name,
        depth: 0,
      }));
    return flattenTree(
      rawItems.value as Array<CategoryNode | DictionaryTag>,
    ) as DisplayItem[];
  }),
  forbiddenParentIds = computed(() => {
    const result = new Set<number>();
    const visit = (nodes: Array<CategoryNode | DictionaryTag>, insideTarget = false) => {
      for (const node of nodes) {
        const isTarget = node.id === editingId.value;
        if (isTarget || insideTarget) result.add(node.id);
        visit(node.children || [], isTarget || insideTarget);
      }
    };
    if (type.value !== "brands" && editingId.value)
      visit(rawItems.value as Array<CategoryNode | DictionaryTag>);
    return result;
  }),
  parentTree = computed(() => {
    const filterNodes = (
      nodes: Array<CategoryNode | DictionaryTag>,
    ): Array<CategoryNode | DictionaryTag> =>
      nodes
        .filter((node) => !forbiddenParentIds.value.has(node.id))
        .map((node) => ({
          ...node,
          children: filterNodes(node.children || []),
        }));
    return filterNodes(
      rawItems.value as Array<CategoryNode | DictionaryTag>,
    );
  });

function itemSubtitle(item: DisplayItem) {
  if (type.value === "brands")
    return [item.alias, item.initial].filter(Boolean).join(" · ") || "品牌";
  return item.parentId ? item.pathLabel : "顶级";
}
function openEditor(item?: DisplayItem) {
  editingId.value = item?.id || 0;
  Object.assign(form, {
    name: item?.name || "",
    parentId: "parentId" in (item || {}) ? (item as any).parentId || null : null,
    alias: "alias" in (item || {}) ? (item as any).alias || "" : "",
    initial: "initial" in (item || {}) ? (item as any).initial || "" : "",
    color: "color" in (item || {}) ? (item as any).color || "#B7FF3C" : "#B7FF3C",
    sort: item?.sort || 0,
  });
  editorOpen.value = true;
}
function closeEditor() {
  editorOpen.value = false;
}
function setParent(value: number | number[] | undefined) {
  form.parentId = typeof value === "number" ? value : null;
}

/**
 * 三类字典共用同一移动编辑器，但按后端契约只提交各自字段，
 * 防止把品牌别名等无关属性写入树形分类或标签。
 */
async function saveItem() {
  if (!form.name.trim())
    return uni.showToast({ title: "请输入名称", icon: "none" });
  saving.value = true;
  try {
    if (type.value === "categories") {
      const payload = {
        name: form.name.trim(),
        parentId: form.parentId,
        sort: form.sort,
      };
      editingId.value
        ? await api.updateCategory(editingId.value, payload)
        : await api.createCategory(payload);
    } else if (type.value === "brands") {
      const payload = {
        name: form.name.trim(),
        alias: form.alias.trim() || undefined,
        initial: form.initial.trim() || undefined,
        sort: form.sort,
      };
      editingId.value
        ? await api.updateBrand(editingId.value, payload)
        : await api.createBrand(payload);
    } else {
      const payload = {
        name: form.name.trim(),
        parentId: form.parentId,
        color: form.color.trim() || undefined,
        sort: form.sort,
      };
      editingId.value
        ? await api.updateTag(editingId.value, payload)
        : await api.createTag(payload);
    }
    uni.showToast({ title: "已保存", icon: "success" });
    closeEditor();
    await load();
  } catch (e) {
    uni.showToast({ title: (e as Error).message, icon: "none" });
  } finally {
    saving.value = false;
  }
}
function remove() {
  uni.showModal({
    title: `删除${config.value.shortTitle}`,
    content: "已有物品正在使用时可能无法删除。",
    confirmColor: "#c33",
    success: async (result) => {
      if (!result.confirm) return;
      saving.value = true;
      try {
        if (type.value === "categories") await api.deleteCategory(editingId.value);
        else if (type.value === "brands") await api.deleteBrand(editingId.value);
        else await api.deleteTag(editingId.value);
        closeEditor();
        await load();
      } catch (e) {
        uni.showToast({ title: (e as Error).message, icon: "none" });
      } finally {
        saving.value = false;
      }
    },
  });
}
async function load() {
  loading.value = true;
  error.value = "";
  try {
    rawItems.value =
      type.value === "categories"
        ? await api.categories()
        : type.value === "brands"
          ? await api.brands()
          : await api.tags();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
const back = () => uni.navigateBack();
onLoad((query) => {
  const requested = String(query?.type || "categories") as DictionaryType;
  type.value = ["categories", "brands", "tags"].includes(requested)
    ? requested
    : "categories";
  load();
});
</script>

<style scoped>
.dictionary-page {
  padding-bottom: 36px;
}
.topbar {
  display: flex;
  align-items: flex-start;
  gap: var(--space-sm);
  margin-bottom: 20px;
}
.back {
  width: 34px;
  justify-content: flex-start;
}
.back-icon {
  width: 10px;
  height: 10px;
  border-left: 2px solid var(--dl-text);
  border-bottom: 2px solid var(--dl-text);
  transform: rotate(45deg);
}
.page-subtitle {
  display: block;
}
.add-button {
  min-width: 68px;
  margin: 0 0 0 auto;
  border-radius: 999px;
  background: var(--dl-black);
  color: #fff;
  font-size: 12px;
}
.dictionary-list {
  padding: 4px 0;
  box-shadow: none;
}
.dictionary-row {
  min-height: 66px;
  padding: 8px 16px;
  justify-content: flex-start;
  border-bottom: 1px solid var(--dl-bg);
}
.dictionary-row:last-child {
  border-bottom: 0;
}
.item-icon,
.tag-dot {
  width: 28px;
  min-width: 28px;
  height: 28px;
  border-radius: 9px;
  background: var(--dl-bg-alt);
  display: flex;
  align-items: center;
  justify-content: center;
}
.item-icon image {
  width: 15px;
  height: 15px;
}
.tag-dot {
  width: 12px;
  min-width: 12px;
  height: 12px;
  margin: 0 8px;
  border-radius: 50%;
}
.item-copy {
  min-width: 0;
  flex: 1;
  margin-left: 12px;
}
.item-copy text {
  display: block;
}
.item-copy text:first-child {
  font-size: 14px;
  font-weight: 700;
}
.item-copy text:last-child {
  margin-top: 4px;
  overflow: hidden;
  color: var(--dl-text-secondary);
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 10px;
}
.edit-label {
  color: var(--dl-text-secondary);
  font-size: 11px;
}
.sheet-mask {
  position: fixed;
  z-index: 100;
  inset: 0;
  padding: 20px;
  background: rgba(15, 16, 14, 0.48);
  display: flex;
  align-items: flex-end;
}
.editor-sheet {
  width: 100%;
  padding: 20px;
  padding-bottom: calc(20px + env(safe-area-inset-bottom));
  border-radius: 28px;
  background: var(--dl-bg);
}
.sheet-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 18px;
}
.sheet-heading text:first-child {
  font-size: 20px;
  font-weight: 700;
}
.sheet-heading text:last-child {
  color: var(--dl-text-secondary);
  font-size: 12px;
}
.field + .field {
  margin-top: 14px;
}
.field > text {
  display: block;
  margin-bottom: 7px;
  color: var(--dl-text-secondary);
  font-size: 12px;
}
.field input,
.picker {
  min-height: 48px;
  padding: 0 14px;
  border-radius: 12px;
  background: #fff;
  display: flex;
  align-items: center;
  font-size: 13px;
}
.sheet-actions {
  display: flex;
  gap: 10px;
  margin-top: 24px;
}
.sheet-actions button {
  flex: 1;
}
.delete-button {
  min-height: 52px;
  border-radius: 999px;
  background: #fff;
  color: #b42e2e;
  font-size: 14px;
}
</style>
