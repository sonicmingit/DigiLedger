<template>
  <div class="detail-page" v-loading="loading">
    <el-breadcrumb separator="/" class="mb">
      <el-breadcrumb-item to="/assets">物品中心</el-breadcrumb-item>
      <el-breadcrumb-item>{{ detail?.name }}</el-breadcrumb-item>
    </el-breadcrumb>
    <div v-if="detail">
      <el-card class="summary-card">
        <div class="summary-header">
          <div>
            <h2>{{ detail.name }}</h2>
            <p>{{ categoryName }} · 状态：{{ detail.status }}</p>
            <div class="tags">
              <el-tag
                v-for="tag in detail.tags"
                :key="tag.id"
                size="small"
                class="tag"
                :style="tag.color ? { backgroundColor: tag.color, borderColor: tag.color, color: '#0f172a' } : undefined"
              >
                <i v-if="tag.icon" :class="tag.icon" class="tag-icon" />
                {{ tag.name }}
              </el-tag>
              <span v-if="!detail.tags.length">无标签</span>
            </div>
          </div>
          <img v-if="detail.coverImageUrl" :src="detail.coverImageUrl" class="cover" />
        </div>
        <div class="metrics">
          <div class="metric">
            <span class="label">总投入</span>
            <span class="value">¥ {{ formatNumber(detail.totalInvest) }}</span>
          </div>
          <div class="metric">
            <span class="label">日均成本</span>
            <span class="value">¥ {{ formatNumber(detail.avgCostPerDay) }}</span>
          </div>
          <div class="metric">
            <span class="label">使用天数</span>
            <span class="value">{{ detail.useDays }}</span>
          </div>
          <div class="metric">
            <span class="label">最近净收入</span>
            <span class="value">¥ {{ formatNumber(detail.lastNetIncome) }}</span>
          </div>
        </div>
        <div class="actions">
          <el-button type="primary" @click="edit">编辑</el-button>
          <el-button type="success" @click="sell">出售向导</el-button>
          <el-button @click="back">返回列表</el-button>
        </div>
      </el-card>
      <el-row :gutter="16" class="mt">
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>购买记录</template>
            <el-table :data="detail.purchases" size="small" empty-text="暂无数据">
              <el-table-column prop="type" label="类型" width="110" />
              <el-table-column label="平台" width="140">
                <template #default="{ row }">{{ row.platformName || '-' }}</template>
              </el-table-column>
              <el-table-column prop="price" label="价格" width="140">
                <template #default="{ row }">{{ row.currency || 'CNY' }} {{ formatNumber(row.price) }}</template>
              </el-table-column>
              <el-table-column prop="quantity" label="数量" width="80" />
              <el-table-column label="卖家" min-width="160">
                <template #default="{ row }">{{ row.seller || '-' }}</template>
              </el-table-column>
              <el-table-column prop="purchaseDate" label="日期" width="120" />
            </el-table>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-card>
            <template #header>售出记录</template>
            <el-table :data="detail.sales" size="small" empty-text="暂无数据">
              <el-table-column prop="saleDate" label="日期" width="120" />
              <el-table-column label="平台" width="140">
                <template #default="{ row }">{{ row.platformName || '-' }}</template>
              </el-table-column>
              <el-table-column label="买家" min-width="140">
                <template #default="{ row }">{{ row.buyer || '-' }}</template>
              </el-table-column>
              <el-table-column prop="salePrice" label="售价" width="120">
                <template #default="{ row }">¥ {{ formatNumber(row.salePrice) }}</template>
              </el-table-column>
              <el-table-column prop="netIncome" label="净收入" width="120">
                <template #default="{ row }">¥ {{ formatNumber(row.netIncome) }}</template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>
      <el-card class="mt">
        <template #header>基础信息</template>
        <el-descriptions :column="2" border>
          <el-descriptions-item label="品牌">{{ detail.brand || '-' }}</el-descriptions-item>
          <el-descriptions-item label="型号">{{ detail.model || '-' }}</el-descriptions-item>
          <el-descriptions-item label="序列号">{{ detail.serialNo || '-' }}</el-descriptions-item>
          <el-descriptions-item label="类别路径">{{ categoryName }}</el-descriptions-item>
          <el-descriptions-item label="购买日期">{{ detail.purchaseDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="启用日期">{{ detail.enabledDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="报废日期">{{ detail.retiredDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="备注" :span="2">{{ detail.notes || '暂无' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>
    <asset-form ref="formRef" @success="reload" />
    <sell-dialog ref="sellDialog" @success="reload" />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchAssetDetail } from '@/api/asset'
import type { AssetDetail } from '@/types'
import AssetForm from './components/AssetForm.vue'
import SellDialog from './components/SellDialog.vue'
import { useDictionaries } from '@/composables/useDictionaries'

const route = useRoute()
const router = useRouter()
const detail = ref<AssetDetail | null>(null)
const loading = ref(false)
const formRef = ref<InstanceType<typeof AssetForm> | null>(null)
const sellDialog = ref<InstanceType<typeof SellDialog> | null>(null)
const { load: loadDicts, categoryPathMap } = useDictionaries()

const load = async () => {
  loading.value = true
  try {
    detail.value = await fetchAssetDetail(Number(route.params.id))
  } finally {
    loading.value = false
  }
}

const categoryName = computed(() => {
  if (!detail.value?.categoryId) {
    return '-'
  }
  return categoryPathMap.value.get(detail.value.categoryId) || '-'
})

const reload = async () => {
  await load()
}

const back = () => router.push('/assets')
const edit = () => detail.value && formRef.value?.open(detail.value)
const sell = () => detail.value && sellDialog.value?.open({ id: detail.value.id, name: detail.value.name })
const formatNumber = (value: number) => value.toFixed(2)

onMounted(async () => {
  await loadDicts()
  await load()
})
</script>

<style scoped>
.detail-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.mb {
  margin-bottom: 12px;
}

.summary-card {
  background: rgba(15, 23, 42, 0.6);
  border: 1px solid rgba(148, 163, 184, 0.2);
}

.summary-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.cover {
  width: 120px;
  height: 120px;
  object-fit: cover;
  border-radius: 12px;
}

.metrics {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(160px, 1fr));
  gap: 12px;
  margin-top: 16px;
}

.metric {
  background: rgba(30, 41, 59, 0.6);
  padding: 12px;
  border-radius: 12px;
}

.metric .label {
  color: #94a3b8;
}

.metric .value {
  display: block;
  font-size: 20px;
  color: #38bdf8;
  margin-top: 6px;
}

.actions {
  margin-top: 16px;
  display: flex;
  gap: 12px;
}

.mt {
  margin-top: 16px;
}

.tag {
  margin-right: 6px;
}

.tag-icon {
  margin-right: 4px;
}
</style>
