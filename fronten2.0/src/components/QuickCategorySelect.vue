<template>
  <div class="quick-select">
    <el-cascader :model-value="modelValue" :options="categories" :props="{ checkStrictly: true, emitPath: false, label: 'name', value: 'id', children: 'children' }" clearable placeholder="请选择分类" @update:model-value="emit('update:modelValue', $event)" />
    <button class="quick-add" type="button" aria-label="新增分类" title="新增分类" @click="open = true">＋</button>
  </div>
  <el-dialog v-model="open" title="新增分类" width="440px" append-to-body>
    <el-form label-position="top"><el-form-item label="分类名称" required><el-input v-model="form.name" placeholder="例如：投影仪" @keyup.enter="save" /></el-form-item><el-form-item label="上级分类"><el-tree-select v-model="form.parentId" :data="categories" check-strictly clearable :props="{ label: 'name', value: 'id', children: 'children' }" placeholder="无（根分类）" /></el-form-item></el-form>
    <template #footer><div class="quick-dialog-actions"><button class="secondary-button" @click="open=false">取消</button><PrimaryButton label="创建并选择" :loading="saving" @click="save" /></div></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createCategory } from '@/api/settings'
import type { CategoryNode } from '@/types'
import PrimaryButton from './PrimaryButton.vue'

defineProps<{ modelValue?: number; categories: CategoryNode[] }>()
const emit = defineEmits<{ 'update:modelValue': [value: number | undefined]; created: [category: CategoryNode] }>()
const open = ref(false); const saving = ref(false); const form = reactive<{ name: string; parentId?: number }>({ name: '' })
async function save() {
  if (!form.name.trim()) return ElMessage.warning('请输入分类名称')
  saving.value = true
  try {
    const category = { id: await createCategory({ name: form.name.trim(), parentId: form.parentId, sort: 0 }), name: form.name.trim(), parentId: form.parentId || null, level: 1, sort: 0, children: [] }
    emit('created', category); emit('update:modelValue', category.id); open.value = false; form.name = ''; form.parentId = undefined
    ElMessage.success('分类已新增并选中')
  } catch (error) { ElMessage.error((error as Error).message) } finally { saving.value = false }
}
</script>

<style scoped>
.quick-select{display:flex;align-items:center;min-width:0;gap:7px}.quick-select :deep(.el-cascader){display:block;flex:1 1 0;min-width:0;width:100%}.quick-select :deep(.el-cascader .el-input){width:100%}.quick-add{flex:none;width:36px;height:36px;padding:0;border:1px solid #dce3d5;border-radius:10px;background:#f2f7eb;color:#567819;font-size:21px;line-height:1;cursor:pointer;transition:.16s ease}.quick-add:hover{border-color:#94d72d;background:var(--dl-accent-soft);transform:translateY(-1px)}.quick-dialog-actions{display:flex;justify-content:flex-end;gap:10px}.quick-dialog-actions :deep(button){min-width:94px;height:36px;padding:0 14px;font-size:12px}
</style>
