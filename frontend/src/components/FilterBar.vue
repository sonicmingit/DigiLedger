<template>
  <ElCard class="dl-filter-bar" shadow="never">
    <div class="dl-filter-bar__inner">
      <div class="dl-filter-bar__fields">
        <slot />
      </div>
      <div v-if="$slots.actions" class="dl-filter-bar__actions">
        <slot name="actions" />
      </div>
    </div>
  </ElCard>
</template>

<script setup lang="ts">
import { ElCard } from 'element-plus'
</script>

<style scoped>
.dl-filter-bar {
  border-radius: var(--dl-radius-md);
  border: 1px solid var(--el-border-color-light);
  background: var(--dl-card);
}

.dl-filter-bar :deep(.el-input__wrapper),
.dl-filter-bar :deep(.el-select .el-input__wrapper),
.dl-filter-bar :deep(.el-cascader .el-input__wrapper),
.dl-filter-bar :deep(.el-date-editor .el-input__wrapper),
.dl-filter-bar :deep(.el-input-number .el-input__wrapper),
.dl-filter-bar :deep(.el-select__wrapper) {
  background-color: var(--el-fill-color-blank);
}

.dl-filter-bar__inner {
  display: flex;
  gap: var(--dl-space-4);
  align-items: flex-end;
  flex-wrap: wrap;
}

.dl-filter-bar__fields {
  display: flex;
  gap: var(--dl-space-3);
  align-items: flex-end;
  flex-wrap: wrap;
  flex: 1;
  min-width: 0;
}

.dl-filter-bar__actions {
  display: flex;
  gap: var(--dl-space-3);
  align-items: center;
  margin-left: auto;
}
</style>
