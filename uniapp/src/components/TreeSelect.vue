<template>
  <view class="tree-select">
    <view
      class="tree-trigger touch"
      :class="{ open, disabled }"
      role="button"
      tabindex="0"
      :aria-expanded="open"
      :aria-disabled="disabled"
      @click="toggleOpen"
      @keyup.enter="toggleOpen"
      @keyup.space="toggleOpen"
    >
      <text :class="{ placeholder: !selectedIds.length }">{{ triggerLabel }}</text>
      <view class="trigger-cue" :class="{ open }" aria-hidden="true" />
    </view>

    <view v-if="open" class="tree-panel">
      <view class="tree-toolbar">
        <view class="tree-search">
          <image src="/static/icons/search.svg" aria-hidden="true" />
          <input
            v-model="query"
            confirm-type="search"
            :placeholder="searchPlaceholder"
          />
        </view>
        <view
          class="clear-action touch"
          role="button"
          tabindex="0"
          @click="clearSelection"
          @keyup.enter="clearSelection"
          @keyup.space="clearSelection"
        >
          {{ clearLabel }}
        </view>
      </view>

      <scroll-view class="tree-list" scroll-y>
        <view
          v-for="row in visibleRows"
          :key="row.id"
          class="tree-row touch"
          :class="{ selected: selectedIds.includes(row.id) }"
          :style="{ paddingLeft: `${12 + row.depth * 16}px` }"
          role="button"
          tabindex="0"
          :aria-selected="selectedIds.includes(row.id)"
          @click="choose(row.id)"
          @keyup.enter="choose(row.id)"
          @keyup.space="choose(row.id)"
        >
          <view
            v-if="row.hasChildren"
            class="branch-toggle touch"
            :class="{ expanded: isExpanded(row.id) }"
            role="button"
            tabindex="0"
            :aria-label="isExpanded(row.id) ? `收起${row.name}` : `展开${row.name}`"
            :aria-expanded="isExpanded(row.id)"
            @click.stop="toggleBranch(row.id)"
            @keyup.enter.stop="toggleBranch(row.id)"
            @keyup.space.stop="toggleBranch(row.id)"
          >
            <view class="branch-cue" aria-hidden="true" />
          </view>
          <view v-else class="branch-spacer" aria-hidden="true" />
          <text class="row-name">{{ row.name }}</text>
          <view
            class="selection-mark"
            :class="{ multiple, checked: selectedIds.includes(row.id) }"
            aria-hidden="true"
          >
            <view v-if="selectedIds.includes(row.id)" class="check-cue" />
          </view>
        </view>
        <view v-if="!visibleRows.length" class="tree-empty">没有匹配项</view>
      </scroll-view>

      <view v-if="multiple" class="tree-footer">
        <text>{{ selectedIds.length ? `已选 ${selectedIds.length} 项` : "暂未选择" }}</text>
        <view
          class="done touch"
          role="button"
          tabindex="0"
          @click="close"
          @keyup.enter="close"
          @keyup.space="close"
        >
          完成
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref, watch } from "vue";

type TreeNode = {
  id: number;
  name: string;
  children?: TreeNode[];
};

type TreeRow = TreeNode & {
  depth: number;
  pathLabel: string;
  hasChildren: boolean;
};

const props = withDefaults(
  defineProps<{
    modelValue?: number | number[] | null;
    nodes: TreeNode[];
    multiple?: boolean;
    placeholder?: string;
    searchPlaceholder?: string;
    clearLabel?: string;
    disabled?: boolean;
  }>(),
  {
    modelValue: undefined,
    multiple: false,
    placeholder: "请选择",
    searchPlaceholder: "搜索",
    clearLabel: "清除",
    disabled: false,
  },
);

const emit = defineEmits<{
  (event: "update:modelValue", value: number | number[] | undefined): void;
  (event: "change", value: number | number[] | undefined): void;
}>();

const open = ref(false);
const query = ref("");
const expandedIds = ref<number[]>([]);

const selectedIds = computed<number[]>(() => {
  if (Array.isArray(props.modelValue)) return props.modelValue;
  return props.modelValue == null ? [] : [props.modelValue];
});

