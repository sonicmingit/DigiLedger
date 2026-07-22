<template>
  <view class="page detail"
    ><view v-if="loading" class="loading">加载物品详情…</view
    ><view v-else-if="error" class="error">{{ error }}</view
    ><template v-else-if="asset"
      ><view class="top"
        ><view class="back touch" @click="back"><view class="back-icon" /></view
        ><text class="title">物品详情</text
        ><view class="dots touch" @click="openMenu"><i /><i /><i /></view></view
      ><view class="hero"
        ><image
          v-if="asset.coverImageUrl"
          :src="asset.coverImageUrl"
          mode="aspectFill"
        /><text v-else>{{ heroInitials }}</text
        ><view class="status">{{ asset.status }}</view></view
      ><view class="headline"
        ><view
          ><text class="name">{{ asset.name }}</text
          ><text class="meta"
            >{{ asset.categoryName || "未分类" }} ·
            {{ asset.model || "未填写型号" }}</text
          ></view
        ><text class="price">{{ money(asset.totalCost) }}</text></view
      ><view class="card info"
        ><view
          ><text>购买日期</text
          ><text>{{ asset.purchaseDate || "--" }}</text></view
        ><view
          ><text>保修到期</text
          ><text>{{ asset.warrantyExpireDate || "--" }}</text></view
        ><view
          ><text>使用成本</text
          ><text>{{ money(asset.dailyCost) }} / 天</text></view
        ></view
      ><view class="card note"
        ><text class="label">备注</text
        ><text>{{ asset.notes || "暂无备注" }}</text></view
      ><view class="actions"
        ><button class="primary" @click="edit">编辑物品</button
        ><button class="primary lime" @click="changeStatus">
          变更状态
        </button></view
      ></template
    ></view
  >
</template>
<script setup lang="ts">
import { computed, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { api, type Asset } from "@/services/api";
const id = ref(0),
  asset = ref<Asset>(),
  loading = ref(true),
  error = ref("");
const heroInitials = computed(() => {
  const last = asset.value?.name.trim().split(/\s+/).pop() || "";
  const latin = last.replace(/[^A-Za-z]/g, "");
  return (latin || asset.value?.name || "--").slice(0, 2).toUpperCase();
});
const money = (n?: number) => `¥ ${Number(n || 0).toLocaleString("zh-CN")}`;
const back = () => uni.navigateBack(),
  edit = () => uni.navigateTo({ url: `/pages/assets/editor?id=${id.value}` });
async function load() {
  try {
    asset.value = await api.asset(id.value);
  } catch (e) {
    error.value = (e as Error).message;
  } finally {
    loading.value = false;
  }
}
function changeStatus() {
  uni.showActionSheet({
    itemList: ["使用中", "已闲置", "待出售", "已出售"],
    success: async (r) => {
      if (!asset.value) return;
      const updated = {
        ...asset.value,
        status: ["使用中", "已闲置", "待出售", "已出售"][r.tapIndex],
      } as any;
      await api.updateAsset(id.value, updated);
      asset.value.status = updated.status;
    },
  });
}
function openMenu() {
  uni.showActionSheet({
    itemList: ["查看购买记录", "记录出售", "删除物品"],
    success: (r) => {
      if (r.tapIndex === 0) return showPurchases();
      if (r.tapIndex === 1) return sell();
      remove();
    },
  });
}
function showPurchases() {
  const list = (asset.value?.purchases || []) as any[];
  uni.showModal({
    title: "购买记录",
    content: list.length
      ? list
          .map((p, i) => `${i + 1}. ${p.purchaseDate || ""} ${money(p.price)}`)
          .join("\n")
      : "暂无购买记录",
    showCancel: false,
  });
}
function sell() {
  uni.showModal({
    title: "记录出售",
    editable: true,
    placeholderText: "出售金额",
    success: (r) => {
      if (!r.confirm || !r.content) return;
      uni.showModal({
        title: "出售日期",
        editable: true,
        content: new Date().toISOString().slice(0, 10),
        success: async (d) => {
          if (!d.confirm) return;
          await api.sellAsset(id.value, {
            saleScope: "ASSET",
            salePrice: Number(r.content),
            saleDate: d.content,
            fee: 0,
            shippingCost: 0,
            otherCost: 0,
          });
          uni.showToast({ title: "出售已记录", icon: "success" });
          load();
        },
      });
    },
  });
}
function remove() {
  uni.showModal({
    title: "删除物品",
    content: "删除后无法恢复，确认继续？",
    confirmColor: "#d33",
    success: async (r) => {
      if (r.confirm) {
        await api.deleteAsset(id.value);
        back();
      }
    },
  });
}
onLoad((q) => {
  id.value = Number(q?.id);
  load();
});
</script>
<style scoped>
.detail {
  padding-bottom: 38px;
}
.top {
  height: 48px;
  display: flex;
  align-items: center;
}
.back {
  width: 40px;
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
  font-size: 22px;
  font-weight: 700;
}
.dots {
  margin-left: auto;
  gap: 4px;
}
.dots i {
  display: block;
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: var(--dl-text-secondary);
}
.hero {
  height: 245px;
  position: relative;
  border-radius: 0 0 28px 28px;
  background: var(--dl-accent-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-size: 52px;
  font-weight: 700;
}
.hero > image {
  width: 100%;
  height: 100%;
}
.status {
  position: absolute;
  right: 19px;
  top: 19px;
  min-width: 92px;
  height: 36px;
  padding: 0 16px;
  border-radius: 999px;
  background: var(--dl-black);
  color: #fff;
  font-size: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
}
.headline {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  margin: 26px 0 22px;
}
.name,
.meta {
  display: block;
}
.name {
  font-size: 24px;
  font-weight: 700;
}
.meta {
  font-size: 12px;
  color: var(--dl-text-secondary);
  margin-top: 5px;
}
.price {
  font-size: 18px;
  font-weight: 700;
}
.info {
  padding: 14px 18px;
}
.info view {
  display: flex;
  justify-content: space-between;
  padding: 8px 0;
  font-size: 12px;
}
.info view text:first-child {
  color: var(--dl-text-secondary);
}
.note {
  margin-top: 18px;
  padding: 16px 18px;
  font-size: 12px;
}
.label {
  display: block;
  color: var(--dl-text-secondary);
  margin-bottom: 5px;
}
.actions {
  display: flex;
  gap: 12px;
  margin-top: 22px;
}
.actions button {
  flex: 1;
}
</style>
