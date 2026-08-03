<template>
  <view class="page route-detail">
    <view class="topbar">
      <PageBackButton fallback="/pages/routes/index" />
      <text class="page-title">{{ graph?.routeName || "路线详情" }}</text>
    </view>
    <view v-if="loading" class="loading">整理路线…</view>
    <view
      v-else-if="error"
      class="error"
      role="button"
      tabindex="0"
      @click="load"
      @keyup.enter="load"
      >{{ error }}，点击重试</view
    >
    <template v-else-if="graph">
      <view class="summary">
        <view>
          <text>净投入</text>
          <text>{{ money(graph.actualSummary?.netInvestment) }}</text>
        </view>
        <view>
          <text>物品</text>
          <text>{{ graph.actualSummary?.assetCount || actualCount }} 件</text>
        </view>
        <view>
          <text>日均成本</text>
          <text>{{ money(graph.actualSummary?.dailyCost) }}</text>
        </view>
      </view>
      <view v-if="hasPlanSummary" class="plan-summary card">
        <view
          ><text>计划预算</text
          ><text>{{ money(graph.planSummary?.plannedBudget) }}</text></view
        >
        <view
          ><text>预计回收</text
          ><text>{{ money(graph.planSummary?.expectedRecovery) }}</text></view
        >
        <view
          ><text>预计净投入</text
          ><text>{{ money(graph.planSummary?.expectedNetInvestment) }}</text></view
        >
      </view>

      <view class="view-heading">
        <view>
          <text class="view-title">升级脉络</text>
          <text class="view-count"
            >{{ generations.length }} 代 · {{ actualCount }} 件</text
          >
        </view>
        <view class="view-tabs" role="tablist" aria-label="路线查看方式">
          <view
            class="view-tab touch"
            :class="{ active: viewMode === 'outline' }"
            role="tab"
            tabindex="0"
            :aria-selected="viewMode === 'outline'"
            @click="viewMode = 'outline'"
            @keyup.enter="viewMode = 'outline'"
            @keyup.space="viewMode = 'outline'"
            >大纲</view
          >
          <view
            class="view-tab touch"
            :class="{ active: viewMode === 'cards' }"
            role="tab"
            tabindex="0"
            :aria-selected="viewMode === 'cards'"
            @click="viewMode = 'cards'"
            @keyup.enter="viewMode = 'cards'"
            @keyup.space="viewMode = 'cards'"
            >卡片</view
          >
        </view>
      </view>

      <view v-if="viewMode === 'outline'" class="outline-view">
        <view
          v-for="(generation, index) in generations"
          :key="generation.level"
          class="outline-generation"
        >
          <view class="generation-rail" aria-hidden="true">
            <view class="generation-dot" />
          </view>
          <view class="generation-content">
            <view class="generation-heading">
              <text>{{ generationLabel(generation.level) }}</text>
              <text>{{ generation.nodes.length }} 件</text>
            </view>
            <view class="outline-nodes">
              <view
                v-for="node in generation.nodes"
                :key="node.nodeId"
                class="outline-node"
                :class="{
                  main: node.mainline,
                  clickable: Boolean(node.assetId),
                  planned: node.nodeType !== 'ASSET',
                }"
                :role="node.assetId ? 'button' : undefined"
                :tabindex="node.assetId ? 0 : undefined"
                :aria-label="
                  node.assetId ? `查看${nodeName(node)}的物品详情` : undefined
                "
                @click="openNode(node)"
                @keyup.enter="openNode(node)"
                @keyup.space.prevent="openNode(node)"
              >
                <view class="branch-line" aria-hidden="true" />
                <image
                  v-if="node.coverImageUrl"
                  class="compact-cover"
                  :src="resolveMediaUrl(node.coverImageUrl)"
                  mode="aspectFill"
                />
                <view v-else class="compact-cover fallback">
                  <image
                    src="/static/icons/upgrade.svg"
                    mode="aspectFit"
                    aria-hidden="true"
                  />
                </view>
                <view class="compact-copy">
                  <view class="compact-title-row">
                    <text class="compact-name">{{ nodeName(node) }}</text>
                    <text v-if="node.mainline" class="main-badge">主线</text>
                    <text
                      v-else-if="node.nodeType !== 'ASSET'"
                      class="type-badge"
                      >{{ nodeTypeLabel(node.nodeType) }}</text
                    >
                  </view>
                  <view class="compact-meta">
                    <text>{{ node.purchaseDate || "日期未记录" }}</text>
                    <text v-if="nodeAmount(node)">{{
                      money(nodeAmount(node))
                    }}</text>
                  </view>
                </view>
                <view
                  v-if="node.assetId"
                  class="open-cue"
                  aria-hidden="true"
                />
              </view>
            </view>
            <text
              v-if="index < generations.length - 1"
              class="step-note"
              >{{
                stepText(
                  generation.nodes,
                  generations[index + 1].nodes,
                )
              }}</text
            >
          </view>
        </view>
      </view>

      <view v-else class="cards-view">
        <view
          v-for="(generation, index) in generations"
          :key="generation.level"
          class="cards-generation"
        >
          <view class="generation-heading">
            <text>{{ generationLabel(generation.level) }}</text>
            <text>{{ generation.nodes.length }} 件</text>
          </view>
          <view class="node-grid">
            <view
              v-for="node in generation.nodes"
              :key="node.nodeId"
              class="node-tile"
              :class="{
                main: node.mainline,
                clickable: Boolean(node.assetId),
                planned: node.nodeType !== 'ASSET',
              }"
              :role="node.assetId ? 'button' : undefined"
              :tabindex="node.assetId ? 0 : undefined"
              :aria-label="
                node.assetId ? `查看${nodeName(node)}的物品详情` : undefined
              "
              @click="openNode(node)"
              @keyup.enter="openNode(node)"
              @keyup.space.prevent="openNode(node)"
            >
              <image
                v-if="node.coverImageUrl"
                class="tile-cover"
                :src="resolveMediaUrl(node.coverImageUrl)"
                mode="aspectFill"
              />
              <view v-else class="tile-cover fallback">
                <image
                  src="/static/icons/upgrade.svg"
                  mode="aspectFit"
                  aria-hidden="true"
                />
              </view>
              <view class="tile-copy">
                <view class="tile-name-row">
                  <text class="tile-name">{{ nodeName(node) }}</text>
                  <view
                    v-if="node.assetId"
                    class="open-cue"
                    aria-hidden="true"
                  />
                </view>
                <text class="tile-date">{{
                  node.purchaseDate || nodeTypeLabel(node.nodeType)
                }}</text>
                <text v-if="nodeAmount(node)" class="tile-price">{{
                  money(nodeAmount(node))
                }}</text>
              </view>
            </view>
          </view>
          <text v-if="index < generations.length - 1" class="cards-step">{{
            stepText(generation.nodes, generations[index + 1].nodes)
          }}</text>
        </view>
      </view>
      <view v-if="graph.warnings?.length" class="warnings card">
        <text class="warning-title">数据提示</text>
        <text v-for="warning in graph.warnings" :key="warning">{{ warning }}</text>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import PageBackButton from "@/components/PageBackButton.vue";
