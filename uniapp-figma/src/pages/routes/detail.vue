<template>
  <view class="page route-detail">
    <view class="topbar">
      <view class="back touch" @click="back"><view class="back-icon" /></view>
      <view class="title-copy">
        <text class="page-title">{{ graph?.routeName || "路线详情" }}</text>
        <text class="page-subtitle">只读升级历程</text>
      </view>
    </view>
    <view v-if="loading" class="loading">整理路线…</view>
    <view v-else-if="error" class="error" @click="load">{{ error }}，点击重试</view>
    <template v-else-if="graph">
      <view class="summary">
        <view>
          <text>真实净投入</text>
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
      <text v-if="graph.remark" class="route-remark">{{ graph.remark }}</text>
      <view v-for="(generation, index) in generations" :key="generation.level" class="generation">
        <view class="generation-heading">
          <text>{{ generationLabel(generation.level) }}</text>
          <text>{{ generation.nodes.length }} 个节点</text>
        </view>
        <scroll-view scroll-x class="node-scroll">
          <view class="node-row">
            <view
              v-for="node in generation.nodes"
              :key="node.nodeId"
              class="node-card card"
              :class="{ main: node.mainline, planned: node.nodeType !== 'ASSET' }"
              @click="openNode(node)"
            >
              <image
                v-if="node.coverImageUrl"
                class="node-cover"
                :src="node.coverImageUrl"
                mode="aspectFill"
              />
              <view v-else class="node-cover fallback">
                <image src="/static/icons/upgrade.svg" mode="aspectFit" />
              </view>
              <view class="node-copy">
                <view class="node-badges">
                  <text>{{ nodeTypeLabel(node.nodeType) }}</text>
                  <text v-if="node.mainline">主物品</text>
                </view>
                <text class="node-name">{{ node.name || node.title || node.targetName || "未命名节点" }}</text>
                <text class="node-meta">{{ [node.brandName, node.model].filter(Boolean).join(" · ") || node.assetStatus || node.status }}</text>
                <text class="node-date">{{ node.purchaseDate || "日期未记录" }}</text>
                <text v-if="node.useDays" class="node-date">使用 {{ node.useDays }} 天</text>
                <text class="node-price">{{ money(node.primaryPurchaseAmount || node.totalInvest || 0) }}</text>
                <text v-if="node.remark" class="node-remark">{{ node.remark }}</text>
              </view>
            </view>
          </view>
        </scroll-view>
        <view v-if="index < generations.length - 1" class="step">
          <text>{{ stepText(generation.nodes, generations[index + 1].nodes) }}</text>
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
import {
  api,
  type UpgradeGraph,
  type UpgradeNode,
} from "@/services/api";

const id = ref(0),
  graph = ref<UpgradeGraph>(),
  loading = ref(true),
  error = ref("");

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
  nodeTypeLabel = (type?: UpgradeNode["nodeType"]) =>
    type === "WISHLIST" ? "心愿" : type === "PLANNED" ? "计划" : "实际物品";

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
.title-copy {
  min-width: 0;
  flex: 1;
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
.route-remark {
  display: block;
  margin: 14px 2px 4px;
  color: var(--dl-text-secondary);
  font-size: 12px;
  line-height: 1.6;
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
.generation {
  margin-top: 24px;
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
.node-scroll {
  width: calc(100vw - 20px);
  margin-right: -20px;
}
.node-row {
  display: flex;
  gap: 12px;
  padding-right: 20px;
}
.node-card {
  width: min(76vw, 286px);
  min-width: min(76vw, 286px);
  overflow: hidden;
}
.node-card.main {
  box-shadow: 0 0 0 3px var(--dl-lime), 0 8px 24px rgba(0, 0, 0, 0.06);
}
.node-cover {
  width: 100%;
  height: 150px;
}
.node-cover.fallback {
  background: var(--dl-accent-soft);
  display: flex;
  align-items: center;
  justify-content: center;
}
.node-cover.fallback image {
  width: 40px;
  height: 40px;
}
.node-copy {
  padding: 14px 16px 16px;
}
.node-badges {
  display: flex;
  gap: 6px;
}
.node-badges text {
  padding: 4px 8px;
  border-radius: 999px;
  background: var(--dl-bg-alt);
  color: var(--dl-text-secondary);
  font-size: 9px;
}
.node-badges text:last-child:not(:first-child) {
  background: var(--dl-lime);
  color: var(--dl-text);
}
.node-name,
.node-meta,
.node-date,
.node-price {
  display: block;
}
.node-remark {
  display: block;
  margin-top: 9px;
  padding-top: 9px;
  border-top: 1px solid var(--dl-bg);
  color: var(--dl-text-secondary);
  font-size: 10px;
  line-height: 1.5;
}
.node-name {
  margin-top: 10px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  font-size: 17px;
  font-weight: 700;
}
.node-meta,
.node-date {
  margin-top: 5px;
  color: var(--dl-text-secondary);
  font-size: 11px;
}
.node-price {
  margin-top: 12px;
  font-size: 15px;
  font-weight: 700;
}
.step {
  min-height: 42px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.step text {
  padding: 5px 11px;
  border-radius: 999px;
  background: var(--dl-accent-soft);
  color: var(--dl-text-secondary);
  font-size: 9px;
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
</style>
