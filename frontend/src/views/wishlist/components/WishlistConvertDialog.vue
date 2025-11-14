<template>
  <el-dialog v-model="visible" title="购买记录" width="560px" @closed="reset">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px" status-icon>
      <el-form-item label="物品名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入物品名称" />
      </el-form-item>
      <el-form-item label="物品类别" prop="categoryId">
        <el-tree-select
          v-model="form.categoryId"
          :data="categoryOptions"
          :props="treeProps"
          check-strictly
          filterable
          placeholder="请选择类别（叶子节点）"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="品牌">
        <div class="brand-field">
          <el-select
            v-model="form.brandId"
            filterable
            clearable
            placeholder="选择品牌"
            @change="handleBrandSelect"
          >
            <el-option v-for="item in brandOptions" :key="item.id" :label="item.label" :value="item.id" />
          </el-select>
          <el-button
            class="inline-action"
            text
            size="small"
            type="primary"
            @click="handleCreateBrand"
          >
            新建
          </el-button>
        </div>
      </el-form-item>
      <el-form-item label="型号">
        <el-input v-model="form.model" placeholder="型号（可选）" />
      </el-form-item>
      <el-form-item label="启用日期" prop="enabledDate">
        <el-date-picker v-model="form.enabledDate" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="标签">
        <el-tree-select
          v-model="form.tagIds"
          :data="tagOptions"
          :props="treeProps"
          multiple
          show-checkbox
          filterable
          placeholder="选择标签"
          class="tag-tree-select"
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.notes" type="textarea" rows="3" placeholder="可补充说明" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">创建物品</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import type { FormInstance } from 'element-plus'
import { ElMessage, ElMessageBox } from 'element-plus'
import { convertWishlist } from '@/api/wishlist'
import type { WishlistItem } from '@/types'
import type { CategoryNode, TagNode } from '@/api/dict'
import { useDictionaries } from '@/composables/useDictionaries'
import { extractObjectKey } from '@/utils/storage'
import { createBrand } from '@/api/dict'

const emit = defineEmits<{ (e: 'success'): void }>()

const visible = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()
const current = ref<WishlistItem | null>(null)

const today = () => new Date().toISOString().slice(0, 10)

const form = reactive({
  name: '',
  categoryId: null as number | null,
  brandId: null as number | null,
  brandName: '',
  model: '',
  enabledDate: today(),
  tagIds: [] as number[],
  notes: '',
  imageKey: ''
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择类别', trigger: 'change' }],
  enabledDate: [{ required: true, message: '请选择启用日期', trigger: 'change' }]
}

const { load: loadDicts, categoryTree, tagTree, brands, brandMap } = useDictionaries()

const brandOptions = computed(() =>
  brands.value.map((item) => ({
    id: item.id,
    label: ((item.alias && item.alias.trim()) || item.name || '').trim()
  }))
)

const resolveBrandNameFromItem = (item: WishlistItem) => {
  if (item.brandName && item.brandName.trim().length) {
    return item.brandName.trim()
  }
  if (item.brandId) {
    const brand = brandMap.value.get(item.brandId)
    if (brand) {
      const alias = brand.alias?.trim()
      if (alias) return alias
      if (brand.name) return brand.name
    }
  }
  return ''
}

const normalizeBrandName = () => {
  form.brandName = form.brandName.trim()
  if (form.brandId && !form.brandName) {
    const brand = brandMap.value.get(form.brandId)
    if (brand) {
      form.brandName = (brand.alias?.trim() || brand.name || '').trim()
    }
  }
}

const handleBrandSelect = (id: number | null) => {
  if (!id) {
    form.brandId = null
    normalizeBrandName()
    return
  }
  const brand = brandMap.value.get(id)
  if (brand) {
    form.brandName = (brand.alias?.trim() || brand.name || '').trim()
  }
}

const handleCreateBrand = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入品牌名称', '新建品牌', {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：Apple'
    })
    const name = (value || '').trim()
    if (!name) return
    const id = await createBrand({ name })
    // 重新加载字典数据以包含新品牌
    await loadDicts()
    form.brandId = id
    form.brandName = name
    ElMessage.success('品牌已创建并回选')
  } catch (err) {
    if (err === 'cancel' || err === 'close') return
    ElMessage.error('创建品牌失败')
  }
}

const treeProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  disabled: 'disabled'
}

const buildCategoryOptions = (nodes: CategoryNode[]): any[] =>
  nodes.map((node) => ({
    value: node.id,
    label: node.name,
    disabled: node.children?.length ? true : false,
    children: node.children ? buildCategoryOptions(node.children) : []
  }))

const buildTagOptions = (nodes: TagNode[]): any[] =>
  nodes.map((node) => ({
    value: node.id,
    label: node.name,
    children: node.children ? buildTagOptions(node.children) : []
  }))

const categoryOptions = computed(() => buildCategoryOptions(categoryTree.value))
const tagOptions = computed(() => buildTagOptions(tagTree.value))

const open = (item: WishlistItem) => {
  current.value = item
  form.name = item.name
  form.brandId = item.brandId ?? null
  form.brandName = resolveBrandNameFromItem(item)
  normalizeBrandName()
  form.model = item.model || ''
  form.notes = item.notes || ''
  form.categoryId = item.categoryId ?? null
  form.tagIds = item.tags ? item.tags.map((tag) => tag.id) : []
  form.enabledDate = today()
  form.imageKey = extractObjectKey(item.imageUrl)
  visible.value = true
}

const reset = () => {
  current.value = null
  form.name = ''
  form.brandId = null
  form.brandName = ''
  form.model = ''
  form.notes = ''
  form.categoryId = null
  form.tagIds = []
  form.enabledDate = today()
  form.imageKey = ''
}

const submit = () => {
  if (!current.value) return
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      normalizeBrandName()
      const brandText = form.brandName.trim()
      await convertWishlist(current.value!.id, {
        name: form.name,
        categoryId: form.categoryId!,
        brandId: form.brandId || undefined,
        brand: brandText || undefined,
        model: form.model || undefined,
        status: '使用中',
        enabledDate: form.enabledDate,
        notes: form.notes || undefined,
        tagIds: form.tagIds,
        coverImageUrl: form.imageKey || undefined,
        purchases: []
      })
      ElMessage.success('已转为物品')
      visible.value = false
      emit('success')
    } finally {
      loading.value = false
    }
  })
}

defineExpose({ open })

onMounted(async () => {
  await loadDicts()
})
</script>

<style scoped>
.brand-field {
  display: flex;
  gap: 8px;
  align-items: center;
}

.brand-field :deep(.el-select) {
  width: 180px;
}

.brand-field :deep(.el-input) {
  flex: 1;
  min-width: 0;
}

.tag-tree-select {
  max-height: 200px;
  overflow-y: auto;
}

/* 显示所有已选标签并允许换行 */
.tag-tree-select :deep(.el-select__tags),
.tag-tree-select :deep(.el-input__inner) {
  flex-wrap: wrap !important;
  white-space: normal !important;
}
.tag-tree-select :deep(.el-tag) {
  display: inline-block !important;
  margin: 4px 6px !important;
  white-space: normal !important;
}

/* 新增：把对话框关闭 X 固定在标题右上角（防止被内容挤到下方） */
:deep(.el-dialog__header) {
  display: flex;
  align-items: center;
  justify-content: center; /* 标题居中 */
  position: relative;
  padding-right: 56px; /* 给关闭按钮留出空间 */
}
:deep(.el-dialog__title) {
  flex: 1;
  text-align: center;
}
:deep(.el-dialog__close-btn) {
  position: absolute !important;
  right: 12px !important;
  top: 8px !important;
  z-index: 20 !important;
}

@media (max-width: 768px) {
  .brand-field {
    flex-direction: column;
    align-items: stretch;
  }

  .brand-field :deep(.el-select) {
    width: 100%;
  }
}
</style>
