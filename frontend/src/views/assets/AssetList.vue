<template>
  <div class="asset-page">
    <el-card class="status-card">
      <el-tabs v-model="statusTab" class="status-tabs" type="card">
        <el-tab-pane v-for="item in statusTabOptions" :key="item.value" :label="item.label" :name="item.value" />
      </el-tabs>
    </el-card>
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
          <el-select v-model="filters.status" clearable placeholder="全部状态" @change="refresh" style="width: 120px;">
            <el-option v-for="item in statuses" :key="item" :label="item" :value="item" />
          </el-select>
        </el-form-item>
        <el-form-item label="类别">
          <el-cascader
            v-model="filters.categoryId"
            :options="categoryOptions"
            :props="cascaderProps"
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
          <el-select
            v-model="filters.tagIds"
            multiple
            clearable
            filterable
            placeholder="选择标签"
            class="filter-tree"
            @change="refresh"
            style="min-width: 200px"
          >
            <el-option v-for="item in flatTagOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
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
        class="asset-table"
        :class="{ 'is-compact': compact }"
        :data="assets"
        empty-text=""
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
            <div class="cell-center">
              <el-tag size="small" round :style="statusTagStyle(row.status)">{{ row.status }}</el-tag>
            </div>
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
              <IconRenderer :icon="tag.icon" />
              {{ tag.name }}
            </el-tag>
            <span v-if="!row.tags.length">-</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="210">
          <template #default="{ row }">
            <div class="row-actions">
              <el-tooltip content="详情" placement="top">
                <el-button circle text type="primary" @click="viewDetail(row.id)">
                  <el-icon><View /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip content="编辑" placement="top">
                <el-button circle text @click="openEdit(row.id)">
                  <el-icon><EditPen /></el-icon>
                </el-button>
              </el-tooltip>
              <el-tooltip v-if="row.status !== '已出售'" content="出售" placement="top">
                <el-button circle text type="success" @click="openSell(row)">
                  <el-icon><Tickets /></el-icon>
                </el-button>
              </el-tooltip>

              <el-dropdown
                v-if="row.status !== '已出售'"
                trigger="click"
                :hide-on-click="true"
                @command="(value) => handleStatusCommand(row, value as AssetStatus)"
              >
                <span class="status-action">
                  <el-tooltip content="修改状态" placement="top">
                    <el-button circle text type="warning">
                      <el-icon><More /></el-icon>
                    </el-button>
                  </el-tooltip>
                </span>
                <template #dropdown>
                  <el-dropdown-menu>
                    <el-dropdown-item
                      v-for="status in editableStatuses"
                      :key="status"
                      :command="status"
                      :disabled="row.status === status"
                    >
                      {{ status }}
                    </el-dropdown-item>
                  </el-dropdown-menu>
                </template>
              </el-dropdown>

              <el-popconfirm title="确认删除该物品？" @confirm="remove(row.id)">
                <template #reference>
                  <el-button circle text type="danger">
                    <el-icon><Delete /></el-icon>
                  </el-button>
                </template>
              </el-popconfirm>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div v-else :class="['card-grid', { 'is-compact': compact }]">
        <asset-card
          v-for="item in assets"
          :key="item.id"
          :asset="item"
          :selectable="true"
          :selected="selectedIds.includes(item.id)"
          :compact="compact"
          @toggle-select="(value) => toggleSelection(item.id, value)"
          @view="viewDetail"
          @edit="openEdit"
          @suggest-cover="openCoverSuggestionDialog"
        >
          <template #actions>
            <el-tooltip content="详情" placement="top">
              <el-button text size="small" type="primary" circle @click.stop="viewDetail(item.id)">
                <el-icon><View /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip content="编辑" placement="top">
              <el-button text size="small" circle @click.stop="openEdit(item.id)">
                <el-icon><EditPen /></el-icon>
              </el-button>
            </el-tooltip>
            <el-tooltip v-if="item.status !== '已出售'" content="出售" placement="top">
              <el-button text size="small" type="success" circle @click.stop="openSell(item)">
                <el-icon><Tickets /></el-icon>
              </el-button>
            </el-tooltip>
            <el-dropdown
              v-if="item.status !== '已出售'"
              trigger="click"
              :hide-on-click="true"
              @command="(value) => handleStatusCommand(item, value as AssetStatus)"
            >
              <span class="status-action">
                <el-tooltip content="修改状态" placement="top">
                  <el-button text size="small" type="warning" circle>
                    <el-icon><More /></el-icon>
                  </el-button>
                </el-tooltip>
              </span>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item
                    v-for="status in editableStatuses"
                    :key="status"
                    :command="status"
                    :disabled="item.status === status"
                  >
                    {{ status }}
                  </el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>
        </asset-card>
      </div>
      <el-empty v-if="!assets.length && !loading" description="暂无物品" />
    </el-card>

    <asset-form ref="formRef" @success="handleFormSuccess" />
    <sell-dialog ref="sellDialog" @success="refresh" />
    <batch-tag-dialog ref="batchDialog" @confirm="handleBatchConfirm" />
  </div>
  <cover-suggestion-dialog
    v-model="coverSuggestionDialogVisible"
    :asset-id="coverSuggestionAsset?.id"
    :query="coverSuggestionQuery"
    @select="handleCoverSuggestionSelected"
    @update:modelValue="(value) => !value && (coverSuggestionAsset = null)"
  />
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Delete, EditPen, More, Plus, Search, Tickets, View } from '@element-plus/icons-vue'
import IconRenderer from '@/components/IconRenderer.vue'
import {
  fetchAssets,
  deleteAsset,
  fetchAssetDetail,
  updateAsset,
  setCoverFromUrl
} from '@/api/asset'
import type { AssetSummary, AssetStatus, BrandInfo, CoverSuggestion } from '@/types'
import AssetForm from './components/AssetForm.vue'
import SellDialog from './components/SellDialog.vue'
import BatchTagDialog from './components/BatchTagDialog.vue'
import AssetCard from '@/components/AssetCard.vue'
import CoverSuggestionDialog from '@/components/CoverSuggestionDialog.vue'
import { useDictionaries } from '@/composables/useDictionaries'
import type { CategoryNode, TagNode } from '@/api/dict'
import { extractObjectKey, extractObjectKeys } from '@/utils/storage'

