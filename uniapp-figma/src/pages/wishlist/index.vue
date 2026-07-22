<template>
  <view class="page"
    ><AppHeader title="心愿单" subtitle="把想买的先放这里，等合适时机"
      ><view class="round-add touch" @click="openEditor()"
        ><image src="/static/icons/plus.svg" /></view></AppHeader
    ><view class="budget"
      ><text>计划预算</text><text class="budget-value">{{ money(total) }}</text
      ><view class="badge">{{ items.length }} 件物品</view></view
    ><text class="section-label">优先级</text
    ><view class="pills"
      ><view
        v-for="p in priorities"
        :key="p.value"
        class="pill"
        :class="{ active: priority === p.value }"
        @click="priority = p.value"
        >{{ p.label }}</view
      ></view
    ><view v-if="loading" class="loading">载入心愿中…</view
    ><view v-else-if="error" class="error" @click="load">{{ error }}</view
    ><view v-else-if="!filtered.length" class="empty card">暂时没有心愿</view
    ><view
      v-for="(item, index) in filtered"
      :key="item.id"
      class="wish card touch"
      @click="actions(item)"
      ><view class="visual" :class="{ soft: index % 2 === 0 }"
        ><image
          v-if="item.imageUrl"
          :src="item.imageUrl"
          mode="aspectFill"
        /><text v-else>{{ item.name.slice(0, 2) }}</text></view
      ><view class="wish-info"
        ><text class="wish-name">{{ item.name }}</text
        ><text class="wish-meta"
          >{{ priorityLabel(item.priority) }} ·
          {{ item.categoryName || "未分类" }}</text
        ><text class="wish-price">{{
          money(item.currentPrice || item.expectedPrice)
        }}</text></view
      ></view
    ><BottomNav active="wishlist"
  /></view>
</template>
<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad, onPullDownRefresh } from "@dcloudio/uni-app";
import AppHeader from "@/components/AppHeader.vue";
import BottomNav from "@/components/BottomNav.vue";
import { api, type Wishlist } from "@/services/api";
const items = ref<Wishlist[]>([]),
  loading = ref(false),
  error = ref(""),
  priority = ref(0);
const priorities = [
    { label: "全部", value: 0 },
    { label: "高", value: 3 },
    { label: "中", value: 2 },
    { label: "低", value: 1 },
  ],
  filtered = computed(() =>
    priority.value
      ? items.value.filter((x) => x.priority === priority.value)
      : items.value,
  ),
  total = computed(() =>
    items.value.reduce(
      (n, x) => n + Number(x.currentPrice || x.expectedPrice || 0),
      0,
    ),
  );
const money = (n?: number) => `¥ ${Number(n || 0).toLocaleString("zh-CN")}`,
  priorityLabel = (p?: number) =>
    p === 3 ? "高优先级" : p === 2 ? "中优先级" : "低优先级";
async function load() {
  loading.value = true;
  try {
    items.value = await api.wishlist();
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
    uni.stopPullDownRefresh();
  }
}
function openEditor(item?: Wishlist) {
  const existing = item;
  let name = existing?.name || "",
    price = String(existing?.expectedPrice || "");
  uni.showModal({
    title: existing ? "编辑心愿" : "新增心愿",
    editable: true,
    placeholderText: "心愿名称",
    content: name,
    success: (r) => {
      if (r.confirm && r.content) {
        uni.showModal({
          title: "预算",
          editable: true,
          placeholderText: "预计价格",
          content: price,
          success: async (p) => {
            if (!p.confirm) return;
            const payload = {
              name: r.content,
              expectedPrice: Number(p.content || 0),
              priority: existing?.priority || 2,
            };
            existing
              ? await api.updateWishlist(existing.id, payload)
              : await api.createWishlist(payload);
            load();
          },
        });
      }
    },
  });
}
function actions(item: Wishlist) {
  uni.showActionSheet({
    itemList: ["编辑", "标记已购", "转为物品", "删除"],
    success: async (r) => {
      if (r.tapIndex === 0) return openEditor(item);
      if (r.tapIndex === 1) await api.markPurchased(item.id);
      if (r.tapIndex === 2) {
        const today = new Date().toISOString().slice(0, 10);
        await api.convertWishlist(item.id, {
          name: item.name,
          categoryId: (item as any).categoryId || 1,
          status: "使用中",
          purchaseDate: today,
          targetCostValue: Number(item.currentPrice || item.expectedPrice || 0),
          targetCostStrategy: "WISHLIST_BUDGET",
        });
        uni.showToast({ title: "已转为物品", icon: "success" });
      }
      if (r.tapIndex === 3) await api.deleteWishlist(item.id);
      load();
    },
  });
}
onLoad(load);
onPullDownRefresh(load);
</script>
<style scoped>
.round-add {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: var(--dl-black);
}
.round-add image {
  width: 20px;
  height: 20px;
}
.budget {
  height: 126px;
  position: relative;
  border-radius: 28px;
  background: var(--dl-black);
  color: #fff;
  padding: 18px 20px;
}
.budget > text:first-child {
  color: var(--dl-muted);
  font-size: 13px;
}
.budget-value {
  display: block;
  font-size: 30px;
  font-weight: 700;
  margin-top: 7px;
}
.badge {
  position: absolute;
  right: 18px;
  top: 18px;
  padding: 0 20px;
  height: 38px;
  border-radius: 999px;
  background: var(--dl-lime);
  color: var(--dl-text);
  font-size: 12px;
  display: flex;
  align-items: center;
}
.section-label {
  display: block;
  margin: 24px 0 9px;
  font-size: 13px;
  color: var(--dl-text-secondary);
}
.pills {
  display: flex;
  gap: 10px;
  margin-bottom: 18px;
}
.wish {
  height: 110px;
  margin-bottom: 12px;
  padding: 12px 14px;
  justify-content: flex-start;
}
.visual {
  width: 86px;
  height: 86px;
  border-radius: 20px;
  background: var(--dl-bg-alt);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  font-weight: 700;
  overflow: hidden;
}
.visual.soft {
  background: var(--dl-accent-soft);
}
.visual image {
  width: 100%;
  height: 100%;
}
.wish-info {
  align-self: stretch;
  padding: 6px 0 4px 18px;
  display: flex;
  flex-direction: column;
}
.wish-name {
  font-weight: 700;
  font-size: 16px;
}
.wish-meta {
  font-size: 12px;
  color: var(--dl-text-secondary);
  margin-top: 5px;
}
.wish-price {
  font-size: 15px;
  font-weight: 600;
  margin-top: auto;
}
</style>
