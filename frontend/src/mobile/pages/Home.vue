<template>
  <div class="page-home">
    <!-- Header Area -->
    <header class="home-header">
      <div class="logo-area">
        <h1 class="app-logo">DigiLedger</h1>
        <div class="avatar">
          <i class="mdi mdi-account"></i>
        </div>
      </div>
      
      <!-- Stats Scroll -->
      <div class="stats-scroll">
        <div class="stat-card primary-card">
          <div class="stat-label">总投入</div>
          <div class="stat-value">￥{{ totalValue }}</div>
          <div class="stat-trend">
            <i class="mdi mdi-buffer"></i> {{ assets.length }} 件资产
          </div>
        </div>
        
        <div class="stat-card">
          <div class="stat-label">本月支出</div>
          <div class="stat-value text-neon">￥{{ currentMonthSpending }}</div>
          <div class="stat-trend" :class="{ up: true }">
            <i class="mdi mdi-arrow-up"></i> 新增 {{ recentAssets.length }} 件
          </div>
        </div>

        <div class="stat-card">
          <div class="stat-label">在用 / 闲置</div>
          <div class="stat-value">{{ statusStats.active }} / {{ statusStats.idle }}</div>
          <div class="stat-bar">
            <div class="bar-fill" :style="{ width: activeRatio + '%' }"></div>
          </div>
        </div>
      </div>
    </header>

    <!-- Recent Activity -->
    <section class="section">
      <div class="section-header">
        <h2>最近活动</h2>
        <span class="link" @click="goAssets">全部 <i class="mdi mdi-chevron-right"></i></span>
      </div>
      
      <div class="asset-list" v-if="recentAssets.length">
        <MobileAssetCard 
          v-for="asset in recentAssets" 
          :key="asset.id" 
          :asset="asset" 
          @select="goDetail(asset.id)"
        />
      </div>
      <div v-else class="empty-state">
        暂无数据
      </div>
    </section>

    <!-- Quick Actions -->
    <section class="section">
      <div class="section-header">
        <h2>快捷操作</h2>
      </div>
      <div class="quick-actions">
        <button class="action-btn" @click="goSearch">
          <div class="icon-box neon-cyan"><i class="mdi mdi-magnify"></i></div>
          <span>搜索</span>
        </button>
        <button class="action-btn" @click="goWishlist">
          <div class="icon-box neon-purple"><i class="mdi mdi-heart-outline"></i></div>
          <span>心愿单</span>
        </button>
         <button class="action-btn" @click="openQuickAdd">
          <div class="icon-box neon-green"><i class="mdi mdi-plus"></i></div>
          <span>记一笔</span>
        </button>
        <button class="action-btn" @click="goStats">
          <div class="icon-box neon-yellow"><i class="mdi mdi-chart-box-outline"></i></div>
          <span>统计</span>
        </button>
      </div>
    </section>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import MobileAssetCard from '@/mobile/components/MobileAssetCard.vue'
import { fetchAssets } from '@/api/asset'
import type { AssetSummary } from '@/types'

const router = useRouter()
const assets = ref<AssetSummary[]>([])

// Load Data
const loadData = async () => {
  try {
    const res = await fetchAssets() // Fetch all for clientside calc
    assets.value = res || []
  } catch (e) {
    console.error(e)
  }
}

// Computeds
const totalValue = computed(() => {
  return assets.value.reduce((sum, item) => sum + (item.totalInvest || 0), 0).toFixed(0)
})

const statusStats = computed(() => {
  const active = assets.value.filter(a => a.status === '使用中').length
  const idle = assets.value.filter(a => a.status === '已闲置').length
  return { active, idle }
})

const activeRatio = computed(() => {
  const { active, idle } = statusStats.value
  const total = active + idle
  return total > 0 ? (active / total) * 100 : 0
})

const currentMonthSpending = computed(() => {
  const now = new Date()
  const year = now.getFullYear()
  const month = now.getMonth()
  
  return assets.value
    .filter(a => {
      if (!a.purchaseDate) return false
      const d = new Date(a.purchaseDate)
      return d.getFullYear() === year && d.getMonth() === month
    })
    .reduce((sum, item) => sum + (item.totalInvest || 0), 0)
    .toFixed(0)
})

