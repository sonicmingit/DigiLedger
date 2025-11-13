<template>
  <div class="brand-manager" v-loading="loading">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新增品牌</el-button>
      <el-button @click="refresh">刷新</el-button>
    </div>
    <el-table :data="brandList" stripe empty-text="暂无品牌">
      <el-table-column prop="name" label="品牌名称" min-width="160" />
      <el-table-column prop="alias" label="展示名称" min-width="160">
        <template #default="{ row }">{{ row.alias || '-' }}</template>
      </el-table-column>
      <el-table-column prop="initial" label="首字母" width="120">
        <template #default="{ row }">{{ row.initial || '-' }}</template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column label="操作" width="200">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确认删除该品牌？" @confirm="remove(row.id)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑品牌' : '新增品牌'" width="520px" @closed="reset">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px" status-icon>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="品牌名称" />
        </el-form-item>
        <el-form-item label="展示名称">
          <el-input v-model="form.alias" placeholder="别名（可选）" />
        </el-form-item>
        <el-form-item label="首字母">
          <el-input v-model="form.initial" maxlength="10" placeholder="例如：SONY" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :step="5" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { useDictionaries } from '@/composables/useDictionaries'
import { createBrand, updateBrand, deleteBrand, type BrandItem } from '@/api/dict'

const { brands, refresh: refreshDicts, load: loadDicts } = useDictionaries()

const brandList = computed(() => brands.value)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: 0,
  name: '',
  alias: '',
  initial: '',
  sort: 0
})

const rules = {
  name: [{ required: true, message: '请输入品牌名称', trigger: 'blur' }]
}

const reset = () => {
  form.id = 0
  form.name = ''
  form.alias = ''
  form.initial = ''
  form.sort = nextSort()
  formRef.value?.clearValidate()
}

const nextSort = () =>
  brandList.value.reduce((max, item) => Math.max(max, item.sort ?? 0), 0) + 10

const openCreate = () => {
  isEdit.value = false
  reset()
  dialogVisible.value = true
}

const openEdit = (item: BrandItem) => {
  isEdit.value = true
  form.id = item.id
  form.name = item.name
  form.alias = item.alias || ''
  form.initial = item.initial || ''
  form.sort = item.sort ?? 0
  dialogVisible.value = true
}

const submit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (isEdit.value) {
        await updateBrand(form.id, {
          name: form.name,
          alias: form.alias || undefined,
          initial: form.initial || undefined,
          sort: form.sort
        })
        ElMessage.success('品牌已更新')
      } else {
        await createBrand({
          name: form.name,
          alias: form.alias || undefined,
          initial: form.initial || undefined,
          sort: form.sort
        })
        ElMessage.success('品牌已创建')
      }
      dialogVisible.value = false
      await refreshDicts()
    } catch (error: any) {
      ElMessage.error(error?.message || '保存失败')
    } finally {
      saving.value = false
    }
  })
}

const remove = async (id: number) => {
  loading.value = true
  try {
    await deleteBrand(id)
    ElMessage.success('已删除品牌')
    await refreshDicts()
  } catch (error: any) {
    ElMessage.error(error?.message || '删除失败')
  } finally {
    loading.value = false
  }
}

const refresh = async () => {
  loading.value = true
  try {
    await refreshDicts()
  } finally {
    loading.value = false
  }
}

loadDicts()
reset()
</script>

<style scoped>
.brand-manager {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.2);
  border-radius: 12px;
  padding: 16px;
}

.toolbar {
  display: flex;
  gap: 12px;
  margin-bottom: 16px;
}

.brand-manager :deep(.el-table) {
  border-radius: 8px;
  overflow: hidden;
}
</style>
