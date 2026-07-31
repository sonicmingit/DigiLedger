<template>
  <view
    class="page-back touch"
    role="button"
    tabindex="0"
    aria-label="返回上一级"
    @click="goBack"
    @keyup.enter="goBack"
    @keyup.space="goBack"
  >
    <view class="page-back__chevron" aria-hidden="true" />
  </view>
</template>

<script setup lang="ts">
import { returnToPreviousPage } from "@/utils/navigation";

const props = withDefaults(defineProps<{ fallback?: string }>(), {
  fallback: "/pages/assets/home",
});

function goBack() {
  returnToPreviousPage(props.fallback);
}
</script>

<style scoped>
.page-back {
  width: 28px;
  min-width: 28px;
  min-height: 28px;
  height: 28px;
  flex: 0 0 28px;
  border: 0;
  border-radius: 9px;
  background: var(--color-paper-2);
  box-shadow: none;
}

.page-back__chevron {
  width: 8px;
  height: 8px;
  margin-left: 2px;
  border-bottom: 1.5px solid var(--color-ink);
  border-left: 1.5px solid var(--color-ink);
  transform: rotate(45deg);
}
</style>
