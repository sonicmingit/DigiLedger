<template>
  <div class="image-search-settings" v-loading="loading">
    <p class="hint">
      选择一个默认的智能找图服务，后续在物品中使用「智能找图」时会自动选中，也可以切换为「自动轮询」按顺序尝试全部服务。
    </p>
    <el-radio-group v-model="selectedProvider" class="provider-radios" :disabled="providers.length === 0">
      <el-radio-button label="">自动轮询（推荐综合结果）</el-radio-button>
      <el-radio-button v-for="provider in providers" :key="provider.name" :label="provider.name">
        {{ provider.displayName }}
      </el-radio-button>
    </el-radio-group>
    <div class="provider-list">
      <el-card v-for="provider in providers" :key="provider.name" class="provider-card" shadow="never">
        <div class="provider-card-header">
          <div>
            <h4>{{ provider.displayName }}</h4>
            <el-tag size="small" type="info">{{ provider.name }}</el-tag>
          </div>
          <el-tag v-if="selectedProvider === provider.name" type="success" effect="dark" size="small">
            当前默认
          </el-tag>
        </div>
        <p class="provider-desc">{{ provider.description || '该服务暂无描述' }}</p>
      </el-card>
      <el-empty description="暂未发现可用的找图服务" v-if="!providers.length && !loading" />
    </div>
    <div class="actions">
      <el-button type="primary" :loading="saving" @click="saveDefault" :disabled="saving">
        保存默认服务
      </el-button>
      <el-button link @click="loadProviders" :disabled="loading">重新加载</el-button>
    </div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { ImageSearchProviderItem } from '@/types'
import { fetchImageSearchProviders, updateDefaultImageSearchProvider } from '@/api/imageSearch'

const loading = ref(false)
const saving = ref(false)
const providers = ref<ImageSearchProviderItem[]>([])
const selectedProvider = ref('')

const assignDefault = (provider?: string) => {
  selectedProvider.value = provider || ''
}

const loadProviders = async () => {
  loading.value = true
  try {
    const data = await fetchImageSearchProviders()
    providers.value = data.providers || []
    assignDefault(data.defaultProvider)
  } catch (err) {
    console.error('Failed to load image search providers', err)
  } finally {
    loading.value = false
  }
}

const saveDefault = async () => {
  saving.value = true
  try {
    await updateDefaultImageSearchProvider(selectedProvider.value || null)
    ElMessage.success('默认智能找图服务已更新')
  } catch (err) {
    console.error('Failed to update default image search provider', err)
  } finally {
    saving.value = false
  }
}

onMounted(() => {
  loadProviders()
})
</script>

<style scoped>
.image-search-settings {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.hint {
  margin: 0;
  padding: 12px 16px;
  background: rgba(59, 130, 246, 0.08);
  border-radius: 12px;
  color: #93c5fd;
  font-size: 13px;
}

.provider-radios {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.provider-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(220px, 1fr));
  gap: 12px;
}

.provider-card {
  border-radius: 12px;
}

.provider-card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.provider-card-header h4 {
  margin: 0;
  color: #e2e8f0;
}

.provider-desc {
  color: #cbd5f5;
  min-height: 36px;
}

.actions {
  display: flex;
  align-items: center;
  gap: 12px;
}
</style>
