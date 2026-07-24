<template>
  <section class="search-settings">
    <header class="search-hero">
      <div>
        <span>IMAGE SOURCING</span>
        <h2>搜图配置</h2>
        <p>决定“新增物品 → 从外接服务搜图”中可选择的来源。服务必须先在外接服务中心保存密钥并启用。</p>
      </div>
      <div class="selection-counter"><strong>{{ selected.length }}</strong><span>个已启用</span></div>
    </header>

    <div v-if="loading" class="loading-state">正在读取搜图服务配置…</div>
    <template v-else>
      <div class="source-list">
        <label v-for="provider in providers" :key="provider.name" class="source-card" :class="{ unavailable: !provider.available, selected: selected.includes(provider.name) }">
          <el-checkbox v-model="selected" :value="provider.name" :disabled="!provider.available" />
          <div class="source-copy"><strong>{{ provider.displayName }}</strong><p>{{ provider.description }}</p></div>
          <span class="source-status">{{ provider.available ? (selected.includes(provider.name) ? '用于搜图' : '可选择') : '请先配置' }}</span>
        </label>
      </div>

      <div v-if="!providers.some(item => item.available)" class="empty-state">
        暂无可用搜图服务。请先在“外接 API”中保存密钥并打开对应服务开关。
      </div>

      <footer class="save-bar">
        <p>MT Photos 也会在已启用且已保存 API Key 后出现在此处。</p>
        <PrimaryButton label="保存搜图配置" :loading="saving" @click="save" />
      </footer>
    </template>
  </section>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { fetchImageSearchProviders, saveImageSearchProviders } from '@/api/settings'
import type { ImageSearchProvider } from '@/types'
import PrimaryButton from './PrimaryButton.vue'

const providers = ref<ImageSearchProvider[]>([])
const selected = ref<string[]>([])
const loading = ref(true)
const saving = ref(false)

async function load() {
  loading.value = true
  try {
    const response = await fetchImageSearchProviders()
    providers.value = response.providers
    selected.value = response.enabledProviders
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    loading.value = false
  }
}

async function save() {
  saving.value = true
  try {
    await saveImageSearchProviders(selected.value)
    ElMessage.success('搜图配置已保存')
    await load()
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    saving.value = false
  }
}

onMounted(load)
</script>

<style scoped>
.search-settings{display:grid;gap:18px}.search-hero{display:flex;align-items:flex-start;justify-content:space-between;gap:30px;padding:27px 30px;border-radius:18px;background:linear-gradient(118deg,#172218,#293629);color:#fbfdf7;overflow:hidden}.search-hero span{display:block;color:#b7ff3c;font-size:10px;font-weight:900;letter-spacing:.16em}.search-hero h2{margin:7px 0 6px;font-size:25px;letter-spacing:-.045em}.search-hero p{max-width:580px;margin:0;color:#cbd5c6;font-size:12px;line-height:1.7}.selection-counter{display:grid;place-items:center;flex:0 0 94px;min-height:75px;border:1px solid rgba(183,255,60,.5);border-radius:16px;background:rgba(5,12,6,.18)}.selection-counter strong{color:#d9ff9a;font-size:26px;line-height:1}.selection-counter span{margin-top:4px;color:#e8f4df;font-size:10px}.source-list{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:12px}.source-card{display:grid;grid-template-columns:auto 1fr auto;align-items:start;gap:12px;min-height:100px;padding:17px;border:1px solid #e0e7d9;border-radius:15px;background:#fff;cursor:pointer;transition:.18s ease}.source-card:hover:not(.unavailable){border-color:#a6c985;transform:translateY(-1px)}.source-card.selected{border-color:#91be65;background:linear-gradient(140deg,#fff 45%,#f3faea)}.source-card.unavailable{background:#f5f6f3;cursor:not-allowed;opacity:.7}.source-card :deep(.el-checkbox){margin-top:2px}.source-copy strong{font-size:13px}.source-copy p{margin:5px 0 0;color:var(--dl-text-secondary);font-size:10px;line-height:1.55}.source-status{padding:4px 6px;border-radius:7px;background:#eaf4df;color:#659049;font-size:9px;font-weight:800;white-space:nowrap}.unavailable .source-status{background:#ebedeb;color:#8b9389}.save-bar{display:flex;align-items:center;justify-content:space-between;gap:16px;padding:14px 4px}.save-bar p,.loading-state,.empty-state{margin:0;color:var(--dl-text-secondary);font-size:11px}.loading-state,.empty-state{padding:32px;border:1px dashed #d8e1d1;border-radius:14px;text-align:center}.empty-state{background:#fafbf8}@media(max-width:720px){.source-list{grid-template-columns:1fr}.search-hero{flex-direction:column}.selection-counter{width:100%}.save-bar{align-items:flex-start;flex-direction:column}}
</style>
