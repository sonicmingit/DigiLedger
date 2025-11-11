<template>
  <div class="asset-page">
    <el-card class="filter-card">
      <div class="filter-bar">
        <el-input
          v-model="keyword"
          placeholder="搜索名称/品牌"
          clearable
          @clear="refresh"
          @keyup.enter="refresh"
        />
        <el-select v-model="status" placeholder="全部状态" clearable @change="refresh">
          <el-option label="全部状态" :value="''" />
          <el-option v-for="item in statuses" :key="item" :label="item" :value="item" />
        </el-select>
        <el-tree-select
          v-model="categoryId"
          :data="categoryOptions"
          :props="treeProps"
          check-strictly
          clearable
          placeholder="选择类别"
          @change="refresh"
        />
        <el-select v-model="platformId" placeholder="全部平台" clearable filterable @change="refresh">
          <el-option v-for="item in platforms" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
        <el-tree-select
          v-model="tagIds"
          :data="tagOptions"
          :props="treeProps"
          multiple
          show-checkbox
          clearable
          collapse-tags
          placeholder="选择标签"
          @change="refresh"
        />
        <el-button type="primary" @click="openCreate">新建物品</el-button>
      </div>
    </el-card>
    <el-card>
      <el-table :data="assets" stripe style="width: 100%" :loading="loading">
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column label="类别" width="160">
          <template #default="{ row }">{{ resolveCategoryName(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag>{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="totalInvest" label="总投入" width="140">
          <template #default="{ row }">¥ {{ formatNumber(row.totalInvest) }}</template>
        </el-table-column>
        <el-table-column prop="avgCostPerDay" label="日均成本" width="140">
          <template #default="{ row }">¥ {{ formatNumber(row.avgCostPerDay) }}</template>
        </el-table-column>
        <el-table-column label="启用日期" width="140" prop="enabledDate" />
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
    </el-card>
    <asset-form ref="formRef" @success="refresh" />
    <sell-dialog ref="sellDialog" @success="refresh" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAssets, deleteAsset, fetchAssetDetail } from '@/api/asset'
import type { AssetSummary } from '@/types'
import AssetForm from './components/AssetForm.vue'
import SellDialog from './components/SellDialog.vue'
import { ElMessage } from 'element-plus'
import { useDictionaries } from '@/composables/useDictionaries'
import type { CategoryNode, TagNode } from '@/api/dict'

const router = useRouter()
const assets = ref<AssetSummary[]>([])
const loading = ref(false)
const keyword = ref('')
const status = ref('')
const statuses = ['使用中', '已闲置', '待出售', '已出售', '已丢弃']
const categoryId = ref<number | null>(null)
const platformId = ref<number | null>(null)
const tagIds = ref<number[]>([])

const formRef = ref<InstanceType<typeof AssetForm> | null>(null)
const sellDialog = ref<InstanceType<typeof SellDialog> | null>(null)

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

const refresh = async () => {
  loading.value = true
  try {
    assets.value = await fetchAssets({
      keyword: keyword.value || undefined,
      status: status.value || undefined,
      categoryId: categoryId.value || undefined,
      platformId: platformId.value || undefined,
      tagIds: tagIds.value.length ? tagIds.value : undefined
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

onMounted(async () => {
  await loadDicts()
  await refresh()
})
</script>

<style scoped>
.asset-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.filter-bar {
  display: flex;
  gap: 12px;
  flex-wrap: wrap;
}

.filter-bar .el-select,
.filter-bar .el-input,
.filter-bar .el-tree-select {
  width: 220px;
}

.tag-item {
  margin-right: 6px;
  margin-bottom: 4px;
}

.tag-icon {
  margin-right: 4px;
}
</style>
