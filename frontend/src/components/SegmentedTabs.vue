<template>
  <ElRadioGroup
    v-model="model"
    class="dl-segmented"
    :size="size"
    @change="handleChange"
  >
    <ElRadioButton v-for="item in items" :key="String(item.value)" :label="item.value">
      {{ item.label }}
    </ElRadioButton>
  </ElRadioGroup>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { ElRadioButton, ElRadioGroup } from 'element-plus'

type Size = 'large' | 'default' | 'small'
type SegmentedValue = string | number

export interface SegmentedItem<T = string | number> {
  label: string
  value: T
}

const props = withDefaults(
  defineProps<{
    modelValue: SegmentedValue
    items: SegmentedItem[]
    size?: Size
  }>(),
  { size: 'large' }
)

const emit = defineEmits<{
  'update:modelValue': [value: SegmentedValue]
  change: [value: SegmentedValue]
}>()

const model = computed({
  get: () => props.modelValue,
  set: (value: SegmentedValue | boolean) => {
    if (typeof value === 'boolean') return
    emit('update:modelValue', value)
  }
})

const handleChange = (value: SegmentedValue | boolean) => {
  if (typeof value === 'boolean') return
  emit('change', value)
}
</script>

<style scoped>
.dl-segmented {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  padding: 6px;
  border-radius: 999px;
  border: 1px solid color-mix(in srgb, var(--dl-accent) 18%, var(--el-border-color-light));
  background: color-mix(in srgb, var(--dl-accent) 6%, transparent);
}

.dl-segmented :deep(.el-radio-button__inner) {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 999px !important;
  border: 1px solid var(--el-border-color-light) !important;
  border-left: 1px solid var(--el-border-color-light) !important;
  background: transparent;
  color: var(--dl-text);
  box-shadow: none !important;
  transition: background-color 0.15s ease, color 0.15s ease, border-color 0.15s ease;
}

.dl-segmented :deep(.el-radio-button__inner:hover) {
  border-color: color-mix(in srgb, var(--dl-accent) 40%, var(--el-border-color-light)) !important;
  background: color-mix(in srgb, var(--dl-accent) 10%, transparent);
  color: var(--dl-accent);
}

.dl-segmented :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: var(--dl-accent);
  border-color: var(--dl-accent) !important;
  color: #fff;
}

.dl-segmented.el-radio-group--large :deep(.el-radio-button__inner) {
  height: 46px;
  padding: 0 18px;
  font-size: 16px;
}

.dl-segmented.el-radio-group--default :deep(.el-radio-button__inner) {
  height: 38px;
  padding: 0 14px;
  font-size: 14px;
}

.dl-segmented.el-radio-group--small :deep(.el-radio-button__inner) {
  height: 32px;
  padding: 0 12px;
  font-size: 12px;
}
</style>
