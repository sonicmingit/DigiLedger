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
  gap: var(--space-md);
  margin-bottom: var(--space-md);
}
.title,
.subtitle {
  display: block;
}
.title {
  color: var(--color-ink);
  font-family: var(--font-display);
  font-size: var(--text-xl);
  line-height: 1.12;
  font-weight: 700;
  letter-spacing: -0.025em;
  overflow-wrap: anywhere;
}
.subtitle {
  margin-top: var(--space-2xs);
  color: var(--color-ink-2);
  font-size: var(--text-sm);
  line-height: 1.5;
}
</style>