const router = useRouter()
const route = useRoute()

const assets = ref<AssetSummary[]>([])
const loading = ref(false)
const viewMode = ref<'table' | 'card'>('table')
const compact = ref(false)
const selectedIds = ref<number[]>([])
let updatingRoute = false
let applyingRoute = false

const filters = reactive({
  keyword: '',
  status: '' as AssetStatus | '',
  categoryId: null as number | null,
  platformId: null as number | null,
  tagIds: [] as number[]
})

const statuses: AssetStatus[] = ['使用中', '已闲置', '待出售', '已出售', '已丢弃']
const editableStatuses: AssetStatus[] = ['使用中', '已闲置', '待出售', '已丢弃']

const statusTagStyle = (status: AssetStatus) => {
  const map: Record<AssetStatus, { backgroundColor: string; color: string; borderColor: string }> = {
    使用中: { backgroundColor: '#d1fae5', color: '#065f46', borderColor: '#bbf7d0' },
    已闲置: { backgroundColor: '#ecfeff', color: '#0ea5e9', borderColor: '#bae6fd' },
    待出售: { backgroundColor: '#fef9c3', color: '#92400e', borderColor: '#fef08a' },
    已出售: { backgroundColor: '#ffe4e6', color: '#be123c', borderColor: '#fecdd3' },
    已丢弃: { backgroundColor: '#f3f4f6', color: '#374151', borderColor: '#e5e7eb' }
  }
  return map[status]
}

const resolveBrandText = (brand?: BrandInfo | null) => {
  const alias = brand?.alias?.trim()
  if (alias) return alias
  const name = brand?.name?.trim()
  return name || undefined
}

const statusTabOptions = computed(() => [
  { label: '全部', value: 'all' },
  ...statuses.map((status) => ({ label: status, value: status }))
])

const statusTab = computed({
  get: () => filters.status || 'all',
  set: (value: string) => {
    filters.status = (value === 'all' ? '' : (value as AssetStatus))
    refresh()
  }
})

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

// 扁平化标签选项用于多选下拉（value,label）
const flatTagOptions = computed(() => {
  const res: { value: number; label: string }[] = []
  const walk = (nodes: any[] = []) => {
    nodes.forEach((n) => {
      if (!n) return
      res.push({ value: n.value, label: n.label })
      if (n.children && n.children.length) walk(n.children)
    })
  }
  walk(tagOptions.value)
  return res
})

const coverSuggestionDialogVisible = ref(false)
const coverSuggestionAsset = ref<AssetSummary | null>(null)

const coverSuggestionQuery = computed(() => buildCoverSuggestionQuery(coverSuggestionAsset.value))

const buildCoverSuggestionQuery = (asset: AssetSummary | null) => {
  if (!asset) return ''
  const parts = [
    asset.name,
    asset.brandName,
    asset.categoryId ? categoryPathMap.value.get(asset.categoryId) : undefined
  ].filter((value): value is string => !!value)
  return parts.join(' ')
}

const openCoverSuggestionDialog = (asset: AssetSummary) => {
  coverSuggestionAsset.value = asset
  coverSuggestionDialogVisible.value = true
}

