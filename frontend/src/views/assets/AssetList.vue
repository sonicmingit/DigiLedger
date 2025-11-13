<template>
  <div class="asset-page">
    <el-card class="filter-card">
      <el-form :model="filters" inline class="filter-form">
        <el-form-item label="关键字">
          <el-input
            v-model="filters.keyword"
            placeholder="名称/品牌/型号"
            clearable
            @clear="refresh"
            @keyup.enter="refresh"
            class="filter-input"
          >
            <template #prefix>
              <el-icon><search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="filters.status" clearable placeholder="全部状态" @change="refresh">
            <el-option v-for="item in statuses" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-tree-select
            v-model="filters.categoryId"
            :data="categoryOptions"
            :props="treeProps"
            filterable
            check-strictly
            clearable
            placeholder="选择类别"
            class="filter-tree"
            @change="refresh"
          />
        </el-form-item>
        <el-form-item label="平台">
          <el-select
            v-model="filters.platformId"
            placeholder="全部平台"
            filterable
            clearable
            @change="refresh"
          >
            <el-option v-for="item in platforms" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-tree-select
            v-model="filters.tagIds"
            :data="tagOptions"
            :props="treeProps"
            multiple
            show-checkbox
            collapse-tags
            clearable
            filterable
            placeholder="选择标签"
            class="filter-tree"
            @change="refresh"
          />
        </el-form-item>
        <div class="filter-actions">
          <el-button type="primary" @click="openCreate">
            <el-icon class="mr-1"><plus /></el-icon>新建物品
          </el-button>
          <el-button @click="refresh" :loading="loading">刷新</el-button>
        </div>
      </el-form>
    </el-card>

    <el-card class="list-card">
      <div class="list-toolbar">
        <el-radio-group v-model="viewMode" size="small">
          <el-radio-button label="table">表格视图</el-radio-button>
          <el-radio-button label="card">卡片视图</el-radio-button>
        </el-radio-group>
        <div class="toolbar-actions">
          <el-button
            type="success"
            size="small"
            :disabled="!selectedIds.length"
            @click="openBatchTags"
          >
            批量设置标签 ({{ selectedIds.length }})
          </el-button>
          <el-button size="small" @click="toggleCompact">{{ compact ? '舒展' : '紧凑' }}模式</el-button>
        </div>
      </div>

      <el-table
        v-if="viewMode === 'table'"
        ref="tableRef"
        :data="assets"
        stripe
        style="width: 100%"
        :loading="loading"
        row-key="id"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="48" />
        <el-table-column prop="name" label="名称" min-width="180" :class-name="compact ? 'compact-col' : ''" />
        <el-table-column label="类别" width="160">
          <template #default="{ row }">{{ resolveCategoryName(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-select v-model="row.status" size="small" @change="(value) => changeStatus(row, value)">
              <el-option v-for="item in statuses" :key="item" :label="item" :value="item" />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="主商品价格" width="140">
          <template #default="{ row }">¥ {{ formatNumber(row.primaryPrice ?? row.totalInvest) }}</template>
        </el-table-column>
        <el-table-column label="日均成本" width="140">
          <template #default="{ row }">¥ {{ formatNumber(row.avgCostPerDay) }}</template>
        </el-table-column>
        <el-table-column prop="purchaseDate" label="购买时间" width="140" />
        <el-table-column label="标签" min-width="160">
          <template #default="{ row }">
            <el-tag
              v-for="tag in row.tags"
              :key="tag.id"
              size="small"
              class="tag-item"
              :style="tag.color ? { backgroundColor: tag.color, borderColor: tag.color, color: '#0f172a' } : undefined"
            >
              <i v-if="tag.icon" :class="tag.icon" class="tag-icon" />
              {{ tag.name }}
            </el-tag>
            <span v-if="!row.tags.length">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row.id)">详情</el-button>
            <el-button link @click="openEdit(row.id)">编辑</el-button>
            <el-button link type="success" @click="openSell(row)">出售</el-button>
            <el-popconfirm title="确认删除该物品？" @confirm="remove(row.id)">
              <template #reference>
                <el-button link type="danger">删除</el-button>
              </template>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>

      <div v-else class="card-grid">
        <asset-card
          v-for="item in assets"
          :key="item.id"
          :asset="item"
          :selectable="true"
          :selected="selectedIds.includes(item.id)"
          @toggle-select="(value) => toggleSelection(item.id, value)"
          @view="viewDetail"
          @edit="openEdit"
          @status-change="(status) => changeStatus(item, status)"
        >
          <template #actions>
            <el-button text size="small" type="primary" @click.stop="viewDetail(item.id)">详情</el-button>
            <el-button text size="small" @click.stop="openEdit(item.id)">编辑</el-button>
            <el-button text size="small" type="success" @click.stop="openSell(item)">出售</el-button>
          </template>
        </asset-card>
      </div>
      <el-empty v-if="!assets.length && !loading" description="暂无物品" />
    </el-card>

    <asset-form ref="formRef" @success="handleFormSuccess" />
    <sell-dialog ref="sellDialog" @success="refresh" />
    <batch-tag-dialog ref="batchDialog" @confirm="handleBatchConfirm" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, Search } from '@element-plus/icons-vue'
