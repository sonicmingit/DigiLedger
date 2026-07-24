<template>
  <div class="page-wishlist">
    <header class="wishlist-header">
      <div class="header-top">
        <h1>心愿单</h1>
        <button class="add-btn" @click="addWish">
          <i class="mdi mdi-plus"></i>
        </button>
      </div>
      
      <!-- Status Tabs -->
      <div class="status-tabs">
        <button 
          v-for="status in statuses" 
          :key="status"
          class="tab" 
          :class="{ active: currentStatus === status }"
          @click="currentStatus = status"
        >
          {{ status }}
        </button>
      </div>
    </header>

    <div class="wishlist-grid" v-if="filteredList.length">
      <div 
        v-for="item in filteredList" 
        :key="item.id" 
        class="wish-card"
        @click="openWish(item)"
      >
        <div class="wish-image" v-if="item.imageUrl">
          <img :src="item.imageUrl" loading="lazy">
        </div>
        <div class="wish-content">
          <div class="wish-tags" v-if="item.priority">
             <span class="priority-tag" :class="`p-${item.priority}`">P{{ item.priority }}</span>
          </div>
          <h3>{{ item.name }}</h3>
          <p class="price" v-if="item.expectedPrice">
            ￥{{ item.expectedPrice }}
          </p>
        </div>
      </div>
    </div>
    
    <div v-else class="empty-state">
      <i class="mdi mdi-heart-broken"></i>
      <p>暂无心愿</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { fetchWishlist, createWishlist } from '@/api/wishlist'
import type { WishlistItem } from '@/types'
import { ElMessage, ElMessageBox } from 'element-plus'

const statuses = ['未购买', '已购买'] as const
const currentStatus = ref<(typeof statuses)[number]>('未购买')
const wishlist = ref<WishlistItem[]>([])

const loadWishlist = async () => {
  try {
    const res = await fetchWishlist({ status: currentStatus.value })
    wishlist.value = res || []
  } catch (e) {
    console.error(e)
  }
}

// Watch status change to reload
import { watch } from 'vue'
watch(currentStatus, () => {
  loadWishlist()
})

const filteredList = computed(() => wishlist.value)

const addWish = async () => {
  // Simple prompt for now
  try {
    const { value } = await ElMessageBox.prompt('请输入想要购买的物品名称', '添加心愿', {
      confirmButtonText: '添加',
      cancelButtonText: '取消',
    })
    
    if (value) {
      await createWishlist({ name: value })
      ElMessage.success('已添加')
      loadWishlist()
    }
  } catch {
    // cancelled
  }
}

const openWish = (item: WishlistItem) => {
  // Go to detail or edit
  // For now simple alert
  ElMessageBox.alert(`
    <p><strong>${item.name}</strong></p>
    <p>预算: ￥${item.expectedPrice || '-'}</p>
    <p>${item.notes || ''}</p>
  `, '心愿详情', {
    dangerouslyUseHTMLString: true
  })
}

onMounted(() => {
  loadWishlist()
})
</script>

<style scoped>
.page-wishlist {
  padding: env(safe-area-inset-top) 20px 80px 20px;
  background: var(--dl-bg-base);
  min-height: 100vh;
}

.wishlist-header {
  margin-bottom: 24px;
}

.header-top {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.header-top h1 {
  font-size: 24px;
  margin: 0;
  color: var(--dl-text-primary);
}

.add-btn {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: var(--dl-primary);
  color: #000;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  box-shadow: 0 0 10px var(--dl-primary-dim);
}

.status-tabs {
  display: flex;
  background: var(--dl-bg-surface-light);
  padding: 4px;
  border-radius: 12px;
}

.tab {
  flex: 1;
  padding: 8px;
  background: transparent;
  border: none;
  color: var(--dl-text-muted);
  font-size: 13px;
  border-radius: 8px;
  transition: all 0.2s;
}

.tab.active {
  background: rgba(255, 255, 255, 0.1);
  color: var(--dl-text-primary);
  font-weight: 600;
}

/* Masonry Grid Simulation */
.wishlist-grid {
  columns: 2; /* CSS columns for masonry layout */
  column-gap: 12px;
}

.wish-card {
  break-inside: avoid;
  background: var(--dl-bg-surface-light);
  border-radius: 12px;
  margin-bottom: 12px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.05);
}

.wish-image {
  width: 100%;
  aspect-ratio: 1; /* square for simple grid */
  background: #000;
}

.wish-image img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.wish-content {
  padding: 12px;
}

.wish-tags {
  margin-bottom: 8px;
}

.priority-tag {
  font-size: 10px;
  padding: 2px 6px;
  border-radius: 4px;
  background: rgba(255, 255, 255, 0.1);
  color: var(--dl-text-muted);
}
.p-1 { color: #ff4757; background: rgba(255, 71, 87, 0.1); }
.p-2 { color: #ffa502; background: rgba(255, 165, 2, 0.1); }
.p-3 { color: #2ed573; background: rgba(46, 213, 115, 0.1); }

.wish-content h3 {
  font-size: 14px;
  margin: 0 0 4px 0;
  color: var(--dl-text-primary);
  line-height: 1.4;
}

.price {
  font-size: 12px;
  color: var(--dl-primary);
  font-family: var(--dl-font-mono);
  margin: 0;
}

.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding-top: 100px;
  color: var(--dl-text-muted);
}

.empty-state i {
  font-size: 48px;
  margin-bottom: 16px;
  opacity: 0.3;
}
</style>
