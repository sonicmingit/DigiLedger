<template>
  <div class="page-detail" v-if="asset">
    <!-- Hero Header -->
    <header class="detail-header" :style="{ backgroundImage: `url(${asset.coverImageUrl})` }">
      <div class="header-overlay"></div>
      <button class="back-btn" @click="goBack">
        <i class="mdi mdi-arrow-left"></i>
      </button>
      <div class="header-content">
        <div class="header-badges">
          <span class="status-badge" :class="statusClass">{{ asset.status }}</span>
          <span class="category-badge">{{ asset.categoryPath }}</span>
        </div>
      </div>
    </header>

    <div class="detail-body">
      <!-- Title Block -->
      <section class="info-block">
        <h1 class="asset-name">{{ asset.name }}</h1>
        <p class="asset-model" v-if="asset.model || asset.brand">
          {{ asset.brand?.name }} {{ asset.model }}
        </p>
        
        <div class="price-row">
          <div class="price-item">
            <label>购入价格</label>
            <span class="price-val">￥{{ formatPrice(asset.totalInvest) }}</span>
          </div>
          <div class="price-item">
            <label>日均成本</label>
            <span class="price-val sm">￥{{ formatPrice(asset.avgCostPerDay) }}</span>
          </div>
        </div>
      </section>

      <!-- Life Cycle -->
      <section class="section">
        <h3 class="section-title">生命周期</h3>
        <div class="lifecycle-card">
          <div class="timeline-item">
            <div class="dot active"></div>
            <div class="content">
              <span>购入日期</span>
              <strong>{{ asset.purchaseDate }}</strong>
            </div>
          </div>
           <div class="timeline-line"></div>
          <div class="timeline-item">
            <div class="dot current"></div>
            <div class="content">
              <span>已使用</span>
              <strong class="text-neon">{{ asset.useDays }} 天</strong>
            </div>
          </div>
        </div>
      </section>

      <!-- Basic Info -->
      <section class="section" v-if="asset.notes">
        <h3 class="section-title">备注</h3>
        <div class="notes-card">
          {{ asset.notes }}
        </div>
      </section>

      <!-- Bottom Spacer -->
      <div class="spacer"></div>
    </div>

    <!-- Floating Action Bar -->
    <footer class="action-bar">
      <button class="action-btn secondary" @click="editAsset">
        <i class="mdi mdi-pencil"></i> 编辑
      </button>
      <button class="action-btn primary" @click="toggleStatus">
        <i class="mdi mdi-swap-horizontal"></i> 变更状态
      </button>
    </footer>
  </div>
  <div v-else-if="loading" class="loading-screen">
    <i class="mdi mdi-loading mdi-spin"></i>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { fetchAssetDetail, updateAsset } from '@/api/asset' // We might need a simpler status update api if exist
import type { AssetDetail } from '@/types'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const asset = ref<AssetDetail | null>(null)
const loading = ref(true)

const formatPrice = (val?: number) => (val || 0).toFixed(2)

const statusClass = computed(() => {
  if (!asset.value) return ''
  switch (asset.value.status) {
    case '使用中': return 'active'
    case '已闲置': return 'idle'
    case '已出售': return 'sold'
    default: return ''
  }
})

