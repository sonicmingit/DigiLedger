<template>
  <div class="mobile-tabbar">
    <div 
      v-for="item in tabs" 
      :key="item.key"
      class="tab-item"
      :class="{ active: currentRoute.path.startsWith(item.path), 'is-fab': item.isFab }"
      @click="navigate(item)"
    >
      <div v-if="item.isFab" class="fab-button">
        <i class="mdi mdi-plus"></i>
      </div>
      <template v-else>
        <i class="mdi" :class="currentRoute.path.startsWith(item.path) ? item.activeIcon : item.icon"></i>
        <span class="tab-label">{{ item.label }}</span>
      </template>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'

const router = useRouter()
const currentRoute = useRoute()

const tabs = [
  { 
    key: 'home', 
    label: '首页', 
    path: '/mobile/index', 
    icon: 'mdi-view-dashboard-outline', 
    activeIcon: 'mdi-view-dashboard' 
  },
  { 
    key: 'assets', 
    label: '物品', 
    path: '/mobile/search', 
    icon: 'mdi-cube-outline', 
    activeIcon: 'mdi-cube' 
  },
  { 
    key: 'add', 
    label: '添加', 
    path: '', 
    icon: '', 
    activeIcon: '', 
    isFab: true 
  },
  { 
    key: 'wishlist', 
    label: '心愿', 
    path: '/mobile/wishlist', 
    icon: 'mdi-heart-outline', 
    activeIcon: 'mdi-heart' 
  },
  { 
    key: 'settings', 
    label: '我的', 
    path: '/mobile/settings', 
    icon: 'mdi-account-outline', 
    activeIcon: 'mdi-account' 
  }
]

const emit = defineEmits(['fab-click'])

const navigate = (item: any) => {
  if (item.isFab) {
    emit('fab-click')
  } else {
    router.push(item.path)
  }
}
</script>

<style scoped>
.mobile-tabbar {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  height: var(--dl-tabbar-height);
  background: rgba(18, 18, 18, 0.85);
  backdrop-filter: blur(12px);
  border-top: 1px solid rgba(255, 255, 255, 0.05);
  display: flex;
  justify-content: space-around;
  align-items: center;
  z-index: 100;
  padding-bottom: env(safe-area-inset-bottom);
}

.tab-item {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: var(--dl-text-muted);
  font-size: 10px;
  gap: 4px;
  transition: all 0.3s ease;
  height: 100%;
}

.tab-item .mdi {
  font-size: 24px;
  transition: transform 0.2s;
}

.tab-item.active {
  color: var(--dl-primary);
  text-shadow: 0 0 8px var(--dl-primary-dim);
}

.tab-item.active .mdi {
  transform: translateY(-2px);
}

.is-fab {
  position: relative;
  top: -20px;
  overflow: visible;
}

.fab-button {
  width: 50px;
  height: 50px;
  border-radius: 50%;
  background: linear-gradient(135deg, var(--dl-primary), var(--dl-secondary));
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 4px 12px rgba(0, 243, 255, 0.3);
  border: 2px solid rgba(255, 255, 255, 0.1);
}

.fab-button .mdi {
  font-size: 32px;
  color: #fff;
  transform: none;
}
</style>
