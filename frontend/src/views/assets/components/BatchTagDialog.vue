<template>
  <el-dialog v-model="visible" title="批量设置标签" width="420px" @closed="reset">
    <el-form label-width="0">
      <el-form-item>
        <div class="tag-selector">
          <el-tree-select
            v-model="selected"
            :data="tagOptions"
            :props="treeProps"
            multiple
            show-checkbox
            filterable
            style="width: 100%"
            placeholder="选择应用到的标签"
          />
          <el-button class="inline-action" text size="small" type="primary" @click="handleCreateTag">
            新建
          </el-button>
        </div>
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
import { ElMessage } from 'element-plus'
import { useDictionaries } from '@/composables/useDictionaries'
import { useDictionaryCreator } from '@/composables/useDictionaryCreator'
import type { TagNode } from '@/api/dict'

const emit = defineEmits<{
  (e: 'confirm', tags: number[]): void
}>()

const visible = ref(false)
const loading = ref(false)
const selected = ref<number[]>([])

const { load: loadDicts, tagTree } = useDictionaries()
const { promptTagCreation } = useDictionaryCreator()

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

const handleCreateTag = async () => {
  try {
    const result = await promptTagCreation()
    if (result) {
      selected.value = Array.from(new Set([...selected.value, result.id]))
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '创建标签失败')
  }
}

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
.tag-selector {
  display: flex;
  gap: 8px;
  align-items: center;
}

.inline-action {
  padding: 0 6px;
}
</style>
