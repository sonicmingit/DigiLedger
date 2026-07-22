<template>
  <view class="page settings-page"
    ><AppHeader title="设置" subtitle="管理分类、偏好和数据" /><view
      class="profile"
      ><view class="avatar">DL</view
      ><view
        ><text>DigiLedger 物品库</text
        ><text>{{ currentLabel }} · {{ statusText }}</text></view
      ></view
    ><text
      class="section server-title"
      @click="serverExpanded = !serverExpanded"
      >服务器与故障转移 · {{ serverExpanded ? "收起" : "展开" }}</text
    ><view v-if="serverExpanded" class="card server"
      ><view class="field"
        ><text>主服务器地址</text
        ><input
          v-model="form.primaryUrl"
          placeholder="https://server.example.com/api" /></view
      ><view class="test-row"
        ><button
          class="test"
          :loading="testing.primary"
          @click="test('primary')"
        >
          测试主节点</button
        ><text :class="nodeState.primary">{{ nodeText.primary }}</text></view
      ><view class="field"
        ><text>备用服务器地址</text
        ><input
          v-model="form.secondaryUrl"
          placeholder="https://backup.example.com/api" /></view
      ><view class="test-row"
        ><button
          class="test"
          :loading="testing.secondary"
          @click="test('secondary')"
        >
          测试备用节点</button
        ><text :class="nodeState.secondary">{{
          nodeText.secondary
        }}</text></view
      ><view class="field"
        ><text>首选节点</text
        ><picker
          :range="['主节点', '备用节点']"
          :value="form.preferred === 'primary' ? 0 : 1"
          @change="
            form.preferred =
              Number($event.detail.value) === 0 ? 'primary' : 'secondary'
          "
          ><view class="picker">{{
            form.preferred === "primary" ? "主节点" : "备用节点"
          }}</view></picker
        ></view
      ><view class="switch-row"
        ><text>自动故障转移</text
        ><switch
          :checked="form.autoFailover"
          color="#171915"
          @change="form.autoFailover = $event.detail.value" /></view
      ><view class="field"
        ><text>连接超时（毫秒）</text
        ><input v-model.number="form.timeoutMs" type="number" /></view
      ><button class="primary save-server" @click="save">保存服务器设置</button
      ><text class="policy"
        >GET / HEAD 仅在断网、超时或 5xx
        时向另一节点重试一次；写请求不会跨节点自动重放。</text
      ></view
    ><text class="section manage-title">管理</text
    ><view class="settings-list card manage-list"
      ><view
        v-for="item in management"
        :key="item.key"
        class="setting touch"
        @click="showDictionary(item.key)"
        ><image :src="item.icon" /><text>{{ item.label }}</text
        ><text class="count">{{ counts[item.key] || 0 }} 个</text
        ><view class="chevron" /></view></view
    ><text class="section preference-title">偏好</text
    ><view class="settings-list card preference-list"
      ><view class="setting touch" @click="exportData"
        ><view class="export-icon" /><text>数据导出</text
        ><text class="count">CSV / JSON</text
        ><view class="chevron" /></view></view
    ><BottomNav active="settings"
  /></view>