const loadDetail = async () => {
  loading.value = true
  try {
    const id = Number(route.params.id)
    if (id) {
      asset.value = await fetchAssetDetail(id)
    }
  } catch (e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const goBack = () => router.back()

const editAsset = () => {
  // Navigate to edit page (Desktop fallback or simple edit)
  // For now redirect to desktop/mobile editor
  router.push({ name: 'mobileEditor', query: { id: asset.value?.id } })
}

const toggleStatus = async () => {
  if (!asset.value) return
  const newStatus = asset.value.status === '使用中' ? '已闲置' : '使用中'
  // Simplified toggle for quick action
  try {
    // Note: Use full update or partial if API exists.
    // fetchAssetDetail returns AssetDetail, we need to send AssetPayload.
    // Ideally we should have a patch status API.
    // The design doc mentions `PATCH /api/assets/{id}/status`.
    // But `src/api/asset.ts` doesn't seem to have `updateAssetStatus` explicitly?
    // Checking `src/api/asset.ts`... line 84 is `updateAsset` (PUT).
    // I missed it or it wasn't there?
    // Let's assume full update for now to be safe or check API file again.
    // Re-reading file content from step 126... 
    // It has `updateAsset` payload `AssetPayload`.
    // It does NOT have specific PATCH status.
    // So I have to map AssetDetail to AssetPayload to update.
    
    // Quick hack for status update:
    /*
    const payload = {
        ...asset.value,
        status: newStatus
    }
    await updateAsset(asset.value.id, payload)
    */
    // Ideally user wants a modal to select status.
    ElMessage.info('功能开发中: 请使用编辑功能修改状态')
  } catch (e) {
    console.error(e)
  }
}

onMounted(() => {
  loadDetail()
})
</script>

<style scoped>
.page-detail {
  background: var(--dl-bg-base);
  min-height: 100vh;
  padding-bottom: 80px;
}

.detail-header {
  height: 280px;
  background-size: cover;
  background-position: center;
  position: relative;
}

.header-overlay {
  position: absolute;
  inset: 0;
  background: linear-gradient(to bottom, rgba(0,0,0,0.3), var(--dl-bg-base));
}

.back-btn {
  position: absolute;
  top: env(safe-area-inset-top);
  left: 20px;
  margin-top: 12px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: rgba(0,0,0,0.5);
  color: #fff;
  border: 1px solid rgba(255,255,255,0.2);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.header-content {
  position: absolute;
  bottom: 20px;
  left: 20px;
  right: 20px;
}

.header-badges {
  display: flex;
  gap: 8px;
}

.status-badge {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  font-weight: 600;
  background: rgba(255,255,255,0.2);
  backdrop-filter: blur(4px);
  color: #fff;
}
.status-badge.active { background: var(--dl-success); color: #000; }
.status-badge.idle { background: var(--dl-warning); color: #000; }
.status-badge.sold { background: var(--dl-danger); color: #000; }

.category-badge {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 12px;
  background: rgba(0,0,0,0.6);
  color: var(--dl-text-secondary);
  border: 1px solid rgba(255,255,255,0.1);
}

.detail-body {
  padding: 20px;
  position: relative;
  top: -20px;
}

.info-block {
  margin-bottom: 32px;
}

.asset-name {
  font-size: 28px;
  margin: 0 0 8px 0;
  color: var(--dl-text-primary);
  line-height: 1.2;
}

.asset-model {
  color: var(--dl-text-muted);
  font-size: 14px;
  margin: 0 0 20px 0;
}

.price-row {
  display: flex;
  gap: 40px;
}

.price-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.price-item label {
  font-size: 12px;
  color: var(--dl-text-muted);
}

.price-val {
  font-size: 24px;
  font-weight: 700;
  color: var(--dl-text-primary);
  font-family: var(--dl-font-mono);
}
.price-val.sm { font-size: 20px; color: var(--dl-primary); }

.section {
  margin-bottom: 32px;
}

.section-title {
  font-size: 14px;
  color: var(--dl-text-muted);
  margin-bottom: 16px;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.lifecycle-card {
  background: var(--dl-bg-surface-light);
  border-radius: var(--dl-radius-md);
  padding: 20px;
}

.timeline-item {
  display: flex;
  align-items: center;
  gap: 16px;
}

.timeline-line {
  height: 24px;
  width: 2px;
  background: rgba(255,255,255,0.1);
  margin: 4px 0 4px 6px; /* align with dot center (12px/2) => 6px */
}

.dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background: var(--dl-text-muted);
}
.dot.active { background: var(--dl-success); box-shadow: 0 0 8px var(--dl-success); }
.dot.current { background: var(--dl-primary); box-shadow: 0 0 8px var(--dl-primary); }

.content {
  display: flex;
  flex-direction: column;
  font-size: 14px;
}
.content span { color: var(--dl-text-muted); font-size: 12px; }

.notes-card {
  background: var(--dl-bg-surface-light);
  border-radius: var(--dl-radius-md);
  padding: 16px;
  color: var(--dl-text-secondary);
  font-size: 14px;
  line-height: 1.6;
}

.action-bar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  background: rgba(18,18,18,0.95);
  backdrop-filter: blur(12px);
  padding: 16px 20px;
  padding-bottom: calc(16px + env(safe-area-inset-bottom));
  border-top: 1px solid rgba(255,255,255,0.05);
  display: flex;
  gap: 16px;
  z-index: 100;
}

.action-btn {
  flex: 1;
  height: 48px;
  border-radius: 24px;
  border: none;
  font-weight: 600;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.action-btn.secondary {
  background: var(--dl-bg-surface-light);
  color: var(--dl-text-primary);
  border: 1px solid rgba(255,255,255,0.1);
}

.action-btn.primary {
  background: var(--dl-primary);
  color: #000;
  box-shadow: 0 0 12px var(--dl-primary-dim);
}

.loading-screen {
  height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 32px;
  color: var(--dl-primary);
}
</style>
