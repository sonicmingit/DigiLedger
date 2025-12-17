<template>
  <div class="wishlist-page">
    <PageHeader title="心愿单" subtitle="记录想买的数码物品，跟踪购买状态">
      <template #actions>
        <el-button type="primary" @click="openDialog()">
          <el-icon class="mr-1"><plus /></el-icon>新增心愿
        </el-button>
        <el-button @click="refresh" :loading="loading">刷新</el-button>
      </template>
      <FilterBar>
        <div class="wishlist-filter-stack">
          <SegmentedTabs
            v-model="activeStatus"
            :items="statusOptions"
            size="large"
            @change="handleStatusChange"
          />
          <el-input
            v-model="keyword"
            placeholder="搜索名称/品牌"
            clearable
            class="search-input"
          >
            <template #prefix>
              <el-icon><search /></el-icon>
            </template>
          </el-input>
        </div>
      </FilterBar>
    </PageHeader>
    <el-card>
      <el-table :data="filtered" stripe :loading="loading" empty-text="暂无心愿" row-key="id">
        <el-table-column label="图片" width="120">
          <template #default="{ row }">
            <el-image
              v-if="resolveOssUrl(row.imageUrl)"
              :src="resolveOssUrl(row.imageUrl)"
              fit="cover"
              style="width: 92px; height: 68px; border-radius: 12px"
              :preview-src-list="[resolveOssUrl(row.imageUrl)]"
            />
            <div v-else class="image-placeholder">无图</div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column label="类别" width="140">
          <template #default="{ row }">{{ resolveCategoryName(row) }}</template>
        </el-table-column>
        <el-table-column label="品牌" width="120">
          <template #default="{ row }">{{ resolveWishlistBrand(row) }}</template>
        </el-table-column>
        <el-table-column prop="expectedPrice" label="期望价" width="120">
          <template #default="{ row }">{{ row.expectedPrice ? '¥ ' + formatNumber(row.expectedPrice) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === '已购买' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360">
          <template #default="{ row }">
            <RowActions
              :actions="getRowActions(row)"
            >
              <el-button
                v-if="row.status === '已购买' && row.convertedAssetId"
                link
                type="success"
                @click="goToAssetDetail(row.convertedAssetId)"
              >
                物品详情
              </el-button>
              <el-popconfirm title="确定删除该心愿？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button link type="danger">删除</el-button>
                </template>
              </el-popconfirm>
            </RowActions>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <el-dialog v-model="visible" :title="current ? '编辑心愿' : '新增心愿'" width="560px" @closed="reset">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="名称" prop="name">
          <el-input v-model="form.name" />
        </el-form-item>
        <el-form-item label="类别">
          <div class="selector-with-action">
            <el-cascader
              v-model="form.categoryId"
              :options="categoryOptions"
              :props="cascaderProps"
              filterable
              clearable
              placeholder="选择类别"
              class="w-full"
            />
            <el-button class="inline-action" text size="small" type="primary" @click="openCategoryDialog">
              新建
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="品牌">
          <div class="selector-with-action">
            <el-select
              v-model="form.brandId"
              filterable
              clearable
              placeholder="选择品牌"
              class="w-full"
            >
              <el-option v-for="item in brandOptions" :key="item.id" :label="item.label" :value="item.id" />
            </el-select>
            <el-button class="inline-action" text size="small" type="primary" @click="handleCreateBrand">
              新建
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="型号">
          <el-input v-model="form.model" />
        </el-form-item>
        <el-form-item label="期望价">
          <el-input-number v-model="form.expectedPrice" :min="0" :precision="2" />
        </el-form-item>
        <el-form-item label="链接">
          <el-input v-model="form.link" placeholder="http(s)://" />
        </el-form-item>
        <el-form-item label="标签">
          <div class="selector-with-action">
            <el-cascader
              v-model="form.tagIds"
              :options="tagOptions"
              :props="cascaderProps"
              multiple
              clearable
              filterable
              placeholder="选择标签"
              class="w-full"
            />
            <el-button class="inline-action" text size="small" type="primary" @click="handleCreateTag">
              新建
            </el-button>
          </div>
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" placeholder="选择优先级">
            <el-option v-for="n in 5" :key="n" :label="n" :value="n" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片">
            <UnifiedUploader :http-request="handleUpload" accept="image/*" capture="environment" :show-file-list="false" />
          <img v-if="imagePreview" :src="imagePreview" class="preview" />
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
    <asset-form ref="assetFormRef" @success="handleAssetFormSuccess" />
    <wishlist-detail-drawer ref="detailDrawer" />
    <category-create-dialog
      v-model="categoryDialogVisible"
      :default-parent-id="form.categoryId ?? null"
      @success="handleCategoryCreated"
    />
  </div>
</template>

<script setup lang="ts">
 import { computed, reactive, ref, onMounted, watch } from 'vue'