</template>
<script setup lang="ts">
import { computed, reactive, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import AppHeader from "@/components/AppHeader.vue";
import BottomNav from "@/components/BottomNav.vue";
import {
  getServerProfile,
  saveServerProfile,
  type NodeName,
  type ServerProfile,
} from "@/services/server-profile";
import { testNode } from "@/services/http";
import { api } from "@/services/api";
const form = reactive<ServerProfile>(getServerProfile()),
  serverExpanded = ref(false),
  testing = reactive({ primary: false, secondary: false }),
  nodeState = reactive({ primary: "idle", secondary: "idle" }),
  nodeText = reactive({ primary: "未测试", secondary: "未测试" }),
  statusText = ref("本地配置"),
  counts = reactive<Record<string, number>>({
    categories: 0,
    brands: 0,
    tags: 0,
  });
const management = [
  { key: "categories", label: "物品分类", icon: "/static/icons/folder.svg" },
  { key: "brands", label: "品牌管理", icon: "/static/icons/tag.svg" },
  { key: "tags", label: "标签管理", icon: "/static/icons/tag.svg" },
];
const currentLabel = computed(() =>
  form.preferred === "primary" ? "主节点优先" : "备用节点优先",
);
async function test(node: NodeName) {
  saveServerProfile(form);
  testing[node] = true;
  nodeText[node] = "连接中";
  try {
    await testNode(node);
    nodeState[node] = "ok";
    nodeText[node] = "连接正常";
  } catch (e) {
    nodeState[node] = "bad";
    nodeText[node] = (e as Error).message;
  } finally {
    testing[node] = false;
  }
}
function save() {
  Object.assign(form, saveServerProfile(form));
  statusText.value = "已保存";
  uni.showToast({ title: "设置已保存", icon: "success" });
}
async function loadCounts() {
  const [c, b, t] = await Promise.all([
    api.categories(),
    api.brands(),
    api.tags(),
  ]);
  counts.categories = c.length;
  counts.brands = b.length;
  counts.tags = t.length;
}
function showDictionary(key: string) {
  const label = management.find((x) => x.key === key)?.label || "管理";
  uni.showModal({
    title: label,
    content: `当前共 ${counts[key] || 0} 项。完整 CRUD 由后端字典接口支持；移动端当前提供查看入口。`,
    showCancel: false,
  });
}
function exportData() {
  uni.showActionSheet({
    itemList: ["导出 JSON", "导出 CSV"],
    success: (r) => {
      const format = r.tapIndex === 0 ? "json" : "csv";
      uni.showModal({
        title: "导出地址",
        content: `请在当前首选节点访问 /api/data/export?format=${format}`,
        showCancel: false,
      });
    },
  });
}
onLoad(() => loadCounts().catch(() => {}));
</script>
<style scoped>
.settings-page {
  display: flex;
  flex-direction: column;
}
.profile {
  order: 1;
  height: 100px;
  border-radius: 28px;
  background: var(--dl-black);
  color: #fff;
  padding: 18px;
  display: flex;
  align-items: center;
}
.avatar {
  width: 64px;
  height: 64px;
  border-radius: 50%;
  background: var(--dl-lime);
  color: var(--dl-text);
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
}
.profile > view:last-child {
  margin-left: 18px;
}
.profile text {
  display: block;
  font-weight: 700;
}
.profile text:last-child {
  margin-top: 8px;
  color: var(--dl-muted);
  font-size: 11px;
  font-weight: 400;
}
.section {
  display: block;
  margin: 26px 0 10px;
  color: var(--dl-text-secondary);
  font-size: 13px;
}
.manage-title {
  order: 2;
}
.manage-list {
  order: 3;
}
.preference-title {
  order: 4;
}
.preference-list {
  order: 5;
}
.server-title {
  order: 6;
  min-height: 48px;
  display: flex;
  align-items: center;
  margin-bottom: 0;
}
.server {
  order: 7;
  padding: 18px;
}
.field {
  margin-bottom: 14px;
}
.field > text {
  display: block;
  font-size: 12px;
  color: var(--dl-text-secondary);
  margin-bottom: 6px;
}
.field input,
.picker {
  min-height: 48px;
  padding: 0 14px;
  border-radius: 12px;
  background: var(--dl-bg);
  font-size: 13px;
}
.picker {
  display: flex;
  align-items: center;
}
.test-row {
  display: flex;
  align-items: center;
  margin: -6px 0 16px;
}
.test {
  margin: 0;
  width: 128px;
  min-height: 40px;
  border-radius: 999px;
  background: var(--dl-accent-soft);
  font-size: 12px;
}
.test-row text {
  margin-left: 12px;
  font-size: 11px;
  max-width: 160px;
}
.ok {
  color: var(--dl-success);
}
.bad {
  color: #c33;
}
.idle {
  color: var(--dl-muted);
}
.switch-row {
  min-height: 52px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
}
.save-server {
  width: 100%;
  margin-top: 8px;
}
.policy {
  display: block;
  margin-top: 10px;
  color: var(--dl-muted);
  font-size: 10px;
  line-height: 16px;
}
.settings-list {
  padding: 4px 0;
}
.setting {
  justify-content: flex-start;
  padding: 0 16px;
  border-bottom: 1px solid var(--dl-bg);
}
.setting:last-child {
  border-bottom: 0;
}
.setting image {
  width: 18px;
  height: 18px;
  margin-right: 14px;
}
.setting > text:not(.count) {
  font-size: 14px;
  font-weight: 600;
}
.count {
  margin-left: auto;
  color: var(--dl-text-secondary);
  font-size: 11px;
}
.chevron {
  width: 8px;
  height: 8px;
  border-top: 1px solid var(--dl-muted);
  border-right: 1px solid var(--dl-muted);
  transform: rotate(45deg);
  margin-left: 9px;
}
.export-icon {
  width: 16px;
  height: 18px;
  border: 1.5px solid var(--dl-text);
  border-radius: 2px;
  margin-right: 14px;
}
</style>