import {
  api,
  type UpgradeGraph,
  type UpgradeNode,
} from "@/services/api";
import { resolveMediaUrl } from "@/services/media";

const id = ref(0),
  graph = ref<UpgradeGraph>(),
  loading = ref(true),
  error = ref(""),
  viewMode = ref<"outline" | "cards">("outline");

/**
 * H5 采用倒序纵向代际：最新一代在顶部，同代备用物品横向滑动。
 * 这能在窄屏保留路线结构，同时避免桌面图谱的横向缩放和拖拽负担。
 */
const generations = computed(() => {
    const grouped = new Map<number, UpgradeNode[]>();
    for (const node of graph.value?.nodes || []) {
      const level = Number(node.level || 0);
      grouped.set(level, [...(grouped.get(level) || []), node]);
    }
    return [...grouped.entries()]
      .sort(([a], [b]) => b - a)
      .map(([level, nodes]) => ({
        level,
        nodes: nodes.sort(
          (a, b) =>
            Number(b.mainline || false) - Number(a.mainline || false) ||
            Number(a.sort || 0) - Number(b.sort || 0),
        ),
      }));
  }),
  actualCount = computed(
    () => graph.value?.nodes.filter((node) => node.nodeType === "ASSET").length || 0,
  ),
  hasPlanSummary = computed(
    () =>
      Number(graph.value?.planSummary?.plannedBudget || 0) !== 0 ||
      Number(graph.value?.planSummary?.expectedRecovery || 0) !== 0 ||
      Number(graph.value?.planSummary?.expectedNetInvestment || 0) !== 0,
  );

