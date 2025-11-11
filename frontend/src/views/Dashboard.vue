<template>
  <div class="dashboard">
    <section class="hero">
      <div>
        <h1>欢迎回来，物品管家</h1>
        <p>快速浏览物品分布与投入情况，开始今日的设备管理工作。</p>
        <el-button type="primary" @click="goAsset">创建物品</el-button>
      </div>
    </section>
    <el-row :gutter="16" class="metrics">
      <el-col :xs="24" :md="8">
        <el-card class="metric-card">
          <div class="metric-value">{{ summaries.length }}</div>
          <div class="metric-label">物品总数</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card class="metric-card">
          <div class="metric-value">¥ {{ formatNumber(totalInvest) }}</div>
          <div class="metric-label">总投入</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="8">
        <el-card class="metric-card">
          <div class="metric-value">¥ {{ formatNumber(totalAvgCost) }}</div>
          <div class="metric-label">日均成本（合计）</div>
        </el-card>
      </el-col>
    </el-row>
    <el-card shadow="hover">
      <template #header>
        <div class="card-header">
          <span>最新物品</span>
          <el-button size="small" @click="refresh" :loading="loading">刷新</el-button>
        </div>
      </template>
      <el-table :data="summaries.slice(0, 5)" empty-text="暂无物品，请先创建" stripe>
        <el-table-column prop="name" label="名称" min-width="180" />
        <el-table-column label="类别" width="160">
          <template #default="{ row }">{{ resolveCategoryName(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="120">
          <template #default="{ row }">
            <el-tag size="small">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="总投入" width="140">
          <template #default="{ row }">¥ {{ formatNumber(row.totalInvest) }}</template>
        </el-table-column>
        <el-table-column label="日均成本" width="140">
          <template #default="{ row }">¥ {{ formatNumber(row.avgCostPerDay) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160">
          <template #default="{ row }">
            <el-button type="primary" link @click="viewDetail(row.id)">详情</el-button>
            <el-button type="primary" link @click="sell(row)">出售</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
    <sell-dialog ref="sellDialog" @success="refresh" />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { fetchAssets } from '@/api/asset'
import type { AssetSummary } from '@/types'
import SellDialog from './assets/components/SellDialog.vue'
import { useDictionaries } from '@/composables/useDictionaries'

const router = useRouter()
const summaries = ref<AssetSummary[]>([])
const loading = ref(false)
const sellDialog = ref<InstanceType<typeof SellDialog> | null>(null)
const { load: loadDicts, categoryPathMap } = useDictionaries()

const refresh = async () => {
  loading.value = true
  try {
    summaries.value = await fetchAssets()
  } finally {
    loading.value = false
  }
}

const totalInvest = computed(() =>
  summaries.value.reduce((sum, item) => sum + (item.totalInvest || 0), 0)
)
const totalAvgCost = computed(() =>
  summaries.value.reduce((sum, item) => sum + (item.avgCostPerDay || 0), 0)
)

const goAsset = () => router.push('/assets')
const viewDetail = (id: number) => router.push(`/assets/${id}`)
const formatNumber = (value: number) => value.toFixed(2)

const sell = (asset: AssetSummary) => {
  sellDialog.value?.open({ id: asset.id, name: asset.name })
}

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
.dashboard {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.hero {
  background: linear-gradient(135deg, rgba(59, 130, 246, 0.35), rgba(14, 165, 233, 0.25));
  padding: 32px;
  border-radius: 16px;
  color: #e2e8f0;
}

.hero h1 {
  margin: 0;
  font-size: 28px;
  font-weight: 600;
}

.hero p {
  margin: 12px 0 24px;
  color: #cbd5f5;
}

.metrics {
  margin: 0;
}

.metric-card {
  text-align: center;
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.metric-value {
  font-size: 28px;
  font-weight: bold;
  color: #38bdf8;
}

.metric-label {
  margin-top: 8px;
  color: #94a3b8;
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}
</style>
