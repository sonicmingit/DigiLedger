<template>
  <el-dialog
    v-model="visible"
    title="智能找图设封面"
    width="760px"
    :before-close="handleClose"
    destroy-on-close
  >
    <div class="search-panel">
      <el-input
        v-model="keyword"
        placeholder="请输入名称/品牌或商品链接，支持模糊搜索"
        clearable
        size="small"
        class="search-input"
        @keyup.enter="searchSuggestions"
      />
      <el-select
        v-model="selectedProvider"
        class="provider-select"
        size="small"
        placeholder="选择图片来源"
        clearable
        :loading="providerLoading"
      >
        <el-option label="自动选择（推荐）" value="" />
        <el-option
          v-for="provider in providerOptions"
          :key="provider.name"
          :label="provider.displayName"
          :value="provider.name"
        >
          <div class="provider-option">
            <span class="provider-option-name">{{ provider.displayName }}</span>
            <span class="provider-option-desc" v-if="provider.description">{{ provider.description }}</span>
          </div>
        </el-option>
      </el-select>
      <el-button type="primary" size="small" :loading="loading" @click="searchSuggestions">
        搜索
      </el-button>
    </div>
    <p class="hint">
      系统会优先解析购买链接获取商品主图，若失败则使用 Bing 图像搜索。可手动调整关键词以提升命中率。
    </p>
    <p v-if="!props.assetId" class="hint warning">请先保存物品后再使用智能找图</p>
    <div v-if="loading" class="loading-state">
      <el-icon class="loading-icon" :size="20">
        <Loading />
      </el-icon>
      正在获取候选图片...
    </div>
    <el-empty
      v-if="showEmpty && !loading && !error"
      description="未找到合适的图片，请尝试修改搜索关键词，或手动上传封面图。"
    />
    <div v-else-if="suggestions.length" class="suggestion-grid">
      <div
        v-for="suggestion in suggestions"
        :key="suggestion.sourceUrl"
        class="suggestion-card"
      >
        <div class="suggestion-meta">
          <el-tag size="small" class="source-tag">{{ resolveSourceLabel(suggestion) }}</el-tag>
          <span v-if="suggestion.title" class="suggestion-title" :title="suggestion.title">
            {{ suggestion.title }}
          </span>
        </div>
        <el-image
          :src="suggestion.thumbUrl"
          fit="cover"
          :preview-src-list="[suggestion.sourceUrl]"
          class="suggestion-image"
        />
        <div class="suggestion-actions">
          <el-button type="primary" size="small" @click="selectSuggestion(suggestion)">
            设为封面
          </el-button>
        </div>
      </div>
    </div>
    <div v-if="error" class="error-text">{{ error }}</div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { Loading } from '@element-plus/icons-vue'
import type { CoverSuggestion, ImageSearchProviderItem } from '@/types'
import { fetchCoverSuggestions } from '@/api/asset'
import { fetchImageSearchProviders } from '@/api/imageSearch'

const props = withDefaults(
  defineProps<{
    modelValue: boolean
    assetId?: number
    query?: string
  }>(), { modelValue: false }
)

const emit = defineEmits<{
  (e: 'update:modelValue', value: boolean): void
  (e: 'select', value: CoverSuggestion): void
}>()

const visible = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value)
})
const keyword = ref(props.query || '')
const suggestions = ref<CoverSuggestion[]>([])
const loading = ref(false)
const error = ref('')
const providerOptions = ref<ImageSearchProviderItem[]>([])
const selectedProvider = ref('')
const providerLoading = ref(false)
const providerInitialized = ref(false)

const showEmpty = computed(() => !suggestions.value.length && !error.value && props.assetId && !loading.value)

const sourceLabelMap: Record<string, string> = {
  PURCHASE_LINK_JD: '购买链接 · 京东',
  PURCHASE_LINK_TAOBAO: '购买链接 · 淘宝',
  PURCHASE_LINK_PDD: '购买链接 · 拼多多',
  BING_IMAGE_SEARCH: 'Bing 图像搜索'
}
const loadProviders = async () => {
  providerLoading.value = true
  try {
    const data = await fetchImageSearchProviders()
    providerOptions.value = data.providers || []
    if (!providerInitialized.value || !selectedProvider.value) {
      selectedProvider.value = data.defaultProvider || ''
    }
    providerInitialized.value = true
  } catch (err) {
    console.error('failed to load image search providers', err)
  } finally {
    providerLoading.value = false
  }
}

onMounted(() => {
  loadProviders()
})

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      if (props.query) {
        keyword.value = props.query
      }
      if (!suggestions.value.length) {
        searchSuggestions()
      }
    } else {
      suggestions.value = []
      error.value = ''
    }
  }
)

watch(() => props.query, (query) => {
  if (query && !keyword.value) {
    keyword.value = query
  }
})

const searchSuggestions = async () => {
  if (!props.assetId) {
    error.value = '请先保存物品后再使用智能找图'
    return
  }
  loading.value = true
  error.value = ''
  try {
    const manualQuery = keyword.value.trim()
    const provider = selectedProvider.value || undefined
    suggestions.value = await fetchCoverSuggestions(
      props.assetId,
      manualQuery || undefined,
      provider
    )
  } catch (err: any) {
    error.value = err?.message || '暂时无法获取封面候选图片，请稍后重试或手动上传。'
  } finally {
    loading.value = false
  }
}

const resolveSourceLabel = (suggestion: CoverSuggestion) => {
  return (
    suggestion.extra?.sourceLabel ||
    (suggestion.source ? sourceLabelMap[suggestion.source] : '') ||
    '封面候选'
  )
}

const selectSuggestion = (suggestion: CoverSuggestion) => {
  emit('select', suggestion)
  visible.value = false
}

const handleClose = () => {
  emit('update:modelValue', false)
}
</script>

<style scoped>
.search-panel {
  display: flex;
  gap: 12px;
  align-items: center;
  margin-bottom: 16px;
}

.search-input {
  flex: 1;
}

.provider-select {
  min-width: 160px;
  width: 160px;
}

.provider-option {
  display: flex;
  flex-direction: column;
  line-height: 1.2;
}

.provider-option-name {
  font-weight: 500;
}

.provider-option-desc {
  font-size: 12px;
  color: #64748b;
}

.suggestion-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(160px, 1fr));
  gap: 12px;
  max-height: 400px;
  overflow-y: auto;
}

.suggestion-card {
  border-radius: 12px;
  border: 1px solid #edf2f7;
  background: #fff;
  overflow: hidden;
  display: flex;
  flex-direction: column;
  justify-content: flex-start;
}

.suggestion-image {
  width: 100%;
  height: 160px;
  background: #f0f4f8;
}

.suggestion-actions {
  padding: 10px;
  text-align: right;
  border-top: 1px solid #edf2f7;
}

.suggestion-meta {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 10px 0;
}

.source-tag {
  align-self: flex-start;
}

.suggestion-title {
  font-size: 13px;
  color: #334155;
  line-height: 1.4;
  display: -webkit-box;
  line-clamp: 2;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.error-text {
  color: #f43f5e;
  margin-top: 12px;
  font-size: 14px;
}
.hint {
  margin-bottom: 8px;
  color: #475569;
  font-size: 13px;
}

.hint.warning {
  color: #b45309;
}

.loading-state {
  display: flex;
  align-items: center;
  gap: 8px;
  color: #0f172a;
  font-size: 14px;
  padding: 12px 0;
}

.loading-icon {
  animation: spin 1s linear infinite;
}

@keyframes spin {
  from {
    transform: rotate(0deg);
  }
  to {
    transform: rotate(360deg);
  }
}
</style>
