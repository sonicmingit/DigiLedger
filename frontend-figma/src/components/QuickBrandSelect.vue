<template>
  <div class="quick-select">
    <el-select :model-value="modelValue" clearable filterable placeholder="请选择品牌" @update:model-value="emit('update:modelValue', $event)">
      <el-option v-for="brand in brands" :key="brand.id" :label="brand.name" :value="brand.id" />
    </el-select>
    <button class="quick-add" type="button" aria-label="新增品牌" title="新增品牌" @click="open = true">＋</button>
  </div>
  <el-dialog v-model="open" title="新增品牌" width="420px" append-to-body>
    <el-form label-position="top"><el-form-item label="品牌名称" required><el-input v-model="form.name" placeholder="例如：Bambu Lab" @keyup.enter="save" /></el-form-item><el-form-item label="别名"><el-input v-model="form.alias" placeholder="可选" /></el-form-item></el-form>
    <template #footer><div class="quick-dialog-actions"><button class="secondary-button" @click="open=false">取消</button><PrimaryButton label="创建并选择" :loading="saving" @click="save" /></div></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createBrand } from '@/api/settings'
import type { BrandItem } from '@/types'
import PrimaryButton from './PrimaryButton.vue'

defineProps<{ modelValue?: number; brands: BrandItem[] }>()
const emit = defineEmits<{ 'update:modelValue': [value: number | undefined]; created: [brand: BrandItem] }>()
const open = ref(false); const saving = ref(false); const form = reactive({ name: '', alias: '' })
async function save() {
  if (!form.name.trim()) return ElMessage.warning('请输入品牌名称')
  saving.value = true
  try {
    const brand = { id: await createBrand({ name: form.name.trim(), alias: form.alias.trim() || undefined }), name: form.name.trim(), alias: form.alias.trim() || undefined }
    emit('created', brand); emit('update:modelValue', brand.id); open.value = false; form.name = ''; form.alias = ''
    ElMessage.success('品牌已新增并选中')
  } catch (error) { ElMessage.error((error as Error).message) } finally { saving.value = false }
}
</script>

<style scoped>
.quick-select{display:flex;align-items:center;min-width:0;gap:7px}.quick-select :deep(.el-select){display:block;flex:1 1 0;min-width:0;width:auto}.quick-select :deep(.el-select__wrapper){min-width:0}.quick-add{flex:none;width:36px;height:36px;padding:0;border:1px solid #dce3d5;border-radius:10px;background:#f2f7eb;color:#567819;font-size:21px;line-height:1;cursor:pointer;transition:.16s ease}.quick-add:hover{border-color:#94d72d;background:var(--dl-accent-soft);transform:translateY(-1px)}.quick-dialog-actions{display:flex;justify-content:flex-end;gap:10px}.quick-dialog-actions :deep(button){min-width:94px;height:36px;padding:0 14px;font-size:12px}
</style>