const recentAssets = computed(() => {
  // Sort by purchase date desc, take top 5
  return [...assets.value]
    .sort((a, b) => new Date(b.purchaseDate || 0).getTime() - new Date(a.purchaseDate || 0).getTime())
    .slice(0, 5)
})

// Navigation
const goDetail = (id: number) => router.push({ name: 'mobileAssetDetail', params: { id } })
const goAssets = () => router.push('/mobile/search')
const goSearch = () => router.push('/mobile/search')
const goWishlist = () => router.push('/mobile/wishlist')
const goStats = () => router.push('/mobile/stats')
const openQuickAdd = () => {
  // Emit event or route to quick add
  // For now using the fab trigger logic or just same route
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.page-home {
  padding: env(safe-area-inset-top) 20px 20px 20px;
}

.home-header {
  margin-bottom: 24px;
}

.logo-area {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
  padding-top: 12px;
}

.app-logo {
  font-family: var(--dl-font-mono);
  font-size: 20px;
  letter-spacing: 1px;
  background: linear-gradient(90deg, #fff, var(--dl-primary));
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  margin: 0;
}

.avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: rgba(255, 255, 255, 0.1);
  display: flex;
  align-items: center;
  justify-content: center;
  color: var(--dl-text-primary);
  border: 1px solid rgba(255, 255, 255, 0.1);
}

/* Stats Scroll */
.stats-scroll {
  display: flex;
  gap: 12px;
  overflow-x: auto;
  scroll-snap-type: x mandatory;
  padding-bottom: 4px; /* hide scrollbar spacing */
  margin: 0 -20px;
  padding: 0 20px;
  scrollbar-width: none;
}
.stats-scroll::-webkit-scrollbar { display: none; }

.stat-card {
  min-width: 140px;
  background: var(--dl-bg-surface-light);
  border-radius: var(--dl-radius-lg);
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  border: 1px solid rgba(255, 255, 255, 0.05);
  scroll-snap-align: start;
}

.primary-card {
  background: linear-gradient(135deg, rgba(0, 243, 255, 0.1), rgba(188, 19, 254, 0.1));
  border: 1px solid rgba(0, 243, 255, 0.2);
}

.stat-label {
  font-size: 12px;
  color: var(--dl-text-muted);
}

.stat-value {
  font-size: 20px;
  font-weight: 700;
  font-family: var(--dl-font-mono);
  color: var(--dl-text-primary);
}

.stat-trend {
  font-size: 11px;
  color: var(--dl-text-secondary);
  display: flex;
  align-items: center;
  gap: 4px;
}

.stat-bar {
  height: 4px;
  background: rgba(255, 255, 255, 0.1);
  border-radius: 2px;
  margin-top: 4px;
  overflow: hidden;
}

.bar-fill {
  height: 100%;
  background: var(--dl-primary);
  border-radius: 2px;
}

/* Sections */
.section {
  margin-bottom: 32px;
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
}

.section-header h2 {
  font-size: 16px;
  margin: 0;
  color: var(--dl-text-primary);
}

.section-header .link {
  font-size: 12px;
  color: var(--dl-text-muted);
  display: flex;
  align-items: center;
}

.asset-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.empty-state {
  padding: 30px;
  text-align: center;
  color: var(--dl-text-muted);
  font-size: 13px;
  background: var(--dl-bg-surface-light);
  border-radius: var(--dl-radius-lg);
}

/* Quick Actions */
.quick-actions {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
}

.action-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  background: none;
  border: none;
  color: var(--dl-text-secondary);
  font-size: 11px;
  padding: 0;
}

.icon-box {
  width: 48px;
  height: 48px;
  border-radius: 16px;
  background: var(--dl-bg-surface-light);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 24px;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.neon-cyan { color: var(--dl-primary); }
.neon-purple { color: var(--dl-secondary); }
.neon-green { color: var(--dl-success); }
.neon-yellow { color: var(--dl-warning); }
</style>
