<template>
  <view class="header"
    ><view
      ><text class="title">{{ title }}</text
      ><text v-if="subtitle" class="subtitle">{{ subtitle }}</text></view
    ><slot
  /></view>
</template>
<script setup lang="ts">
defineProps<{ title: string; subtitle?: string }>();
</script>
<style scoped>
.header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  margin-bottom: 18px;
}
.title,
.subtitle {
  display: block;
}
.title {
  font-size: 28px;
  line-height: 35px;
  font-weight: 700;
  letter-spacing: -0.5px;
}
.subtitle {
  margin-top: 2px;
  font-size: 13px;
  color: var(--dl-text-secondary);
}
</style>
