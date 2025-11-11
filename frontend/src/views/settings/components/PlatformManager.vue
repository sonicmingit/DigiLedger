<template>
  <div class="platform-manager" v-loading="loading">
    <div class="toolbar">
      <el-button type="primary" @click="openCreate">新增平台</el-button>
      <el-button @click="refreshDicts">刷新</el-button>
    </div>
    <el-table :data="platforms" stripe empty-text="暂无平台">
      <el-table-column prop="name" label="平台名称" min-width="160" />
      <el-table-column label="链接" min-width="200">
        <template #default="{ row }">
          <el-link v-if="row.link" :href="row.link" target="_blank" type="primary">{{ row.link }}</el-link>
          <span v-else>-</span>
        </template>
      </el-table-column>
      <el-table-column prop="sort" label="排序" width="100" />
      <el-table-column label="操作" width="160">
        <template #default="{ row }">
          <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
          <el-popconfirm title="确认删除该平台？" @confirm="remove(row.id)">
            <template #reference>
              <el-button link type="danger">删除</el-button>
            </template>
          </el-popconfirm>
        </template>
      </el-table-column>
    </el-table>
    <el-dialog v-model="dialogVisible" :title="isEdit ? '编辑平台' : '新增平台'" width="480px" @closed="reset">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px" status-icon>
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" placeholder="平台名称" />
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="form.link" placeholder="https://" />
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
import { reactive, ref, computed } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { createPlatform, updatePlatform, deletePlatform, type PlatformItem } from '@/api/dict'
import { useDictionaries } from '@/composables/useDictionaries'

const { platforms: platformList, refresh: refreshDicts, load: loadDicts } = useDictionaries()

const platforms = computed(() => platformList.value)
const loading = ref(false)
const saving = ref(false)
const dialogVisible = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

const form = reactive({
  id: 0,
  name: '',
  link: '',
  sort: 0
})

const rules = {
  name: [{ required: true, message: '请输入平台名称', trigger: 'blur' }]
}

const reset = () => {
  form.id = 0
  form.name = ''
  form.link = ''
  form.sort = nextSort()
}

const nextSort = () => platforms.value.reduce((max, item) => Math.max(max, item.sort ?? 0), 0) + 10

const openCreate = () => {
  isEdit.value = false
  reset()
  dialogVisible.value = true
}

const openEdit = (item: PlatformItem) => {
  isEdit.value = true
  form.id = item.id
  form.name = item.name
  form.link = item.link || ''
  form.sort = item.sort ?? 0
  dialogVisible.value = true
}

const submit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (isEdit.value) {
        await updatePlatform(form.id, { name: form.name, link: form.link || undefined, sort: form.sort })
        ElMessage.success('平台已更新')
      } else {
        await createPlatform({ name: form.name, link: form.link || undefined, sort: form.sort })
        ElMessage.success('平台已创建')
      }
      dialogVisible.value = false
      await refreshDicts()
    } catch (err: any) {
      ElMessage.error(err.message || '保存失败')
    } finally {
      saving.value = false
    }
  })
}

const remove = async (id: number) => {
  loading.value = true
  try {
    await deletePlatform(id)
    ElMessage.success('已删除平台')
    await refreshDicts()
  } catch (err: any) {
    ElMessage.error(err.message || '删除失败')
  } finally {
    loading.value = false
  }
}

loadDicts()
reset()
</script>

<style scoped>
.platform-manager {
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
</style>
