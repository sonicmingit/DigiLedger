<template>
  <el-config-provider namespace="el">
    <el-container class="layout">
      <el-aside width="220px" class="sidebar">
        <div class="logo">DigiLedger</div>
        <el-menu :default-active="active" router>
          <el-menu-item index="/">资产总览</el-menu-item>
          <el-menu-item index="/assets">物品中心</el-menu-item>
          <el-menu-item index="/wishlist">心愿单</el-menu-item>
          <el-menu-item index="/settings">系统设置</el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="header">
          <span class="header-title">数码物品全生命周期管理</span>
          <div class="header-actions">
            <el-select v-model="currentTheme" size="small" class="theme-select">
              <el-option
                v-for="item in themeOptions"
                :key="item.value"
                :label="item.label"
                :value="item.value"
              />
            </el-select>
          </div>
        </el-header>
        <el-main class="main">
          <RouterView />
        </el-main>
      </el-container>
    </el-container>
  </el-config-provider>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { RouterView, useRoute } from 'vue-router'
import { useTheme } from '@/composables/theme'

const route = useRoute()
const active = computed(() => {
  if (route.path.startsWith('/assets')) return '/assets'
  if (route.path.startsWith('/wishlist')) return '/wishlist'
  if (route.path.startsWith('/settings')) return '/settings'
  return '/'
})

const { theme, setTheme, options } = useTheme()

const themeOptions = options

const currentTheme = computed({
  get: () => theme.value,
  set: (value) => setTheme(value)
})
</script>

<style scoped>
:global(body) {
  margin: 0;
  background: var(--color-bg);
  color: var(--color-text);
  font-family: 'Segoe UI', system-ui, -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
}

.layout {
  min-height: 100vh;
}

.sidebar {
  background: var(--color-sidebar-bg);
  color: var(--color-sidebar-text);
  padding: 16px 0;
}

.logo {
  color: var(--color-accent);
  font-size: 20px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 16px;
}

.header {
  background: var(--color-header-bg);
  color: var(--color-text);
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 24px;
}

.main {
  background: var(--color-bg-alt);
  padding: 24px;
}

.main :deep(.el-card) {
  background: var(--color-card);
  border-color: transparent;
  color: var(--color-text);
}

.main :deep(.el-card__header) {
  border-bottom-color: rgba(148, 163, 184, 0.2);
}

.sidebar :deep(.el-menu) {
  border-right: none;
  background: transparent;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.sidebar :deep(.el-menu-item) {
  color: var(--color-muted);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.theme-select {
  min-width: 120px;
}

.header-title {
  font-weight: 600;
}

@media (max-width: 900px) {
  .layout {
    flex-direction: column;
  }

  .sidebar {
    width: 100% !important;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  .sidebar :deep(.el-menu) {
    width: 100%;
    display: flex;
    justify-content: space-around;
  }

  .main {
    padding: 16px;
  }
}
</style>
