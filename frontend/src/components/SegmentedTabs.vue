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
.dl-segmented :deep(.el-radio-button__inner) {
  border-radius: var(--dl-radius-md);
  border: 1px solid var(--el-border-color-light);
  background: transparent;
  color: var(--dl-text);
}

.dl-segmented :deep(.el-radio-button__inner:hover) {
  color: var(--dl-accent);
}

.dl-segmented :deep(.el-radio-button.is-active .el-radio-button__inner) {
  background: var(--dl-accent-soft);
  border-color: var(--dl-accent);
  color: var(--dl-accent);
  box-shadow: none;
}
</style>