import { ElMessage } from 'element-plus'
import UnifiedUploader from '@/components/UnifiedUploader.vue'
import type { FormInstance } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
 import PageHeader from '@/components/PageHeader.vue'
 import FilterBar from '@/components/FilterBar.vue'
 import SegmentedTabs from '@/components/SegmentedTabs.vue'
 import RowActions from '@/components/RowActions.vue'
 import { useRouter } from 'vue-router'
 import { fetchWishlist, createWishlist, updateWishlist, deleteWishlist, convertWishlist } from '@/api/wishlist'
 import type { WishlistItem } from '@/types'
import WishlistDetailDrawer from './components/WishlistDetailDrawer.vue'
import { uploadFile } from '@/api/file'
import { useDictionaries } from '@/composables/useDictionaries'
import { useDictionaryCreator } from '@/composables/useDictionaryCreator'
import type { CategoryNode, TagNode } from '@/api/dict'
import { buildOssUrl, extractObjectKey } from '@/utils/storage'
import AssetForm from '@/views/assets/components/AssetForm.vue'
import CategoryCreateDialog from '@/components/CategoryCreateDialog.vue'

const router = useRouter()

const items = ref<WishlistItem[]>([])
const filtered = ref<WishlistItem[]>([])
const loading = ref(false)
const visible = ref(false)
const saving = ref(false)
const current = ref<WishlistItem | null>(null)
const formRef = ref<FormInstance>()
 const keyword = ref('')
 const activeStatus = ref<'全部' | '未购买' | '已购买'>('未购买')
 const categoryDialogVisible = ref(false)

 const statusOptions = [
   { label: '全部', value: '全部' },
   { label: '未购买', value: '未购买' },
   { label: '已购买', value: '已购买' }
 ]

const form = reactive({
  name: '',
  categoryId: null as number | null,
  brandId: null as number | null,
  model: '',
  expectedPrice: undefined as number | undefined,
  link: '',
  notes: '',
  priority: 3,
  status: '未购买' as '未购买' | '已购买',
  imageKey: '',
  tagIds: [] as number[]
})

const imagePreview = computed(() => buildOssUrl(form.imageKey))

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const openCategoryDialog = () => {
  categoryDialogVisible.value = true
}

const handleCategoryCreated = (payload: { id: number }) => {
  form.categoryId = payload.id
}

const brandOptions = computed(() =>
  brands.value.map((item) => ({
    id: item.id,
    label: ((item.alias && item.alias.trim()) || item.name || '').trim()
  }))
)

const handleCreateBrand = async () => {
  try {
    const result = await promptBrandCreation()
    if (result) {
      form.brandId = result.id
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '创建品牌失败')
  }
}

const resolveWishlistBrand = (item: WishlistItem) => {
  // 兼容后端可能返回的 brand 字段（字符串）、brandName 或 brandId
  const explicit = ((item as any).brand && (item as any).brand.toString().trim()) || (item as any).brandName?.trim()
  if (explicit) return explicit
  if (item.brandId) {
    const brand = brandMap.value.get(item.brandId)
    if (brand) {
      const alias = brand.alias?.trim()
      if (alias) return alias
      if (brand.name) return brand.name
    }
  }
  return '-'
}

// 新增：从 categoryOptions 构建映射以通过 categoryId 回显名称
const categoryMap = computed(() => {
  const map = new Map<number, string>()
  const walk = (nodes: any[]) => {
    nodes.forEach((n) => {
      if (n && n.value != null) map.set(n.value, n.label)
      if (n && n.children) walk(n.children)
    })
  }
  walk(categoryOptions.value)
  return map
})

const resolveCategoryName = (item: WishlistItem) => {
  // 优先使用后端可能返回的 categoryName 或直接的 category 字段
  const explicit = (item as any).categoryName || (item as any).category
  if (explicit) return explicit
  if (item.categoryId) return categoryMap.value.get(item.categoryId) ?? '-'
  return '-'
}

const { load: loadDicts, categoryTree, tagTree, brands, brandMap } = useDictionaries()
const { promptBrandCreation, promptTagCreation } = useDictionaryCreator()

const treeProps = {
  value: 'value',
  label: 'label',
  children: 'children'
}

// cascader props: 返回最终节点的值而非路径数组
const cascaderProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  emitPath: false
}

