<template>
  <div class="asset-page">
    <el-card class="filter-card">
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="搜索名称/品牌" clearable @clear="refresh" @keyup.enter="refresh" />
        <el-select v-model="status" placeholder="全部状态" clearable @change="refresh">
          <el-option label="全部状态" :value="''" />
          <el-option v-for="item in statuses" :key="item" :label="item" :value="item" />
        </el-select>
        <el-button type="primary" @click="openCreate">新建资产</el-button>
      </div>
    </el-card>
    <el-card>
      <el-table :data="assets" stripe style="width: 100%" :loading="loading">
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column prop="category" label="类别" width="120" />
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
        <el-table-column label="操作" width="220">
          <template #default="{ row }">
            <el-button link type="primary" @click="viewDetail(row.id)">详情</el-button>
            <el-button link @click="openEdit(row.id)">编辑</el-button>
            <el-button link type="success" @click="openSell(row)">出售</el-button>
            <el-popconfirm title="确认删除该资产？" @confirm="remove(row.id)">
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
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAssets, deleteAsset, fetchAssetDetail } from '@/api/asset'
import type { AssetSummary } from '@/types'
import AssetForm from './components/AssetForm.vue'
import SellDialog from './components/SellDialog.vue'
import { ElMessage } from 'element-plus'

const router = useRouter()
const assets = ref<AssetSummary[]>([])
const loading = ref(false)
const keyword = ref('')
const status = ref('')
const statuses = ['使用中', '已闲置', '待出售', '已出售', '已丢弃']

const formRef = ref<InstanceType<typeof AssetForm> | null>(null)
const sellDialog = ref<InstanceType<typeof SellDialog> | null>(null)

const refresh = async () => {
  loading.value = true
  try {
    assets.value = await fetchAssets({ keyword: keyword.value || undefined, status: status.value || undefined })
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

onMounted(refresh)
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
.filter-bar .el-input {
  width: 220px;
}
</style>
