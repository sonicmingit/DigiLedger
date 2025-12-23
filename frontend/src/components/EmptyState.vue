<template>
  <div class="dl-empty">
    <ElEmpty :description="description">
      <template v-if="actionText" #default>
        <ElButton type="primary" @click="$emit('action')">{{ actionText }}</ElButton>
      </template>
    </ElEmpty>
  </div>
</template>

<script setup lang="ts">
import { ElButton, ElEmpty } from 'element-plus'

defineProps<{
  description: string
  actionText?: string
}>()

defineEmits<{ action: [] }>()
</script>

<style scoped>
.dl-empty {
  padding: var(--dl-space-6) 0;
}
</style>