const buildCategoryOptions = (nodes: CategoryNode[]): any[] =>
  nodes.map((node) => ({
    value: node.id,
    label: node.name,
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

const resolveOssUrl = (value?: string | null) => buildOssUrl(value)

const filterItems = () => {
  const keywordValue = keyword.value.trim().toLowerCase()
  filtered.value = items.value.filter((item) => {
    if (!keywordValue) return true
    const brandText = resolveWishlistBrand(item)
    const combined = `${item.name} ${brandText === '-' ? '' : brandText}`
    return combined.toLowerCase().includes(keywordValue)
  })
}

const refresh = async () => {
  loading.value = true
  try {
    const statusParam = activeStatus.value === '全部' ? undefined : activeStatus.value
    items.value = await fetchWishlist(statusParam ? { status: statusParam } : undefined)
    filterItems()
  } finally {
    loading.value = false
  }
}

const handleStatusChange = () => {
  refresh()
}

watch(keyword, () => {
  filterItems()
})

const openDialog = (item?: WishlistItem) => {
  current.value = item || null
  visible.value = true
  if (item) {
    Object.assign(form, {
      name: item.name,
      categoryId: item.categoryId ?? null,
      brandId: item.brandId ?? null,
      model: item.model || '',
      expectedPrice: item.expectedPrice,
      link: item.link || '',
      notes: item.notes || '',
      priority: item.priority || 3,
      status: item.status,
      imageKey: extractObjectKey(item.imageUrl),
      tagIds: item.tags ? item.tags.map((tag) => tag.id) : []
    })
  } else {
    reset()
  }
}

const reset = () => {
  Object.assign(form, {
    name: '',
    categoryId: null,
    brandId: null,
    model: '',
    expectedPrice: undefined,
    link: '',
    notes: '',
    priority: 3,
    status: '未购买',
    imageKey: '',
    tagIds: []
  })
}

const submit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    saving.value = true
    try {
      const payload = {
        name: form.name,
        categoryId: form.categoryId || undefined,
        brandId: form.brandId || undefined,
        model: form.model || undefined,
        expectedPrice: form.expectedPrice,
        link: form.link || undefined,
        notes: form.notes || undefined,
        priority: form.priority,
        status: form.status,
        imageUrl: extractObjectKey(form.imageKey) || form.imageKey || undefined,
        tagIds: form.tagIds
      }
      if (current.value) {
        await updateWishlist(current.value.id, payload)
        ElMessage.success('更新成功')
      } else {
        await createWishlist(payload)
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

const detailDrawer = ref<InstanceType<typeof WishlistDetailDrawer> | null>(null)
const assetFormRef = ref<InstanceType<typeof AssetForm> | null>(null)

const showDetail = (item: WishlistItem) => {
  detailDrawer.value?.open(item)
}

const resolveBrandNameForPrefill = (item: WishlistItem) => {
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

const convert = (item: WishlistItem) => {
  const prefill = {
    name: item.name,
    categoryId: item.categoryId ?? null,
    brandId: item.brandId ?? null,
    brandName: resolveBrandNameForPrefill(item),
    model: item.model || '',
    notes: item.notes || '',
    coverImageKey: extractObjectKey(item.imageUrl) || '',
    tagIds: item.tags ? item.tags.map((tag) => tag.id) : undefined
  }
  assetFormRef.value?.open(null, {
    prefill,
    submit: (payload) => convertWishlist(item.id, payload),
    successMessage: '已转为物品'
  })
}

const handleAssetFormSuccess = async () => {
  await refresh()
}

const goToAssetDetail = (assetId: number) => {
  router.push(`/assets/${assetId}`)
}

const getRowActions = (row: WishlistItem) => [
  { key: 'detail', label: '详情', type: 'info', onClick: () => showDetail(row) },
  { key: 'edit', label: '编辑', type: 'primary', onClick: () => openDialog(row) },
  {
    key: 'convert',
    label: '已购买',
    type: 'success',
    disabled: row.status === '已购买',
    onClick: () => convert(row)
  }
] as const

const handleUpload = async (options: any) => {
  try {
    const { objectKey, url } = await uploadFile(options.file)
    form.imageKey = url || objectKey || extractObjectKey(url) || ''
    ElMessage.success('上传成功')
    options.onSuccess(objectKey)
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
    options.onError(error)
  }
}

const handleCreateTag = async () => {
  try {
    const result = await promptTagCreation()
    if (result) {
      form.tagIds = Array.from(new Set([...form.tagIds, result.id]))
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '创建标签失败')
  }
}

const formatNumber = (value: number) => value.toFixed(2)

onMounted(async () => {
  await loadDicts()
  await refresh()
})
</script>

<style scoped>
.wishlist-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.wishlist-filter-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 12px;
  width: 100%;
}

.search-input {
  width: 360px;
  max-width: 100%;
}

.preview {
  width: 96px;
  height: 96px;
  border-radius: 12px;
  margin-left: 12px;
  object-fit: cover;
}

.selector-with-action {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inline-action {
  padding: 0 6px;
}

.image-placeholder {
  width: 92px;
  height: 68px;
  border-radius: 12px;
  background: var(--dl-bg-alt);
  border: 1px dashed var(--el-border-color-lighter);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--dl-muted);
}

.w-full {
  width: 100%;
}

@media (max-width: 768px) {
  .search-input {
    width: 100%;
  }
}
</style>