function flatten(
  nodes: TreeNode[],
  depth = 0,
  parentPath = "",
): TreeRow[] {
  return nodes.flatMap((node) => {
    const pathLabel = parentPath ? `${parentPath} / ${node.name}` : node.name;
    const row: TreeRow = {
      ...node,
      depth,
      pathLabel,
      hasChildren: Boolean(node.children?.length),
    };
    return [row, ...flatten(node.children || [], depth + 1, pathLabel)];
  });
}

const allRows = computed(() => flatten(props.nodes));

const triggerLabel = computed(() => {
  if (!selectedIds.value.length) return props.placeholder;
  if (props.multiple) {
    if (selectedIds.value.length > 1) return `已选 ${selectedIds.value.length} 项`;
    return (
      allRows.value.find((row) => row.id === selectedIds.value[0])?.pathLabel ||
      props.placeholder
    );
  }
  return (
    allRows.value.find((row) => row.id === selectedIds.value[0])?.pathLabel ||
    props.placeholder
  );
});

function branchMatches(
  node: TreeNode,
  parentPath: string,
  normalizedQuery: string,
): boolean {
  const pathLabel = parentPath ? `${parentPath} / ${node.name}` : node.name;
  if (pathLabel.toLocaleLowerCase("zh-CN").includes(normalizedQuery)) return true;
  return (node.children || []).some((child) =>
    branchMatches(child, pathLabel, normalizedQuery),
  );
}

const visibleRows = computed<TreeRow[]>(() => {
  const rows: TreeRow[] = [];
  const normalizedQuery = query.value.trim().toLocaleLowerCase("zh-CN");
  const visit = (nodes: TreeNode[], depth = 0, parentPath = "") => {
    for (const node of nodes) {
      const pathLabel = parentPath ? `${parentPath} / ${node.name}` : node.name;
      if (
        normalizedQuery &&
        !branchMatches(node, parentPath, normalizedQuery)
      )
        continue;
      const hasChildren = Boolean(node.children?.length);
      rows.push({ ...node, depth, pathLabel, hasChildren });
      if (
        hasChildren &&
        (normalizedQuery || expandedIds.value.includes(node.id))
      )
        visit(node.children || [], depth + 1, pathLabel);
    }
  };
  visit(props.nodes);
  return rows;
});

function toggleOpen() {
  if (props.disabled) return;
  open.value = !open.value;
  if (open.value && !expandedIds.value.length)
    expandedIds.value = props.nodes
      .filter((node) => node.children?.length)
      .map((node) => node.id);
  if (!open.value) query.value = "";
}

function close() {
  open.value = false;
  query.value = "";
}

function isExpanded(id: number) {
  return Boolean(query.value.trim()) || expandedIds.value.includes(id);
}

function toggleBranch(id: number) {
  if (query.value.trim()) return;
  expandedIds.value = expandedIds.value.includes(id)
    ? expandedIds.value.filter((value) => value !== id)
    : [...expandedIds.value, id];
}

function publish(value: number | number[] | undefined) {
  emit("update:modelValue", value);
  emit("change", value);
}

function choose(id: number) {
  if (props.multiple) {
    publish(
      selectedIds.value.includes(id)
        ? selectedIds.value.filter((value) => value !== id)
        : [...selectedIds.value, id],
    );
    return;
  }
  publish(id);
  close();
}

function clearSelection() {
  publish(props.multiple ? [] : undefined);
  if (!props.multiple) close();
}

watch(
  () => props.nodes,
  (nodes) => {
    if (open.value && !expandedIds.value.length)
      expandedIds.value = nodes
        .filter((node) => node.children?.length)
        .map((node) => node.id);
  },
);
</script>

