<template>
  <el-config-provider namespace="el" size="default">
    <el-container class="layout">
      <el-aside width="220px" class="sidebar">
        <div class="logo">DigiLedger</div>
        <el-menu :default-active="active" router>
          <el-menu-item index="/">资产总览</el-menu-item>
          <el-menu-item index="/assets">物品中心</el-menu-item>
          <el-menu-item index="/wishlist">心愿单</el-menu-item>
          <el-menu-item index="/upgrade-routes">装备升级路线图</el-menu-item>
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
  if (route.path.startsWith('/upgrade-routes')) return '/upgrade-routes'
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
  background: var(--color-bg);
}

.sidebar {
  background: var(--color-sidebar-bg);
  color: var(--color-sidebar-text);
  padding: 18px 0 12px;
  border-right: 1px solid #e5e7eb;
  box-shadow: 0 10px 40px rgba(15, 23, 42, 0.06);
}

.logo {
  color: var(--color-accent);
  font-size: 20px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 18px;
  letter-spacing: 0.5px;
}

.header {
  background: var(--color-header-bg);
  color: var(--color-text);
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid #e5e7eb;
  box-shadow: 0 6px 18px rgba(15, 23, 42, 0.04);
}

.main {
  background: var(--color-bg-alt);
  padding: 28px;
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
  padding: 0 12px;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: var(--color-accent-soft);
  color: var(--color-accent);
  border-radius: 12px;
  margin: 0 6px;
}

.sidebar :deep(.el-menu-item) {
  display: flex;
  align-items: center;
  line-height: 44px;
  color: var(--color-sidebar-text);
  border-radius: 12px;
  height: 44px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  transition: background-color 0.15s ease, color 0.15s ease;
}

.sidebar :deep(.el-menu-item > span) {
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
}

.sidebar :deep(.el-menu-item:hover) {
  background: var(--color-accent-soft);
  color: var(--color-accent);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.theme-select {
  min-width: 140px;
}

.header-title {
  font-weight: 700;
  color: var(--color-text);
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
