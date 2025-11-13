<template>
  <div class="wishlist-page">
    <el-card class="mb">
      <div class="actions">
        <el-radio-group v-model="activeStatus" size="large" @change="handleStatusChange">
          <el-radio-button label="全部" />
          <el-radio-button label="未购买" />
          <el-radio-button label="已购买" />
        </el-radio-group>
        <div class="action-buttons">
          <el-input
            v-model="keyword"
            placeholder="搜索名称/品牌"
            clearable
            class="search-input"
            @keyup.enter="filterItems"
            @clear="filterItems"
          >
            <template #prefix>
              <el-icon><search /></el-icon>
            </template>
          </el-input>
          <el-button type="primary" @click="openDialog()">
            <el-icon class="mr-1"><plus /></el-icon>新增心愿
          </el-button>
          <el-button @click="refresh" :loading="loading">刷新</el-button>
        </div>
      </div>
    </el-card>
    <el-card>
      <el-table :data="filtered" stripe :loading="loading" empty-text="暂无心愿" row-key="id">
        <el-table-column label="图片" width="120">
          <template #default="{ row }">
            <el-image
              v-if="row.imageUrl"
              :src="row.imageUrl"
              fit="cover"
              style="width: 92px; height: 68px; border-radius: 12px"
              :preview-src-list="[row.imageUrl]"
            />
            <div v-else class="image-placeholder">无图</div>
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" min-width="160" />
        <el-table-column prop="category" label="类别" width="140" />
        <el-table-column prop="brand" label="品牌" width="120" />
        <el-table-column prop="expectedPrice" label="期望价" width="120">
          <template #default="{ row }">{{ row.expectedPrice ? '¥ ' + formatNumber(row.expectedPrice) : '-' }}</template>
        </el-table-column>
        <el-table-column prop="priority" label="优先级" width="100" />
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag :type="row.status === '已购买' ? 'success' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="300">
          <template #default="{ row }">
            <el-button link type="info" @click="showDetail(row)">详情</el-button>
            <el-button link type="primary" @click="openDialog(row)">编辑</el-button>
            <el-button
              link
              type="success"
              @click="convert(row)"
              :disabled="row.status === '已购买'"
            >
              已购买
            </el-button>
            <el-popconfirm title="确定删除该心愿？" @confirm="remove(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
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
          <el-tree-select
            v-model="form.categoryId"
            :data="categoryOptions"
            :props="treeProps"
            filterable
            check-strictly
            clearable
            placeholder="选择类别"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="品牌">
          <el-select
            v-model="form.brand"
            filterable
            allow-create
            default-first-option
            placeholder="选择或输入品牌"
            @change="ensureBrandOption"
            class="w-full"
          >
            <el-option v-for="item in brandOptions" :key="item" :label="item" :value="item" />
          </el-select>
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
          <el-tree-select
            v-model="form.tagIds"
            :data="tagOptions"
            :props="treeProps"
            multiple
            show-checkbox
            collapse-tags
            filterable
            clearable
            placeholder="选择标签"
            class="w-full"
          />
        </el-form-item>
        <el-form-item label="优先级">
          <el-select v-model="form.priority" placeholder="选择优先级">
            <el-option v-for="n in 5" :key="n" :label="n" :value="n" />
          </el-select>
        </el-form-item>
        <el-form-item label="图片">
          <el-upload :http-request="handleUpload" :show-file-list="false" accept="image/*" capture="environment">
            <el-button type="primary">上传图片</el-button>
          </el-upload>
          <img v-if="form.imageUrl" :src="form.imageUrl" class="preview" />
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
    <wishlist-detail-drawer ref="detailDrawer" />
    <wishlist-convert-dialog ref="convertDialog" @success="refresh" />
  </div>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import { fetchWishlist, createWishlist, updateWishlist, deleteWishlist } from '@/api/wishlist'
import type { WishlistItem } from '@/types'
import WishlistConvertDialog from './components/WishlistConvertDialog.vue'
import WishlistDetailDrawer from './components/WishlistDetailDrawer.vue'
import { uploadFile } from '@/api/file'
import { useDictionaries } from '@/composables/useDictionaries'
import type { CategoryNode, TagNode } from '@/api/dict'

