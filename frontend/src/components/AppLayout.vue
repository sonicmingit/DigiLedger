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
  background: var(--dl-bg);
  color: var(--dl-text);
  font-family: 'Segoe UI', system-ui, -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
}

.layout {
  min-height: 100vh;
  background: var(--dl-bg);
}

.sidebar {
  background: var(--color-sidebar-bg);
  color: var(--color-sidebar-text);
  padding: 18px 0 12px;
  border-right: 1px solid var(--dl-border);
  box-shadow: var(--dl-shadow-md);
  position: sticky;
  top: 0;
  height: 100vh;
}

.logo {
  color: var(--dl-accent);
  font-size: 20px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 18px;
  letter-spacing: 0.5px;
}

.header {
  background: var(--color-header-bg);
  color: var(--dl-text);
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 24px;
  border-bottom: 1px solid var(--dl-border);
  box-shadow: var(--dl-shadow-sm);
}

.main {
  background: var(--dl-bg-alt);
  padding: 28px;
}

.main :deep(.el-card) {
  background: var(--dl-card);
  border-color: transparent;
  color: var(--dl-text);
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
  background: var(--dl-accent-soft);
  color: var(--dl-accent);
  border-radius: 12px;
  box-shadow: inset 2px 0 0 var(--dl-accent);
}

.sidebar :deep(.el-menu-item) {
  display: flex;
  align-items: center;
  line-height: 44px;
  color: var(--color-sidebar-text);
  border-radius: 12px;
  height: 44px;
  margin: 6px 6px;
  box-sizing: border-box;
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
  background: var(--dl-accent-soft);
  color: var(--dl-accent);
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
  color: var(--dl-text);
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
    position: static;
    height: auto;
  }

  .sidebar :deep(.el-menu) {
    width: 100%;
    display: flex;
    justify-content: space-around;
  }

  .sidebar :deep(.el-menu-item) {
    margin: 0 6px;
  }

  .main {
    padding: 16px;
  }
}
</style>