const handleCoverSuggestionSelected = async (suggestion: CoverSuggestion) => {
  const asset = coverSuggestionAsset.value
  if (!asset) return
  try {
    await setCoverFromUrl(asset.id, { sourceUrl: suggestion.sourceUrl })
    ElMessage.success('封面更新成功')
    await refresh()
  } catch (error: any) {
    ElMessage.error(error?.message || '设置封面失败')
  } finally {
    coverSuggestionDialogVisible.value = false
    coverSuggestionAsset.value = null
  }
}

watch(coverSuggestionDialogVisible, (value) => {
  if (!value) {
    coverSuggestionAsset.value = null
  }
})

const buildQuerySignature = (query: Record<string, any>) =>
  Object.entries(query)
    .map(([key, value]) => {
      const raw = Array.isArray(value) ? value.join(',') : value ?? ''
      return [key, String(raw)]
    })
    .filter(([, value]) => value !== '')
    .sort((a, b) => a[0].localeCompare(b[0]))
    .map(([key, value]) => `${key}=${value}`)
    .join('&')

const syncFiltersToRoute = () => {
  if (applyingRoute) return
  const nextQuery: Record<string, any> = {}
  ;['create', 'edit'].forEach((key) => {
    const value = route.query[key]
    if (value != null) {
      nextQuery[key] = value
    }
  })
  const keyword = filters.keyword.trim()
  if (keyword) nextQuery.q = keyword
  if (filters.status) nextQuery.status = filters.status
  if (filters.categoryId) nextQuery.category = String(filters.categoryId)
  if (filters.platformId) nextQuery.platform = String(filters.platformId)
  if (filters.tagIds.length) nextQuery.tags = filters.tagIds.join(',')
  if (viewMode.value === 'card') nextQuery.view = 'card'
  if (compact.value) nextQuery.compact = '1'
  const currentSignature = buildQuerySignature(route.query as Record<string, any>)
  const nextSignature = buildQuerySignature(nextQuery)
  if (currentSignature === nextSignature) {
    return
  }
  updatingRoute = true
  router
    .replace({ query: nextQuery })
    .finally(() => {
      updatingRoute = false
    })
}

const applyRouteFilters = () => {
  const query = route.query
  filters.keyword = typeof query.q === 'string' ? query.q : ''
  filters.status =
    typeof query.status === 'string' && statuses.includes(query.status as AssetStatus)
      ? (query.status as AssetStatus)
      : ''
  filters.categoryId =
    typeof query.category === 'string' && !Number.isNaN(Number(query.category))
      ? Number(query.category)
      : null
  filters.platformId =
    typeof query.platform === 'string' && !Number.isNaN(Number(query.platform))
      ? Number(query.platform)
      : null
  filters.tagIds =
    typeof query.tags === 'string' && query.tags.trim().length
      ? query.tags
          .split(',')
          .map((id) => Number(id))
          .filter((id) => !Number.isNaN(id))
      : []
  viewMode.value = query.view === 'card' ? 'card' : 'table'
  compact.value = query.compact === '1'
}

const handleRouteActions = () => {
  const { create, edit } = route.query
  if (create === '1') {
    formRef.value?.open()
    const next = { ...route.query }
    delete next.create
    updatingRoute = true
    router.replace({ query: next }).finally(() => {
      updatingRoute = false
    })
  }
  if (edit) {
    const id = Number(edit)
    if (!Number.isNaN(id)) {
      openEdit(id)
    }
    const next = { ...route.query }
    delete next.edit
    updatingRoute = true
    router.replace({ query: next }).finally(() => {
      updatingRoute = false
    })
  }
}

watch(
  () => ({
    keyword: filters.keyword,
    status: filters.status,
    categoryId: filters.categoryId,
    platformId: filters.platformId,
    tagIds: [...filters.tagIds]
  }),
  () => syncFiltersToRoute()
)

watch(viewMode, () => syncFiltersToRoute())
watch(compact, () => syncFiltersToRoute())

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
  if (asset.status === '已出售') {
    ElMessage.warning('已出售的物品不可重复出售')
    return
  }
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

const handleStatusCommand = (asset: AssetSummary, status: AssetStatus) => {
  void changeStatus(asset, status)
}