<style scoped>
.tree-select {
  position: relative;
}
.tree-trigger {
  width: 100%;
  min-height: 48px;
  padding: 0 var(--space-sm);
  border: var(--rule-hairline);
  border-radius: var(--radius-input);
  background: var(--color-paper-2);
  justify-content: space-between;
  gap: var(--space-sm);
  color: var(--color-ink);
  font-size: var(--text-sm);
}
.tree-trigger.open {
  border-color: var(--color-focus);
}
.tree-trigger.disabled {
  opacity: 0.48;
  cursor: not-allowed;
}
.tree-trigger text {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.tree-trigger .placeholder {
  color: var(--color-muted);
}
.trigger-cue,
.branch-cue {
  width: 7px;
  height: 7px;
  border-right: 1.5px solid currentColor;
  border-bottom: 1.5px solid currentColor;
  transform: rotate(45deg);
  transition: transform var(--dur-short) var(--ease-out);
}
.trigger-cue {
  flex: 0 0 auto;
  margin: -4px 3px 0 0;
}
.trigger-cue.open {
  margin-top: 4px;
  transform: rotate(225deg);
}
.tree-panel {
  margin-top: var(--space-xs);
  border: var(--rule-hairline);
  border-radius: var(--radius-input);
  background: var(--color-surface);
  box-shadow: var(--shadow-card);
  overflow: hidden;
}
.tree-toolbar {
  padding: var(--space-xs);
  border-bottom: var(--rule-hairline);
  display: flex;
  align-items: center;
  gap: var(--space-xs);
}
.tree-search {
  min-width: 0;
  min-height: 42px;
  flex: 1;
  padding: 0 var(--space-sm);
  border-radius: var(--radius-input);
  background: var(--color-paper);
  display: flex;
  align-items: center;
}
.tree-search image {
  width: 16px;
  height: 16px;
  margin-right: var(--space-xs);
  opacity: 0.64;
}
.tree-search input {
  min-width: 0;
  height: 42px;
  flex: 1;
  font-size: var(--text-sm);
}
.clear-action {
  min-width: 64px;
  min-height: 42px;
  padding: 0 var(--space-xs);
  border-radius: var(--radius-input);
  color: var(--color-focus);
  font-size: var(--text-xs);
  font-weight: 700;
}
.tree-list {
  max-height: 288px;
}
.tree-row {
  width: 100%;
  min-height: 48px;
  padding-right: var(--space-sm);
  border-bottom: var(--rule-hairline);
  justify-content: flex-start;
  gap: var(--space-xs);
  color: var(--color-ink-2);
}
.tree-row:last-child {
  border-bottom: 0;
}
.tree-row.selected {
  background: var(--color-accent-soft);
  color: var(--color-accent-ink);
  font-weight: 700;
}
.branch-toggle,
.branch-spacer {
  width: 28px;
  min-width: 28px;
  min-height: 40px;
  padding: 0;
}
.branch-spacer {
  display: block;
}
.branch-cue {
  transform: rotate(-45deg);
}
.branch-toggle.expanded .branch-cue {
  margin-top: -4px;
  transform: rotate(45deg);
}
.row-name {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-sm);
}
.selection-mark {
  width: 18px;
  min-width: 18px;
  height: 18px;
  border: var(--rule-hairline);
  border-radius: var(--radius-pill);
  display: flex;
  align-items: center;
  justify-content: center;
}
.selection-mark.multiple {
  border-radius: var(--space-2xs);
}
.selection-mark.checked {
  border-color: var(--color-accent-deep);
  background: var(--color-accent);
}
.check-cue {
  width: 7px;
  height: 4px;
  margin-top: -2px;
  border-left: 1.5px solid var(--color-accent-ink);
  border-bottom: 1.5px solid var(--color-accent-ink);
  transform: rotate(-45deg);
}
.tree-empty {
  min-height: 88px;
  color: var(--color-muted);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: var(--text-sm);
}
.tree-footer {
  min-height: 52px;
  padding: var(--space-xs);
  border-top: var(--rule-hairline);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-sm);
}
.tree-footer > text {
  color: var(--color-muted);
  font-size: var(--text-xs);
}
.done {
  min-width: 72px;
  min-height: 40px;
  padding: 0 var(--space-sm);
  border-radius: var(--radius-pill);
  background: var(--color-accent);
  color: var(--color-accent-ink);
  font-size: var(--text-sm);
  font-weight: 700;
}
</style>
