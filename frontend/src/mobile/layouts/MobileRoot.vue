<template>
  <div class="mobile-shell">
    <RouterView />
    <div class="mobile-bottom-nav">
      <RouterLink
        v-for="item in navItems"
        :key="item.to"
        :to="item.to"
        class="mobile-bottom-item"
        :class="{ active: route.path.startsWith(item.activePrefix) }"
      >
        <span class="icon"><i class="mdi" :class="item.icon" aria-hidden="true"></i></span>
        <span>{{ item.label }}</span>
      </RouterLink>
    </div>

    <button type="button" class="mobile-fab" @click="toggleFab">+</button>

    <transition name="fade">
      <div v-if="fabOpen" class="mobile-overlay" @click="toggleFab"></div>
    </transition>
    <transition name="pop">
      <div v-if="fabOpen" class="mobile-fab-panel">
        <button type="button" @click="openEditor('asset')">新增资产</button>
        <button type="button" @click="openEditor('wishlist')">新增心愿</button>
      </div>
    </transition>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { useRoute, useRouter, RouterView, RouterLink } from 'vue-router'
import '@/mobile/styles/mobile.css'

const route = useRoute()
const router = useRouter()
const fabOpen = ref(false)

const navItems = computed(() => [
  { to: '/mobile/index', icon: 'mdi-home-variant-outline', label: '首页', activePrefix: '/mobile/index' },
  { to: '/mobile/wishlist', icon: 'mdi-heart-outline', label: '心愿', activePrefix: '/mobile/wishlist' },
  { to: '/mobile/stats', icon: 'mdi-chart-bar', label: '统计', activePrefix: '/mobile/stats' }
])

const toggleFab = () => {
  fabOpen.value = !fabOpen.value
}

const openEditor = (type: 'asset' | 'wishlist') => {
  fabOpen.value = false
  router.push({ name: 'mobileEditor', query: { type } })
}
</script>

<style scoped>
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.pop-enter-active,
.pop-leave-active {
  transition: transform 0.2s ease, opacity 0.2s ease;
}

.pop-enter-from,
.pop-leave-to {
  opacity: 0;
  transform: translateY(12px);
}
</style>
