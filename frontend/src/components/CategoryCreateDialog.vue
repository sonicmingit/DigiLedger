<template>
  <el-dialog
    v-model="visible"
    title="新建类别"
    width="680px"
    destroy-on-close
    @closed="handleClosed"
    @open="handleOpen"
  >
    <div class="category-create-dialog" v-loading="loading">
      <div class="tree-panel">
        <el-input
          v-model="filterText"
          placeholder="搜索类别"
          clearable
          size="small"
          class="tree-search"
        />
        <el-button text type="primary" class="select-root" @click="selectRoot">
          选择为根类别
        </el-button>
        <el-tree
          ref="treeRef"
          :data="categoryTree"
          node-key="id"
          :props="treeProps"
          highlight-current
          :filter-node-method="filterNode"
          default-expand-all
          empty-text="暂无类别"
          @node-click="handleNodeClick"
        />
      </div>
      <div class="form-panel">
        <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" status-icon>
          <el-form-item label="父级类别">
            <el-input :model-value="parentLabel" disabled />
          </el-form-item>
          <el-form-item label="子类名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入子类名称" maxlength="50" show-word-limit />
          </el-form-item>
        </el-form>
      </div>
    </div>
    <template #footer>
      <el-button @click="close">取消</el-button>
      <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, nextTick, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormItemRule, FormRules } from 'element-plus'
import { useDictionaries } from '@/composables/useDictionaries'
import { createCategory, type CategoryNode } from '@/api/dict'

const props = defineProps<{ modelValue: boolean; defaultParentId?: number | null }>()

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'success', payload: { id: number; parentId: number | null; name: string }): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value) => emit('update:modelValue', value)
})

const { load: loadDicts, refresh: refreshDicts, categoryTree, categoryPathMap } = useDictionaries()

const formRef = ref<FormInstance>()
const treeRef = ref<any>()
const filterText = ref('')
const form = reactive({ name: '' })
const saving = ref(false)
const loading = ref(false)
const selectedParentId = ref<number | null>(null)

const treeProps = { children: 'children', label: 'name' }

const parentLabel = computed(() => {
  if (selectedParentId.value == null) {
    return '根类别'
  }
  return categoryPathMap.value.get(selectedParentId.value) ?? '未找到类别'
})

const getSiblings = (parentId: number | null): CategoryNode[] => {
  if (parentId == null) {
    return categoryTree.value
  }
  const stack = [...categoryTree.value]
  while (stack.length) {
    const node = stack.pop()!
    if (node.id === parentId) {
      return node.children || []
    }
    if (node.children?.length) {
      stack.push(...node.children)
    }
  }
  return []
}

const nextSort = (nodes: CategoryNode[]) =>
  nodes.reduce((max, item) => Math.max(max, item.sort ?? 0), 0) + 10

const validateUnique: NonNullable<FormItemRule['validator']> = (_rule, value, callback) => {
  const trimmed = typeof value === 'string' ? value.trim() : ''
  if (!trimmed) {
    callback(new Error('请输入子类名称'))
    return
  }
  const siblings = getSiblings(selectedParentId.value)
  if (siblings.some((item) => item.name === trimmed)) {
    callback(new Error('同父级下已存在该名称'))
    return
  }
  callback()
}

const rules: FormRules = {
  name: [
    { required: true, message: '请输入子类名称', trigger: 'blur' },
    { validator: validateUnique, trigger: ['blur', 'change'] }
  ]
}

const filterNode = (value: string, data: CategoryNode) => {
  if (!value) return true
  return data.name.toLowerCase().includes(value.toLowerCase())
}

const setTreeCurrent = async (id: number | null) => {
  await nextTick()
  const tree = treeRef.value
  if (!tree) return
  if (id == null) {
    tree.setCurrentKey(null)
    return
  }
  tree.setCurrentKey(id)
  const current = tree.getNode?.(id)
  let parent = current?.parent
  while (parent && parent !== tree.root) {
    parent.expanded = true
    parent = parent.parent
  }
}

const handleNodeClick = (node: CategoryNode) => {
  selectedParentId.value = node.id
}

const selectRoot = () => {
  selectedParentId.value = null
}

const close = () => {
  visible.value = false
}

const resetForm = () => {
  form.name = ''
  filterText.value = ''
  formRef.value?.clearValidate()
}

const handleOpen = async () => {
  resetForm()
  loading.value = true
  try {
    await loadDicts()
    selectedParentId.value = props.defaultParentId ?? null
    await setTreeCurrent(selectedParentId.value)
  } finally {
    loading.value = false
  }
}

const handleClosed = () => {
  selectedParentId.value = null
  resetForm()
}

const submit = async () => {
  if (!formRef.value) return
  try {
    await formRef.value.validate()
  } catch {
    return
  }
  const trimmedName = form.name.trim()
  const siblings = getSiblings(selectedParentId.value)
  saving.value = true
  try {
    const id = await createCategory({
      name: trimmedName,
      parentId: selectedParentId.value,
      sort: nextSort(siblings)
    })
    await refreshDicts()
    ElMessage.success('类别已创建')
    emit('success', { id, parentId: selectedParentId.value, name: trimmedName })
    close()
  } catch (error: any) {
    ElMessage.error(error?.message || '创建类别失败')
  } finally {
    saving.value = false
  }
}

watch(
  () => props.defaultParentId,
  async (value) => {
    if (!visible.value) return
    selectedParentId.value = value ?? null
    await setTreeCurrent(selectedParentId.value)
    formRef.value?.validateField('name').catch(() => undefined)
  }
)

watch(filterText, (value) => {
  treeRef.value?.filter(value.trim())
})

watch(categoryTree, async () => {
  if (!visible.value) return
  await setTreeCurrent(selectedParentId.value)
})

watch(selectedParentId, () => {
  if (!visible.value) return
  formRef.value?.validateField('name').catch(() => undefined)
})
</script>

<style scoped>
.category-create-dialog {
  display: flex;
  gap: 16px;
  min-height: 360px;
}

.tree-panel {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-width: 220px;
}

.tree-panel .el-tree {
  flex: 1;
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 8px;
  padding: 8px;
}

.tree-search {
  width: 100%;
}

.select-root {
  align-self: flex-start;
  padding: 0;
}

.form-panel {
  flex: 1;
  background: rgba(15, 23, 42, 0.4);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 16px;
}
</style>