const items = ref<WishlistItem[]>([])
const filtered = ref<WishlistItem[]>([])
const loading = ref(false)
const visible = ref(false)
const saving = ref(false)
const current = ref<WishlistItem | null>(null)
const formRef = ref<FormInstance>()
const keyword = ref('')
const activeStatus = ref<'全部' | '未购买' | '已购买'>('全部')

const form = reactive({
  name: '',
  categoryId: null as number | null,
  brand: '',
  model: '',
  expectedPrice: undefined as number | undefined,
  link: '',
  notes: '',
  priority: 3,
  imageUrl: '',
  tagIds: [] as number[]
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }]
}

const brandOptions = ref<string[]>([])

const ensureBrandOption = (value: string) => {
  if (!value) return
  if (!brandOptions.value.includes(value)) {
    brandOptions.value.push(value)
  }
}

const { load: loadDicts, categoryTree, tagTree } = useDictionaries()

const treeProps = {
  value: 'value',
  label: 'label',
  children: 'children'
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

const filterItems = () => {
  const keywordValue = keyword.value.trim().toLowerCase()
  filtered.value = items.value.filter((item) => {
    if (!keywordValue) return true
    return `${item.name} ${item.brand || ''}`.toLowerCase().includes(keywordValue)
  })
}

const refresh = async () => {
  loading.value = true
  try {
    const statusParam = activeStatus.value === '全部' ? undefined : activeStatus.value
    items.value = await fetchWishlist(statusParam ? { status: statusParam } : undefined)
    const brands = items.value
      .map((item) => item.brand)
      .filter((name): name is string => Boolean(name && name.trim().length))
    brandOptions.value = Array.from(new Set(brands))
    filterItems()
  } finally {
    loading.value = false
  }
}

const handleStatusChange = () => {
  refresh()
}

const openDialog = (item?: WishlistItem) => {
  current.value = item || null
  visible.value = true
  if (item) {
    Object.assign(form, {
      name: item.name,
      categoryId: item.categoryId ?? null,
      brand: item.brand || '',
      model: item.model || '',
      expectedPrice: item.expectedPrice,
      link: item.link || '',
      notes: item.notes || '',
      priority: item.priority || 3,
      imageUrl: item.imageUrl || '',
      tagIds: item.tags ? item.tags.map((tag) => tag.id) : []
    })
    ensureBrandOption(form.brand)
  } else {
    reset()
  }
}

const reset = () => {
  Object.assign(form, {
    name: '',
    categoryId: null,
    brand: '',
    model: '',
    expectedPrice: undefined,
    link: '',
    notes: '',
    priority: 3,
    imageUrl: '',
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
        brand: form.brand || undefined,
        model: form.model || undefined,
        expectedPrice: form.expectedPrice,
        link: form.link || undefined,
        notes: form.notes || undefined,
        priority: form.priority,
        imageUrl: form.imageUrl || undefined,
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
const convertDialog = ref<InstanceType<typeof WishlistConvertDialog> | null>(null)

const showDetail = (item: WishlistItem) => {
  detailDrawer.value?.open(item)
}

const convert = (item: WishlistItem) => {
  convertDialog.value?.open(item)
}

const handleUpload = async (options: any) => {
  try {
    const { url } = await uploadFile(options.file)
    form.imageUrl = url
    ElMessage.success('上传成功')
    options.onSuccess(url)
  } catch (error: any) {
    ElMessage.error(error.message || '上传失败')
    options.onError(error)
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

.mb {
  margin-bottom: 8px;
}

.actions {
  display: flex;
  justify-content: space-between;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
}

.action-buttons {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.search-input {
  width: 220px;
}

.preview {
  width: 96px;
  height: 96px;
  border-radius: 12px;
  margin-left: 12px;
  object-fit: cover;
}

.image-placeholder {
  width: 92px;
  height: 68px;
  border-radius: 12px;
  background: rgba(30, 41, 59, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
}

.w-full {
  width: 100%;
}

@media (max-width: 768px) {
  .action-buttons {
    width: 100%;
  }

  .search-input {
    flex: 1;
  }
}
</style>