const changeStatus = async (asset: AssetSummary, status: AssetStatus) => {
  if (asset.status === '已出售') {
    ElMessage.warning('已出售的物品不可修改状态')
    return
  }
  if (status === '已出售') {
    ElMessage.warning('请通过出售向导完成售出流程')
    return
  }
  if (asset.status === status) return
  const detail = await fetchAssetDetail(asset.id)
  try {
    await updateAsset(asset.id, {
      name: detail.name,
      categoryId: detail.categoryId!,
      brandId: detail.brand?.id ?? undefined,
      brand: resolveBrandText(detail.brand),
      model: detail.model || undefined,
      serialNo: detail.serialNo || undefined,
      status,
      purchaseDate: detail.purchaseDate || undefined,
      coverImageUrl: extractObjectKey(detail.coverImageUrl) || undefined,
      notes: detail.notes || undefined,
      tagIds: detail.tags?.map((tag) => tag.id) || [],
      purchases: detail.purchases.map((p) => ({
        type: p.type,
        platformId: p.platformId,
        seller: p.seller || undefined,
        price: p.price,
        shippingCost: p.shippingCost,
        quantity: p.quantity,
        purchaseDate: p.purchaseDate,
        warrantyMonths: p.warrantyMonths ?? undefined,
        warrantyExpireDate: p.warrantyExpireDate || undefined,
        notes: p.notes || undefined,
        name: p.name,
        attachments: extractObjectKeys(p.attachments)
      }))
    })
    asset.status = status
    ElMessage.success('状态已更新')
  } catch (error: any) {
    ElMessage.error(error?.message || '状态更新失败')
  }
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
          brandId: detail.brand?.id ?? undefined,
          brand: resolveBrandText(detail.brand),
          model: detail.model || undefined,
          serialNo: detail.serialNo || undefined,
          status: detail.status,
          purchaseDate: detail.purchaseDate || undefined,
          coverImageUrl: extractObjectKey(detail.coverImageUrl) || undefined,
          notes: detail.notes || undefined,
          tagIds: tags,
          purchases: detail.purchases.map((p) => ({
            type: p.type,
            platformId: p.platformId,
            seller: p.seller || undefined,
            price: p.price,
            shippingCost: p.shippingCost,
            quantity: p.quantity,
            purchaseDate: p.purchaseDate,
            warrantyMonths: p.warrantyMonths ?? undefined,
            warrantyExpireDate: p.warrantyExpireDate || undefined,
            notes: p.notes || undefined,
            name: p.name,
            attachments: extractObjectKeys(p.attachments)
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

watch(
  () => route.query,
  () => {
    if (updatingRoute) return
    applyingRoute = true
    applyRouteFilters()
    applyingRoute = false
    handleRouteActions()
    refresh()
  }
)

onMounted(async () => {
  await loadDicts()
  applyingRoute = true
  applyRouteFilters()
  applyingRoute = false
  await refresh()
  handleRouteActions()
  syncFiltersToRoute()
})
</script>

<style scoped>
.asset-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.status-card {
  padding: 8px 12px;
  background: #ecfdf3;
  border: 1px solid #d1fae5;
  box-shadow: 0 10px 24px rgba(15, 23, 42, 0.06);
  border-radius: 14px;
}

.status-tabs :deep(.el-tabs__header) {
  border-bottom: none;
}

.status-tabs :deep(.el-tabs__item) {
  border: none;
  background: transparent;
  color: var(--color-muted);
  padding: 8px 18px;
  height: 44px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.status-tabs :deep(.el-tabs__item.is-active) {
  background: var(--color-accent-soft);
  color: var(--color-accent);
  border-radius: 8px 8px 0 0;
  box-shadow: none;
}

.status-tabs :deep(.el-tabs__nav) {
  display: flex;
  gap: 12px;
  align-items: center;
}

.filter-form {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: flex-end;
}

.filter-form :deep(.el-input__wrapper),
.filter-form :deep(.el-select .el-input__wrapper),
.filter-form :deep(.el-cascader .el-input__wrapper) {
  border-radius: 12px;
  border-color: #e5e7eb;
  background-color: #f9fafb;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.9);
}

.filter-card {
  border: 1px solid #e5e7eb;
  border-radius: 14px;
  box-shadow: 0 12px 30px rgba(15, 23, 42, 0.06);
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

.status-action {
  display: inline-flex;
  align-items: center;
}

.cell-center {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 100%;
}

.row-actions {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
}
.row-actions :deep(.el-button) {
  padding: 6px;
  height: 32px;
}
.row-actions :deep(.el-button .el-icon) {
  font-size: 16px;
}

.tag-item {
  margin-right: 6px;
  margin-bottom: 4px;
}

.tag-icon {
  margin-right: 4px;
}

.tag-icon-svg {
  width: 16px;
  height: 16px;
  vertical-align: text-bottom;
  margin-right: 6px;
}

.card-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}

.asset-table.is-compact :deep(.el-table__cell) {
  padding: 6px 8px;
  font-size: 13px;
}

.asset-table.is-compact :deep(.el-table__header-wrapper .el-table__cell) {
  padding: 6px 8px;
}

.card-grid.is-compact {
  gap: 12px;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
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