import {
  fetchAssets,
  deleteAsset,
  fetchAssetDetail,
  updateAsset
} from '@/api/asset'
import type { AssetSummary, AssetStatus } from '@/types'
import AssetForm from './components/AssetForm.vue'
import SellDialog from './components/SellDialog.vue'
import BatchTagDialog from './components/BatchTagDialog.vue'
import AssetCard from '@/components/AssetCard.vue'
import { useDictionaries } from '@/composables/useDictionaries'
import type { CategoryNode, TagNode } from '@/api/dict'

const router = useRouter()
const route = useRoute()

const assets = ref<AssetSummary[]>([])
const loading = ref(false)
const viewMode = ref<'table' | 'card'>('table')
const compact = ref(false)
const selectedIds = ref<number[]>([])

const filters = reactive({
  keyword: '',
  status: '' as AssetStatus | '',
  categoryId: null as number | null,
  platformId: null as number | null,
  tagIds: [] as number[]
})

const statuses: AssetStatus[] = ['使用中', '已闲置', '待出售', '已出售', '已丢弃']

const formRef = ref<InstanceType<typeof AssetForm> | null>(null)
const sellDialog = ref<InstanceType<typeof SellDialog> | null>(null)
const batchDialog = ref<InstanceType<typeof BatchTagDialog> | null>(null)

const { load: loadDicts, categoryTree, categoryPathMap, tagTree, platforms } = useDictionaries()

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

const refresh = async () => {
  loading.value = true
  try {
    assets.value = await fetchAssets({
      keyword: filters.keyword || undefined,
      status: filters.status || undefined,
      categoryId: filters.categoryId || undefined,
      platformId: filters.platformId || undefined,
      tagIds: filters.tagIds.length ? filters.tagIds : undefined
    })
  } finally {
    loading.value = false
  }
}

const openCreate = () => {
  formRef.value?.open()
}

const openEdit = async (id: number) => {
  loading.value = true
  try {
    const detail = await fetchAssetDetail(id)
    formRef.value?.open(detail)
  } finally {
    loading.value = false
  }
}

const openSell = (asset: AssetSummary) => {
  sellDialog.value?.open({ id: asset.id, name: asset.name })
}

const viewDetail = (id: number) => {
  router.push(`/assets/${id}`)
}