const money = (value?: number) =>
    `¥ ${Number(value || 0).toLocaleString("zh-CN", { maximumFractionDigits: 2 })}`,
  generationLabel = (level: number) => (level > 0 ? `第 ${level} 代` : "起点"),
  nodeName = (node: UpgradeNode) =>
    node.name || node.title || node.targetName || "未命名物品",
  nodeAmount = (node: UpgradeNode) =>
    Number(node.primaryPurchaseAmount || node.totalInvest || 0),
  nodeTypeLabel = (type?: UpgradeNode["nodeType"]) =>
    type === "WISHLIST" ? "心愿" : type === "PLANNED" ? "计划" : "物品";

function stepText(current: UpgradeNode[], next: UpgradeNode[]) {
  const currentIds = new Set(current.map((node) => node.nodeId));
  const nextIds = new Set(next.map((node) => node.nodeId));
  const link = graph.value?.links.find(
    (item) =>
      item.relationType !== "ALTERNATIVE" &&
      ((currentIds.has(item.toNodeId) && nextIds.has(item.fromNodeId)) ||
        (currentIds.has(item.fromNodeId) && nextIds.has(item.toNodeId))),
  );
  if (!link) return "上一代";
  const details = [
    link.purchaseGapDays != null
      ? `间隔 ${Math.abs(link.purchaseGapDays)} 天`
      : "",
    link.purchasePriceDelta != null
      ? `价差 ${money(link.purchasePriceDelta)}`
      : "",
    link.replacementNetOutflow != null
      ? `补款 ${money(link.replacementNetOutflow)}`
      : "",
  ].filter(Boolean);
  return details.join(" · ") || "上一代";
}
function openNode(node: UpgradeNode) {
  if (!node.assetId) return;
  uni.navigateTo({ url: `/pages/assets/detail?id=${node.assetId}` });
}
async function load() {
  loading.value = true;
  error.value = "";
  try {
    graph.value = await api.upgradeRouteGraph(id.value);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
const back = () => uni.navigateBack();
onLoad((query) => {
  id.value = Number(query?.id || 0);
  load();
});
</script>

<style scoped>
.route-detail {
  padding-bottom: 42px;
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
.page-title {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.summary {
  display: grid;
  grid-template-columns: 1.45fr 0.8fr 1fr;
  gap: 1px;
  overflow: hidden;
  border-radius: 24px;
  background: rgba(255, 255, 255, 0.35);
}
.summary > view {
  min-width: 0;
  padding: 16px 12px;
  background: var(--dl-black);
  color: #fff;
}
.summary text {
  display: block;
}
.summary text:first-child {
  color: var(--dl-muted);
  font-size: 10px;
}
.summary text:last-child {
  margin-top: 7px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 14px;
  font-weight: 700;
}
.plan-summary {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1px;
  margin-top: 10px;
  overflow: hidden;
  box-shadow: none;
}
.plan-summary > view {
  min-width: 0;
  padding: 12px 10px;
  background: var(--dl-accent-soft);
}
.plan-summary text {
  display: block;
}
.plan-summary text:first-child {
  color: var(--dl-text-secondary);
  font-size: 9px;
}
.plan-summary text:last-child {
  margin-top: 6px;
  overflow: hidden;
  text-overflow: ellipsis;
  font-size: 11px;
  font-weight: 700;
}
.generation-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10px;
}
.generation-heading text:first-child {
  font-size: 18px;
  font-weight: 700;
}
.generation-heading text:last-child {
  color: var(--dl-text-secondary);
  font-size: 10px;
}
.warnings {
  margin-top: 24px;
  padding: 16px;
}
.warnings text {
  display: block;
  color: var(--dl-text-secondary);
  font-size: 11px;
  line-height: 1.6;
}
.warning-title {
  margin-bottom: 6px;
  color: var(--dl-text) !important;
  font-size: 13px !important;
  font-weight: 700;
}

/* Compact mobile route map */
.route-detail {
  padding-bottom: calc(var(--space-xl) + env(safe-area-inset-bottom));
}
.topbar {
  align-items: center;
  margin-bottom: var(--space-md);
}
.back {
  width: 42px;
  flex: 0 0 42px;
}
.page-title {
  min-width: 0;
  flex: 1;
  font-family: var(--font-display);
  font-size: var(--text-lg);
  letter-spacing: -0.04em;
}
.summary {
  grid-template-columns: 1.2fr 0.8fr 1fr;
  border-radius: var(--radius-card);
  background: var(--color-rule);
}
.summary > view {
  min-height: 68px;
  padding: var(--space-sm);
}
.summary text:first-child {
  font-size: var(--text-xs);
}
.summary text:last-child {
  margin-top: var(--space-xs);
  font-size: var(--text-sm);
  line-height: 1.2;
}
.plan-summary {
  margin-top: var(--space-xs);
  border: var(--rule-hairline);
  border-radius: var(--radius-input);
}
.plan-summary > view {
  padding: var(--space-xs);
}
.view-heading {
  margin: var(--space-lg) 0 var(--space-sm);
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: var(--space-sm);
}
.view-heading > view:first-child {
  min-width: 0;
}
.view-title,
.view-count {
  display: block;
}
.view-title {
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-weight: 700;
  letter-spacing: -0.03em;
}
.view-count {
  margin-top: var(--space-2xs);
  color: var(--color-muted);
  font-size: var(--text-xs);
}
.view-tabs {
  flex: 0 0 auto;
  padding: var(--space-3xs);
  border: var(--rule-hairline);
  border-radius: var(--radius-pill);
  background: var(--color-paper-2);
  display: flex;
}
.view-tab {
  min-height: 32px;
  padding: 0 var(--space-sm);
  border-radius: var(--radius-pill);
  color: var(--color-muted);
  font-size: var(--text-xs);
  font-weight: 600;
}
.view-tab.active {
  background: var(--color-ink);
  color: var(--color-surface);
}
.outline-generation {
  display: grid;
  grid-template-columns: 16px minmax(0, 1fr);
  gap: var(--space-sm);
}
.generation-rail {
  position: relative;
}
.generation-rail::after {
  content: "";
  position: absolute;
  z-index: var(--z-base);
  top: 14px;
  bottom: -18px;
  left: 7px;
  width: 2px;
  border-radius: var(--radius-pill);
  background: var(--color-rule);
}
.outline-generation:last-child .generation-rail::after {
  display: none;
}
.generation-dot {
  position: relative;
  z-index: var(--z-raised);
  width: 16px;
  height: 16px;
  margin-top: var(--space-3xs);
  border: 4px solid var(--color-paper);
  border-radius: 50%;
  background: var(--color-accent-deep);
  box-shadow: 0 0 0 1px var(--color-accent-deep);
}
.generation-content {
  min-width: 0;
  padding-bottom: var(--space-lg);
}
.generation-heading {
  margin-bottom: var(--space-xs);
}
.generation-heading text:first-child {
  font-family: var(--font-display);
  font-size: var(--text-base);
}
.generation-heading text:last-child {
  font-size: var(--text-xs);
}
.outline-nodes {
  display: grid;
  gap: var(--space-xs);
}
.outline-node {
  position: relative;
  min-width: 0;
  min-height: 68px;
  padding: var(--space-xs) 34px var(--space-xs) var(--space-xs);
  border: var(--rule-hairline);
  border-radius: var(--radius-input);
  background: var(--color-surface);
  display: flex;
  align-items: center;
  gap: var(--space-xs);
  box-shadow: 0 6px 18px -16px var(--color-shadow-ambient);
  transition:
    transform var(--dur-micro) var(--ease-out),
    border-color var(--dur-short) var(--ease-out);
}
.outline-node.main {
  border-left: 4px solid var(--color-accent-deep);
  padding-left: var(--space-2xs);
}
.outline-node.planned {
  background: var(--color-cyan-soft);
}
.outline-node.clickable {
  cursor: pointer;
}
.outline-node.clickable:active,
.node-tile.clickable:active {
  transform: translateY(2px);
}
.branch-line {
  position: absolute;
  top: 50%;
  left: -20px;
  width: 19px;
  height: 1px;
  background: var(--color-rule);
}
.compact-cover {
  width: 52px;
  height: 52px;
  flex: 0 0 52px;
  border-radius: 10px;
  background: var(--color-paper-2);
}
.compact-cover.fallback,
.tile-cover.fallback {
  display: flex;
  align-items: center;
  justify-content: center;
}
.compact-cover.fallback image,
.tile-cover.fallback image {
  width: 22px;
  height: 22px;
}
.compact-copy {
  min-width: 0;
  flex: 1;
}
.compact-title-row {
  min-width: 0;
  display: flex;
  align-items: center;
  gap: var(--space-2xs);
}
.compact-name {
  min-width: 0;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-sm);
  font-weight: 700;
}
.main-badge,
.type-badge {
  flex: 0 0 auto;
  padding: var(--space-3xs) var(--space-2xs);
  border-radius: var(--radius-pill);
  background: var(--color-accent-soft);
  color: var(--color-ink-2);
  font-size: 9px;
  font-weight: 700;
}
.type-badge {
  background: var(--color-cyan-soft);
}
.compact-meta {
  margin-top: var(--space-xs);
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: var(--space-xs);
  color: var(--color-muted);
  font-size: var(--text-xs);
}
.compact-meta text:last-child {
  color: var(--color-ink);
  font-weight: 700;
}
.open-cue {
  position: absolute;
  top: 50%;
  right: var(--space-sm);
  width: 8px;
  height: 8px;
  border-top: 1.5px solid var(--color-muted);
  border-right: 1.5px solid var(--color-muted);
  transform: translateY(-50%) rotate(45deg);
}
.step-note,
.cards-step {
  display: inline-block;
  margin-top: var(--space-xs);
  padding: var(--space-2xs) var(--space-xs);
  border-radius: var(--radius-pill);
  background: var(--color-accent-soft);
  color: var(--color-ink-2);
  font-size: 9px;
}
.cards-generation {
  margin-top: var(--space-lg);
}
.cards-generation:first-child {
  margin-top: 0;
}
.node-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: var(--space-xs);
}
.node-tile {
  min-width: 0;
  overflow: hidden;
  position: relative;
  border: var(--rule-hairline);
  border-radius: var(--radius-input);
  background: var(--color-surface);
  box-shadow: 0 8px 22px -18px var(--color-shadow-ambient);
  transition: transform var(--dur-micro) var(--ease-out);
}
.node-tile.main {
  border-bottom: 3px solid var(--color-accent-deep);
}
.node-tile.planned {
  background: var(--color-cyan-soft);
}
.node-tile.clickable {
  cursor: pointer;
}
.tile-cover {
  width: 100%;
  height: 92px;
  background: var(--color-paper-2);
}
.tile-copy {
  min-height: 82px;
  padding: var(--space-xs);
}
.tile-name-row {
  position: relative;
  padding-right: var(--space-md);
}
.tile-name {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: var(--text-sm);
  font-weight: 700;
}
.tile-name-row .open-cue {
  right: var(--space-2xs);
}
.tile-date,
.tile-price {
  display: block;
  margin-top: var(--space-2xs);
}
.tile-date {
  color: var(--color-muted);
  font-size: var(--text-xs);
}
.tile-price {
  font-size: var(--text-xs);
  font-weight: 700;
}
.cards-step {
  margin: var(--space-xs) 0 0;
}
.warnings {
  margin-top: var(--space-sm);
  padding: var(--space-sm);
  border-radius: var(--radius-input);
  box-shadow: none;
}

@media (hover: hover) and (pointer: fine) {
  .outline-node.clickable:hover,
  .node-tile.clickable:hover {
    border-color: var(--color-accent-deep);
    transform: translateY(-1px);
  }
}
</style>
