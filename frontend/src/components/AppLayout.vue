<template>
  <el-config-provider namespace="el">
    <el-container class="layout">
      <el-aside width="240px" class="sidebar">
        <div class="logo">
          <div class="mark">DL</div>
          <div class="brand">
            <span class="brand-title">DigiLedger</span>
            <small class="brand-sub">资产管理台</small>
          </div>
        </div>
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
          <div class="header-left">
            <span class="header-title">数码物品全生命周期管理</span>
            <span class="badge">v2</span>
          </div>
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
  background: radial-gradient(120% 80% at 10% 20%, rgba(124, 58, 237, 0.15), transparent),
    radial-gradient(60% 60% at 80% 0%, rgba(56, 189, 248, 0.12), transparent),
    var(--color-bg);
}

.sidebar {
  background: linear-gradient(160deg, rgba(255, 255, 255, 0.95), rgba(255, 255, 255, 0.85));
  color: var(--color-sidebar-text);
  padding: 24px 0;
  border-right: 1px solid var(--color-border);
  box-shadow: 18px 0 40px rgba(124, 58, 237, 0.08);
}

.logo {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 20px 16px;
  text-align: center;
  margin-bottom: 8px;
  color: var(--color-text);
}

.mark {
  width: 42px;
  height: 42px;
  border-radius: 12px;
  background: linear-gradient(135deg, #7c3aed, #6d28d9);
  color: #f8f9ff;
  display: grid;
  place-items: center;
  font-weight: 700;
  box-shadow: 0 12px 30px rgba(124, 58, 237, 0.25);
}

.brand-title {
  font-size: 18px;
  font-weight: 700;
}

.brand-sub {
  color: var(--color-muted);
}

.header {
  background: var(--color-header-bg);
  color: var(--color-text);
  font-size: 17px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 28px;
  box-shadow: 0 1px 0 var(--color-border), 0 10px 30px rgba(12, 10, 41, 0.04);
}

.main {
  background: var(--color-bg-alt);
  padding: 24px;
}

.main :deep(.el-card) {
  background: var(--color-card);
  border-color: var(--color-border);
  color: var(--color-text);
  box-shadow: var(--color-shadow);
}

.main :deep(.el-card__header) {
  border-bottom-color: var(--color-border);
}

.sidebar :deep(.el-menu) {
  border-right: none;
  background: transparent;
  padding: 0 8px;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: linear-gradient(135deg, rgba(124, 58, 237, 0.12), rgba(124, 58, 237, 0.08));
  color: var(--color-accent);
  border-radius: 12px;
  font-weight: 600;
}

.sidebar :deep(.el-menu-item) {
  color: var(--color-muted);
  border-radius: 10px;
  margin: 4px 8px;
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 10px;
}

.badge {
  padding: 4px 8px;
  border-radius: 10px;
  background: var(--color-surface-soft);
  color: var(--color-accent);
  font-size: 12px;
  font-weight: 600;
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
