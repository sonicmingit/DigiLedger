<template>
  <div v-if="loading" class="state-panel" aria-live="polite"><div class="state-spinner" />正在整理数据…</div>
  <div v-else-if="error" class="state-panel error-state"><strong>暂时无法加载</strong><span>{{ error }}</span><button class="text-button" @click="$emit('retry')">重新加载</button></div>
  <div v-else-if="empty" class="state-panel"><div class="empty-shape" /><strong>{{ emptyTitle }}</strong><span>{{ emptyText }}</span><slot name="empty-action" /></div>
  <slot v-else />
</template>
<script setup lang="ts">
/** Accessible reusable request boundary for loading, empty and error states. */
withDefaults(defineProps<{ loading: boolean; error?: string; empty?: boolean; emptyTitle?: string; emptyText?: string }>(), { error: '', empty: false, emptyTitle: '这里还没有内容', emptyText: '从第一条记录开始构建你的数字仓库。' })
defineEmits<{ retry: [] }>()
</script>
