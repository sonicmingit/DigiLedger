<template>
  <view class="asset-card card touch" @click="$emit('select', asset.id)"
    ><view class="visual" :class="{ soft: index % 2 === 0 }"
      ><image
        v-if="asset.coverImageUrl"
        :src="asset.coverImageUrl"
        mode="aspectFill"
      /><text v-else>{{ initials }}</text></view
    ><view class="content"
      ><text class="name">{{ asset.name }}</text
      ><text class="meta"
        >{{ asset.categoryName || asset.categoryPath || "未分类" }} ·
        {{ asset.status || "未设置" }}</text
      ><text class="price">{{
        money(asset.totalInvest ?? asset.totalCost ?? asset.currentValue)
      }}</text></view
    ></view
  >
</template>
<script setup lang="ts">
import { computed } from "vue";
import type { Asset } from "@/services/api";
const props = withDefaults(defineProps<{ asset: Asset; index?: number }>(), {
  index: 0,
});
defineEmits<{ (e: "select", id: number): void }>();
const initials = computed(() => props.asset.name.slice(0, 2));
const money = (v?: number) => `¥ ${Number(v || 0).toLocaleString("zh-CN")}`;
</script>
<style scoped>
.asset-card {
  height: 110px;
  padding: 12px 14px;
  justify-content: flex-start;
  margin-bottom: 12px;
}
.visual {
  width: 86px;
  height: 86px;
  flex: 0 0 auto;
  border-radius: 20px;
  background: var(--dl-bg-alt);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-size: 18px;
  font-weight: 700;
}
.visual.soft {
  background: var(--dl-accent-soft);
}
.visual image {
  width: 100%;
  height: 100%;
}
.content {
  align-self: stretch;
  min-width: 0;
  padding: 6px 0 4px 18px;
  display: flex;
  flex-direction: column;
}
.name {
  font-size: 16px;
  line-height: 23px;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.meta {
  margin-top: 5px;
  font-size: 12px;
  color: var(--dl-text-secondary);
}
.price {
  margin-top: auto;
  font-size: 15px;
  font-weight: 600;
}
</style>
