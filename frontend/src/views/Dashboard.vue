<template>
  <div class="layout">
    <header class="hero">
      <div>
        <h1>DigiLedger 数码资产管家</h1>
        <p>掌控购买、折旧与售出盈亏，一站式管理你的数码设备。</p>
        <el-button type="primary" @click="loadAssets" :loading="loading">刷新资产</el-button>
      </div>
      <img src="/hero.svg" alt="hero" />
    </header>

    <section class="stats" v-if="assets.length">
      <asset-summary-card v-for="item in assets" :key="item.id" :asset="item" />
    </section>

    <el-empty description="暂无资产" v-else />
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '@/api/http'
import AssetSummaryCard from '@/components/AssetSummaryCard.vue'
import type { AssetSummary } from '@/types'

const assets = ref<AssetSummary[]>([])
const loading = ref(false)

const loadAssets = async () => {
  loading.value = true
  try {
    assets.value = await http.get<AssetSummary[]>('/assets')
  } finally {
    loading.value = false
  }
}

onMounted(loadAssets)
</script>

<style scoped>
.layout {
  padding: 2rem 5vw;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

.hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: linear-gradient(120deg, #60a5fa, #2563eb);
  border-radius: 16px;
  padding: 2rem;
  color: white;
}

.hero img {
  width: 220px;
  max-width: 40vw;
}

.stats {
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 1.5rem;
}
</style>
