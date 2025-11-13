<template>
  <el-dialog v-model="visible" title="批量设置标签" width="420px" @closed="reset">
    <el-form label-width="0">
      <el-form-item>
        <el-tree-select
          v-model="selected"
          :data="tagOptions"
          :props="treeProps"
          multiple
          show-checkbox
          collapse-tags
          filterable
          style="width: 100%"
          placeholder="选择应用到的标签"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="confirm">应用标签</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useDictionaries } from '@/composables/useDictionaries'
import type { TagNode } from '@/api/dict'

const emit = defineEmits<{
  (e: 'confirm', tags: number[]): void
}>()

const visible = ref(false)
const loading = ref(false)
const selected = ref<number[]>([])

const { load: loadDicts, tagTree } = useDictionaries()

const treeProps = {
  value: 'value',
  label: 'label',
  children: 'children'
}

const buildOptions = (nodes: TagNode[]): any[] =>
  nodes.map((node) => ({
    value: node.id,
    label: node.name,
    children: node.children ? buildOptions(node.children) : []
  }))

const tagOptions = computed(() => buildOptions(tagTree.value))

const open = async (defaultValues: number[] = []) => {
  await loadDicts()
  selected.value = [...defaultValues]
  visible.value = true
}

const confirm = () => {
  loading.value = true
  emit('confirm', [...selected.value])
}

const reset = () => {
  selected.value = []
  loading.value = false
}

const setLoading = (value: boolean) => {
  loading.value = value
}

const close = () => {
  visible.value = false
}

defineExpose({ open, setLoading, close })
</script>

<style scoped>
</style>