const remove = async (id: number) => {
  await ElMessageBox.confirm('确认删除该物品？该操作不可撤销。', '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await deleteAsset(id)
  ElMessage.success('删除成功')
  refresh()
}

const formatNumber = (value: number) => value.toFixed(2)

const resolveCategoryName = (asset: AssetSummary) => {
  if (!asset.categoryId) {
    return '-'
  }
  return categoryPathMap.value.get(asset.categoryId) || '-'
}

const handleSelectionChange = (rows: AssetSummary[]) => {
  selectedIds.value = rows.map((item) => item.id)
}

const toggleSelection = (id: number, checked: boolean) => {
  if (checked && !selectedIds.value.includes(id)) {
    selectedIds.value.push(id)
  }
  if (!checked) {
    selectedIds.value = selectedIds.value.filter((item) => item !== id)
  }
}

const changeStatus = async (asset: AssetSummary, status: AssetStatus) => {
  if (asset.status === status) return
  const detail = await fetchAssetDetail(asset.id)
  await updateAsset(asset.id, {
    name: detail.name,
    categoryId: detail.categoryId!,
    brand: detail.brand || undefined,
    model: detail.model || undefined,
    serialNo: detail.serialNo || undefined,
    status,
    purchaseDate: detail.purchaseDate || undefined,
    enabledDate: detail.purchaseDate || detail.enabledDate,
    coverImageUrl: detail.coverImageUrl || undefined,
    notes: detail.notes || undefined,
    tagIds: detail.tags?.map((tag) => tag.id) || [],
    purchases: detail.purchases.map((p) => ({
      type: p.type,
      platformId: p.platformId,
      seller: p.seller || undefined,
      price: p.price,
      shippingCost: p.shippingCost,
      currency: p.currency,
      quantity: p.quantity,
      purchaseDate: p.purchaseDate,
      invoiceNo: p.invoiceNo || undefined,
      warrantyMonths: p.warrantyMonths ?? undefined,
      warrantyExpireDate: p.warrantyExpireDate || undefined,
      notes: p.notes || undefined,
      name: p.name,
      attachments: p.attachments
    }))
  })
  asset.status = status
  ElMessage.success('状态已更新')
}

const toggleCompact = () => {
  compact.value = !compact.value
}

const openBatchTags = () => {
  batchDialog.value?.open()
}

const handleBatchConfirm = async (tags: number[]) => {
  if (!selectedIds.value.length) {
    batchDialog.value?.setLoading(false)
    return
  }
  try {
    await Promise.all(
      selectedIds.value.map(async (id) => {
        const detail = await fetchAssetDetail(id)
        await updateAsset(id, {
          name: detail.name,
          categoryId: detail.categoryId!,
          brand: detail.brand || undefined,
          model: detail.model || undefined,
          serialNo: detail.serialNo || undefined,
          status: detail.status,
          purchaseDate: detail.purchaseDate || undefined,
          enabledDate: detail.purchaseDate || detail.enabledDate,
          coverImageUrl: detail.coverImageUrl || undefined,
          notes: detail.notes || undefined,
          tagIds: tags,
          purchases: detail.purchases.map((p) => ({
            type: p.type,
            platformId: p.platformId,
            seller: p.seller || undefined,
            price: p.price,
            shippingCost: p.shippingCost,
            currency: p.currency,
            quantity: p.quantity,
            purchaseDate: p.purchaseDate,
            invoiceNo: p.invoiceNo || undefined,
            warrantyMonths: p.warrantyMonths ?? undefined,
            warrantyExpireDate: p.warrantyExpireDate || undefined,
            notes: p.notes || undefined,
            name: p.name,
            attachments: p.attachments
          }))
        })
      })
    )
    ElMessage.success('批量标签设置完成')
    batchDialog.value?.setLoading(false)
    batchDialog.value?.close()
    selectedIds.value = []
    refresh()
  } catch (error) {
    batchDialog.value?.setLoading(false)
    batchDialog.value?.close()
    ElMessage.error('批量操作失败')
  }
}

const handleFormSuccess = async () => {
  await refresh()
}

const syncFromRoute = () => {
  const { create, edit } = route.query
  if (create === '1') {
    formRef.value?.open()
    router.replace({ query: { ...route.query, create: undefined } })
  }
  if (edit) {
    const id = Number(edit)
    if (!Number.isNaN(id)) {
      openEdit(id)
    }
    const next = { ...route.query }
    delete next.edit
    router.replace({ query: next })
  }
}

watch(
  () => route.query,
  () => {
    syncFromRoute()
  }
)

onMounted(async () => {
  await loadDicts()
  await refresh()
  syncFromRoute()
})
</script>

<style scoped>
.asset-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.filter-input {
  width: 220px;
}

.filter-tree {
  min-width: 200px;
}

.filter-actions {
  margin-left: auto;
  display: flex;
  gap: 12px;
}

.list-toolbar {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.toolbar-actions {
  display: flex;
  gap: 8px;
}

.tag-item {
  margin-right: 6px;
  margin-bottom: 4px;
}

.tag-icon {
  margin-right: 4px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.compact-col {
  padding: 4px 0;
}

@media (max-width: 992px) {
  .filter-form {
    flex-direction: column;
    align-items: stretch;
  }

  .filter-actions {
    margin-left: 0;
    justify-content: flex-start;
  }

  .filter-input,
  .filter-tree {
    width: 100%;
  }

  .card-grid {
    grid-template-columns: repeat(auto-fill, minmax(180px, 1fr));
  }
}
</style>
