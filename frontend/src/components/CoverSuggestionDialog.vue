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
        placeholder="请输入物品名称、品牌和类别，支持手动调整"
        clearable
        size="small"
        class="search-input"
        @keyup.enter="searchSuggestions"
      />
      <el-button type="primary" size="small" :loading="loading" @click="searchSuggestions">
        搜索
      </el-button>
    </div>
    <p v-if="!props.assetId" class="hint">请先保存物品后再使用智能找图</p>
    <el-empty v-if="showEmpty && !loading && !error" description="暂无推荐，请调整关键词" />
    <div v-else class="suggestion-grid">
      <div
        v-for="suggestion in suggestions"
        :key="suggestion.sourceUrl"
        class="suggestion-card"
      >
        <el-image
          :src="suggestion.thumbUrl"
          fit="cover"
          :preview-src-list="[suggestion.sourceUrl]"
          class="suggestion-image"
        />
        <div class="suggestion-actions">
          <el-button type="primary" size="mini" @click="selectSuggestion(suggestion)">
            设为封面
          </el-button>
        </div>
      </div>
    </div>
    <div v-if="error" class="error-text">{{ error }}</div>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { CoverSuggestion } from '@/types'
import { fetchCoverSuggestions } from '@/api/asset'

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

const showEmpty = computed(() => !suggestions.value.length && !error.value && props.assetId && !loading.value)

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
    suggestions.value = await fetchCoverSuggestions(props.assetId, keyword.value.trim())
  } catch (err: any) {
    error.value = err?.message || '获取封面建议失败'
  } finally {
    loading.value = false
  }
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
  justify-content: space-between;
}

.suggestion-image {
  width: 100%;
  height: 140px;
  background: #f0f4f8;
}

.suggestion-actions {
  padding: 10px;
  text-align: right;
  border-top: 1px solid #edf2f7;
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
</style>
