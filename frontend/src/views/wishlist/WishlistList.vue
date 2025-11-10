<template>
  <div class="wishlist-page">
    <el-card class="mb">
      <div class="actions">
        <el-button type="primary" @click="openDialog()">新增心愿</el-button>
        <el-button @click="refresh" :loading="loading">刷新</el-button>
      </div>
    </el-card>
    <el-card>
      <el-table :data="items" stripe :loading="loading" empty-text="暂无心愿">
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="category" label="类别" width="120" />
        <el-table-column prop="expectedPrice" label="期望价" width="120">
          <template #default="{ row }">{{ row.expectedPrice ? '¥ ' + formatNumber(row.expectedPrice) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="plannedPlatform" label="平台" width="140" />
        <el-table-column prop="priority" label="优先级" width="100" />
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button link type="success" @click="convert(row.id)" :disabled="!!row.convertedAssetId">转为资产</el-button>
            <el-popconfirm title="确定删除该心愿？" @confirm="remove(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="visible" :title="current ? '编辑心愿' : '新增心愿'" width="520px" @closed="reset">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类别">
          <el-input v-model="form.category" />
        </el-form-item>
        <el-form-item label="品牌">
          <el-input v-model="form.brand" />
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="form.model" />
        </el-form-item>
        <el-form-item label="期望价">
          <el-input-number v-model="form.expectedPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="平台">
          <el-input v-model="form.plannedPlatform" />
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="form.link" placeholder="http(s)://" />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" placeholder="选择优先级">
            <el-option v-for="n in 5" :key="n" :label="n" :value="n" />
          </el-select>
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="form.notes" type="textarea" rows="3" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="visible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { fetchWishlist, createWishlist, updateWishlist, deleteWishlist, convertWishlist } from '@/api/wishlist'
import type { WishlistItem } from '@/types'

const items = ref<WishlistItem[]>([])
const loading = ref(false)
const visible = ref(false)
const saving = ref(false)
const current = ref<WishlistItem | null>(null)
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  category: '',
  brand: '',
  model: '',
  expectedPrice: undefined as number | undefined,
  plannedPlatform: '',
  link: '',
  priority: 3,
  notes: ''
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const refresh = async () => {
  loading.value = true
  try {
    items.value = await fetchWishlist()
  } finally {
    loading.value = false
  }
}

const openDialog = (item?: WishlistItem) => {
  current.value = item || null
  visible.value = true
  if (item) {
    Object.assign(form, {
      name: item.name,
      category: item.category || '',
      brand: item.brand || '',
      model: item.model || '',
      expectedPrice: item.expectedPrice,
      plannedPlatform: item.plannedPlatform || '',
      link: item.link || '',
      priority: item.priority || 3,
      notes: item.notes || ''
    })
  } else {
    reset()
  }
}

const reset = () => {
  Object.assign(form, {
    name: '',
    category: '',
    brand: '',
    model: '',
    expectedPrice: undefined,
    plannedPlatform: '',
    link: '',
    priority: 3,
    notes: ''
  })
}

const submit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      if (current.value) {
        await updateWishlist(current.value.id, form)
        ElMessage.success('更新成功')
      } else {
        await createWishlist(form)
        ElMessage.success('创建成功')
      }
      visible.value = false
      refresh()
    } finally {
      saving.value = false
    }
  })
}

const remove = async (id: number) => {
  await deleteWishlist(id)
  ElMessage.success('删除成功')
  refresh()
}

const convert = async (id: number) => {
  await convertWishlist(id)
  ElMessage.success('已转为资产')
  refresh()
}

const formatNumber = (value: number) => value.toFixed(2)

onMounted(refresh)
</script>

<style scoped>
.wishlist-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mb {
  margin-bottom: 8px;
}

.actions {
  display: flex;
  gap: 12px;
}
</style>
