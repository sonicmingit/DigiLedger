<template>
  <div class="mobile-scroll">
    <header class="mobile-topbar">
      <div class="mobile-topbar-header">
        <h1>心愿</h1>
      </div>
      <div class="mobile-summary-card">
        <div class="mobile-summary-grid">
          <div class="mobile-summary-item">
            <h3>心愿总值</h3>
            <strong>￥{{ totalValue }}</strong>
          </div>
          <div class="mobile-summary-item">
            <h3>心愿数量</h3>
            <strong>{{ wishlist.length }} 个</strong>
          </div>
        </div>
      </div>
    </header>

    <section class="mobile-card-section">
      <article v-for="item in wishlist" :key="item.id" class="mobile-wishlist-card">
        <div class="title">{{ item.name }}</div>
        <div class="mobile-wishlist-meta">
          <span>预期：￥{{ item.expectedPrice?.toFixed(2) ?? '0.00' }}</span>
          <span>状态：{{ item.status }}</span>
        </div>
        <p v-if="item.notes" class="desc">{{ item.notes }}</p>
        <div class="mobile-wishlist-actions">
          <button type="button" @click="edit(item.id)">编辑</button>
          <button type="button" @click="mark(item.id)" :disabled="item.status === '已购买'">
            {{ item.status === '已购买' ? '已完成' : '标记已购' }}
          </button>
          <button type="button" @click="remove(item.id)">删除</button>
        </div>
      </article>
      <MobileEmptyState
        v-if="!wishlist.length"
        description="空空如也，点击右下角 + 添加心愿"
      />
    </section>

    <div v-if="toast" class="mobile-toast">{{ toast }}</div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import MobileEmptyState from '@/mobile/components/MobileEmptyState.vue'
import { deleteWishlist, fetchWishlist, markWishlistPurchased } from '@/api/wishlist'
import type { WishlistItem } from '@/types'

const router = useRouter()
const wishlist = ref<WishlistItem[]>([])
const toast = ref('')

const totalValue = computed(() =>
  wishlist.value.reduce((sum, item) => sum + (item.expectedPrice || 0), 0).toFixed(2)
)

const loadWishlist = async () => {
  const data = await fetchWishlist()
  wishlist.value = data
}

const edit = (id: number) => {
  router.push({ name: 'mobileEditor', query: { type: 'wishlist', id: id.toString() } })
}

const mark = async (id: number) => {
  try {
    await markWishlistPurchased(id)
    toast.value = '已标记为已购买，快去登记资产吧'
    await loadWishlist()
  } catch (error) {
    toast.value = '操作失败，请稍后重试'
  } finally {
    setTimeout(() => (toast.value = ''), 1800)
  }
}

const remove = async (id: number) => {
  if (!confirm('确认删除该心愿吗？')) return
  try {
    await deleteWishlist(id)
    toast.value = '已删除'
    await loadWishlist()
  } catch (error) {
    toast.value = '删除失败，请稍后再试'
  } finally {
    setTimeout(() => (toast.value = ''), 1800)
  }
}

onMounted(loadWishlist)
</script>

<style scoped>
.desc {
  color: var(--mobile-muted);
  font-size: 13px;
}

button:disabled {
  opacity: 0.6;
}
</style>
