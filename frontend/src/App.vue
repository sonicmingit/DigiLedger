<template>
  <el-config-provider namespace="el">
    <el-container class="layout">
      <el-aside width="220px" class="sidebar">
        <div class="logo">DigiLedger</div>
        <el-menu :default-active="active" router>
          <el-menu-item index="/">仪表盘</el-menu-item>
          <el-menu-item index="/assets">资产中心</el-menu-item>
          <el-menu-item index="/wishlist">心愿单</el-menu-item>
        </el-menu>
      </el-aside>
      <el-container>
        <el-header class="header">数码资产全生命周期管理</el-header>
        <el-main class="main">
          <RouterView />
        </el-main>
      </el-container>
    </el-container>
  </el-config-provider>
</template>

<script setup lang="ts">
import { RouterView, useRoute } from 'vue-router'
import { computed } from 'vue'

const route = useRoute()
const active = computed(() => {
  if (route.path.startsWith('/assets')) return '/assets'
  if (route.path.startsWith('/wishlist')) return '/wishlist'
  return '/'
})
</script>

<style scoped>
:global(body) {
  margin: 0;
  background: #0f172a;
  color: #e2e8f0;
  font-family: 'Segoe UI', system-ui, -apple-system, BlinkMacSystemFont, 'PingFang SC', sans-serif;
}

.layout {
  min-height: 100vh;
}

.sidebar {
  background: #111827;
  color: #94a3b8;
  padding: 16px 0;
}

.logo {
  color: #38bdf8;
  font-size: 20px;
  font-weight: bold;
  text-align: center;
  margin-bottom: 16px;
}

.header {
  background: #1e293b;
  color: #e2e8f0;
  font-size: 18px;
  display: flex;
  align-items: center;
  padding-left: 24px;
}

.main {
  background: #0f172a;
  padding: 24px;
}

.sidebar :deep(.el-menu) {
  border-right: none;
  background: transparent;
}

.sidebar :deep(.el-menu-item.is-active) {
  background: rgba(56, 189, 248, 0.2);
  color: #38bdf8;
}

.sidebar :deep(.el-menu-item) {
  color: #cbd5f5;
}
</style>
