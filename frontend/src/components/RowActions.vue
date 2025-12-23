<template>
  <div class="dl-row-actions">
    <ElButton
      v-for="action in actions"
      :key="action.key"
      link
      :type="action.type"
      :disabled="action.disabled"
      @click="() => action.onClick()"
    >
      {{ action.label }}
    </ElButton>
    <slot />
  </div>
</template>

<script setup lang="ts">
import { ElButton } from 'element-plus'

export interface RowAction {
  key: string
  label: string
  type?: 'primary' | 'info' | 'success' | 'warning' | 'danger'
  disabled?: boolean
  onClick: () => void
}

withDefaults(
  defineProps<{
    actions: readonly RowAction[]
  }>(),
  { actions: () => [] }
)
</script>

<style scoped>
.dl-row-actions {
  display: inline-flex;
  align-items: center;
  gap: var(--dl-space-2);
  flex-wrap: wrap;
}
</style>
