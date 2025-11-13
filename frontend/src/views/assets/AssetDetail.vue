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
            <div class="title-row">
              <h2>{{ detail.name }}</h2>
              <el-tag type="success" v-if="primaryPurchase" size="small">主商品</el-tag>
            </div>
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
            <template #header>
              <div class="card-header">
                <span>购买记录</span>
                <el-button size="small" type="primary" text @click="openCreatePurchase">新增附属品</el-button>
              </div>
            </template>
            <el-table :data="detail.purchases" size="small" empty-text="暂无数据">
              <el-table-column label="类型" width="110">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.type === 'PRIMARY' ? 'success' : 'info'">
                    {{ purchaseTypeLabel(row.type) }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="名称" min-width="140">
                <template #default="{ row }">{{ row.type === 'PRIMARY' ? detail.name : row.name || '-' }}</template>
              </el-table-column>
              <el-table-column label="平台" width="140">
                <template #default="{ row }">{{ row.platformName || '-' }}</template>
              </el-table-column>
              <el-table-column label="价格" width="120">
                <template #default="{ row }">{{ row.currency || 'CNY' }} {{ formatNumber(row.price) }}</template>
              </el-table-column>
              <el-table-column prop="purchaseDate" label="购买日期" width="120" />
              <el-table-column label="操作" width="150">
                <template #default="{ row }">
                  <el-button link type="primary" @click="openEditPurchase(row)">编辑</el-button>
                  <el-button link type="danger" @click="confirmDeletePurchase(row)">删除</el-button>
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :xs="24" :md="12" v-if="detail.sales.length">
          <el-card>
            <template #header>售出记录</template>
            <el-table :data="detail.sales" size="small" empty-text="暂无数据">
              <el-table-column label="范围" width="100">
                <template #default="{ row }">
                  <el-tag size="small" :type="row.saleScope === 'ASSET' ? 'warning' : 'info'">
                    {{ row.saleScope === 'ASSET' ? '主商品' : '配件' }}
                  </el-tag>
                </template>
              </el-table-column>
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
          <el-descriptions-item label="备注" :span="2">{{ detail.notes || '暂无' }}</el-descriptions-item>
        </el-descriptions>
      </el-card>
    </div>
    <asset-form ref="formRef" @success="reload" />
    <sell-dialog ref="sellDialog" @success="reload" />
    <purchase-editor-dialog ref="purchaseDialog" @submit="handlePurchaseSubmit" />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { fetchAssetDetail, updateAsset } from '@/api/asset'
import type { AssetDetail, PurchaseRecord, AssetStatus } from '@/types'
import AssetForm from './components/AssetForm.vue'
import SellDialog from './components/SellDialog.vue'
import PurchaseEditorDialog from './components/PurchaseEditorDialog.vue'
import { useDictionaries } from '@/composables/useDictionaries'

const route = useRoute()
const router = useRouter()
const detail = ref<AssetDetail | null>(null)
const loading = ref(false)
const formRef = ref<InstanceType<typeof AssetForm> | null>(null)
const sellDialog = ref<InstanceType<typeof SellDialog> | null>(null)
const purchaseDialog = ref<InstanceType<typeof PurchaseEditorDialog> | null>(null)
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

const primaryPurchase = computed(() =>
  detail.value?.purchases.find((purchase) => purchase.type === 'PRIMARY')
)

const reload = async () => {
  await load()
}

const back = () => router.push('/assets')
const edit = () => detail.value && formRef.value?.open(detail.value)
const sell = () => detail.value && sellDialog.value?.open({ id: detail.value.id, name: detail.value.name })

const formatNumber = (value: number) => value.toFixed(2)

const purchaseTypeLabel = (type: PurchaseRecord['type']) => {
  switch (type) {
    case 'PRIMARY':
      return '主商品'
    case 'ACCESSORY':
      return '配件'
    case 'SERVICE':
      return '服务'
    default:
      return type
  }
}

const openCreatePurchase = () => {
  purchaseDialog.value?.open()
}

const openEditPurchase = (purchase: PurchaseRecord) => {
  purchaseDialog.value?.open(purchase)
}

const confirmDeletePurchase = async (purchase: PurchaseRecord) => {
  await ElMessageBox.confirm('确认删除该购买记录？', '提示', {
    confirmButtonText: '删除',
    cancelButtonText: '取消',
    type: 'warning'
  })
  await savePurchases(detail.value!.purchases.filter((item) => item.id !== purchase.id))
  ElMessage.success('已删除')
  await reload()
}

const savePurchases = async (purchases: PurchaseRecord[] | Partial<PurchaseRecord>[]) => {
  if (!detail.value) return
  const payload = {
    name: detail.value.name,
    categoryId: detail.value.categoryId!,
    brand: detail.value.brand || undefined,
    model: detail.value.model || undefined,
    serialNo: detail.value.serialNo || undefined,
    status: detail.value.status as AssetStatus,
    purchaseDate: detail.value.purchaseDate || undefined,
    enabledDate: detail.value.purchaseDate || detail.value.enabledDate,
    coverImageUrl: detail.value.coverImageUrl || undefined,
    notes: detail.value.notes || undefined,
    tagIds: detail.value.tags.map((tag) => tag.id),
    purchases: purchases.map((p) => ({
      type: p.type!,
      platformId: p.platformId,
      seller: p.seller || undefined,
      price: p.price!,
      shippingCost: p.shippingCost ?? 0,
      currency: p.currency || 'CNY',
      quantity: p.quantity || 1,
      purchaseDate: p.purchaseDate!,
      invoiceNo: p.invoiceNo || undefined,
      warrantyMonths: p.warrantyMonths ?? undefined,
      warrantyExpireDate: p.warrantyExpireDate || undefined,
      notes: p.notes || undefined,
      name: p.type === 'PRIMARY' ? undefined : p.name,
      attachments: p.attachments || []
    }))
  }
  await updateAsset(detail.value.id, payload)
  purchaseDialog.value?.setLoading(false)
  purchaseDialog.value?.reset()
  purchaseDialog.value?.close()
}

const handlePurchaseSubmit = async (payload: Partial<PurchaseRecord>) => {
  if (!detail.value) return
  purchaseDialog.value?.setLoading(true)
  const purchases = detail.value.purchases.map((item) => ({ ...item }))
  if (payload.id) {
    const index = purchases.findIndex((item) => item.id === payload.id)
    if (index >= 0) {
      purchases[index] = { ...purchases[index], ...payload }
    }
  } else {
    purchases.push({
      id: Date.now(),
      type: payload.type!,
      name: payload.name,
      platformId: payload.platformId,
      platformName: undefined,
      seller: payload.seller,
      price: payload.price!,
      shippingCost: payload.shippingCost ?? 0,
      currency: payload.currency || 'CNY',
      quantity: payload.quantity || 1,
      purchaseDate: payload.purchaseDate!,
      invoiceNo: payload.invoiceNo,
      warrantyMonths: payload.warrantyMonths,
      warrantyExpireDate: payload.warrantyExpireDate,
      attachments: [],
      notes: payload.notes
    })
  }
  await savePurchases(purchases)
  ElMessage.success('购买记录已保存')
  purchaseDialog.value?.setLoading(false)
  purchaseDialog.value?.close()
  await reload()
}

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

.title-row {
  display: flex;
  align-items: center;
  gap: 8px;
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

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

@media (max-width: 768px) {
  .summary-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 12px;
  }
  .cover {
    width: 100%;
  }
}
</style>
