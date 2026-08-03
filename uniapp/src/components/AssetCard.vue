<template>
  <view
    class="asset-card card touch"
    role="button"
    tabindex="0"
    @click="$emit('select', asset.id)"
    @keyup.enter="$emit('select', asset.id)"
    @keyup.space="$emit('select', asset.id)"
  >
    <view class="visual" :class="{ soft: index % 2 === 0 }"
      ><image
        v-if="asset.coverImageUrl"
        :src="resolveMediaUrl(asset.coverImageUrl)"
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
import { resolveMediaUrl } from "@/services/media";
const props = withDefaults(defineProps<{ asset: Asset; index?: number }>(), {
  index: 0,
});
defineEmits<{ (e: "select", id: number): void }>();
const initials = computed(() => props.asset.name.slice(0, 2));
const money = (v?: number) => `¥ ${Number(v || 0).toLocaleString("zh-CN")}`;
</script>
<style scoped>
.asset-card {
  height: 100px;
  padding: var(--space-sm);
  justify-content: flex-start;
  margin-bottom: var(--space-sm);
  overflow: hidden;
}
.visual {
  width: 76px;
  height: 76px;
  flex: 0 0 auto;
  border-radius: 16px;
  background: var(--color-cyan-soft);
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  font-family: var(--font-display);
  font-size: var(--text-md);
  font-weight: 700;
}
.visual.soft {
  background: var(--color-accent-soft);
}
.visual image {
  width: 100%;
  height: 100%;
}
.content {
  align-self: stretch;
  min-width: 0;
  padding: var(--space-2xs) 0 var(--space-2xs) var(--space-md);
  display: flex;
  flex-direction: column;
}
.name {
  color: var(--color-ink);
  font-size: var(--text-base);
  line-height: 1.4;
  font-weight: 700;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.meta {
  margin-top: var(--space-2xs);
  color: var(--color-ink-2);
  font-size: var(--text-xs);
  line-height: 1.4;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
.price {
  margin-top: auto;
  font-family: var(--font-label);
  font-size: var(--text-sm);
  font-weight: 700;
  font-variant-numeric: tabular-nums;
}
</style>
