<template>
  <div class="predecessor-picker">
    <el-select :model-value="modelValue ?? undefined" filterable remote clearable :remote-method="search" :loading="loading" :disabled="!categoryId" :placeholder="categoryId ? '搜索同类别物品名称、品牌或型号' : '请先选择物品分类'" :no-data-text="error ? '搜索暂不可用' : '没有找到同类别物品，试试其他关键词'" @update:model-value="choose" @visible-change="onVisibilityChange">
      <el-option v-for="item in displayedOptions" :key="item.id" :label="item.name" :value="item.id" class="predecessor-option">
        <div class="option-row"><img v-if="item.coverImageUrl" :src="item.coverImageUrl" alt="" /><img v-else class="option-empty-image" :src="assetIcon" alt="" /><div><strong>{{ item.name }}</strong><small>{{ item.status }} · {{ item.primaryPurchaseDate || '购买日期未填写' }}</small></div><span>{{ nullableMoney(item.primaryPrice) }}</span></div>
      </el-option>
    </el-select>
    <p v-if="error" class="picker-error" role="alert">{{ error }} <button type="button" @click="search(keyword)">重试</button></p>
    <p class="picker-hint">选填；只搜索当前分类的物品，每次最多显示 20 条，可输入关键词缩小范围。</p>
    <div v-if="selectedAsset" class="selected-predecessor">
      <button type="button" class="selected-cover" :aria-label="`预览 ${selectedAsset.name}`" @click="$emit('preview', selectedAsset.id)"><img :src="selectedAsset.coverImageUrl || assetIcon" :alt="selectedAsset.name" /></button>
      <div class="selected-copy"><button type="button" class="selected-name" @click="$emit('preview', selectedAsset.id)">{{ selectedAsset.name }}</button><span>{{ selectedAsset.status }} · 主商品 {{ nullableMoney(selectedAsset.primaryPrice) }}</span><small>购买于 {{ selectedAsset.primaryPurchaseDate || '未填写' }}</small></div>
      <button type="button" class="picker-preview" @click="$emit('preview', selectedAsset.id)">预览</button>
      <button type="button" class="picker-clear" @click="choose(null)">取消关联</button>
      <div class="draft-comparison"><span>主商品差价 <strong>{{ signedMoney(comparison?.delta ?? null) }}</strong></span><span>购买间隔 <strong>{{ comparison?.gap === null || comparison?.gap === undefined ? '暂无法计算' : `${Math.abs(comparison.gap)} 天` }}</strong></span><small v-if="comparison?.gap !== null && comparison?.gap !== undefined && comparison.gap < 0">当前购买日期早于上代，请核对。</small></div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onBeforeUnmount, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { AssetPredecessor, PurchaseRecord } from '@/types'
import { fetchPredecessorOptions } from '@/api/assets'
import { nullableMoney, purchaseComparison, signedMoney } from '@/utils/predecessor'
import assetIcon from '@/assets/icons/assets.svg'
const props = defineProps<{ modelValue?: number | null; categoryId?: number; excludeAssetId?: number; selected?: AssetPredecessor | null; currentPurchase?: PurchaseRecord }>()
const emit = defineEmits<{ 'update:modelValue': [id: number | null]; preview: [id: number] }>()
const options = ref<AssetPredecessor[]>([]), selectedAsset = ref<AssetPredecessor | null>(null), loading = ref(false), error = ref(''), keyword = ref('')
let requestId = 0, timer: ReturnType<typeof setTimeout> | undefined
const displayedOptions = computed(() => {
  const selected = selectedAsset.value
  if (!selected || options.value.some(item => item.id === selected.id)) return options.value
  const query = keyword.value.trim().toLowerCase()
  if (query && !`${selected.name} ${selected.brandName || ''} ${selected.model || ''}`.toLowerCase().includes(query)) return options.value
  return [selected, ...options.value]
})
const comparison = computed(() => selectedAsset.value ? purchaseComparison(props.currentPurchase, selectedAsset.value) : null)
function choose(value: number | null | undefined | '') {
  const id = typeof value === 'number' ? value : null
  selectedAsset.value = displayedOptions.value.find(item => item.id === id) || null
  emit('update:modelValue', id)
}
function cancelSearch() { clearTimeout(timer); requestId++; loading.value = false }
function onVisibilityChange(open: boolean) { if (open) search(keyword.value) }
function search(query: string) {
  cancelSearch(); keyword.value = query; error.value = ''
  const category = props.categoryId, excluded = props.excludeAssetId, current = requestId
  if (!category) { options.value = []; return }
  loading.value = true
  timer = setTimeout(async () => {
    try { const found = await fetchPredecessorOptions(category, query, excluded); if (current === requestId) options.value = found }
    catch (e) { if (current === requestId) { options.value = []; error.value = (e as Error).message } }
    finally { if (current === requestId) loading.value = false }
  }, 250)
}
watch(() => [props.modelValue, props.selected, props.categoryId] as const, ([id, initial, category]) => {
  if (!id) selectedAsset.value = null
  else if (initial?.id === id) selectedAsset.value = initial
  else selectedAsset.value = displayedOptions.value.find(item => item.id === id) || null
  if (selectedAsset.value && selectedAsset.value.categoryId !== category) {
    choose(null); ElMessage.info('分类已变更，请重新选择同类别的上代产品')
  }
}, { immediate: true })
watch(() => [props.categoryId, props.excludeAssetId], () => { cancelSearch(); options.value = []; error.value = ''; keyword.value = '' })
onBeforeUnmount(cancelSearch)
</script>

<style scoped>
.predecessor-picker{width:100%}.predecessor-picker :deep(.el-select){width:100%}.picker-hint{margin:8px 0 0;color:var(--dl-muted);font-size:10px;line-height:1.6}.picker-error{margin:8px 0;color:var(--dl-danger);font-size:12px}.picker-error button{border:0;background:none;color:inherit;text-decoration:underline;cursor:pointer}.selected-predecessor{display:grid;grid-template-columns:64px minmax(0,1fr) auto auto;align-items:center;gap:12px;margin-top:14px;padding:14px;border:1px solid #e1e9d7;border-radius:14px;background:#f4f8ed}.selected-cover{display:grid;place-items:center;width:64px;height:72px;padding:4px;border:0;border-radius:10px;background:#e9eee2;cursor:zoom-in}.selected-cover img{width:100%;height:100%;object-fit:contain}.selected-copy{display:flex;flex-direction:column;gap:5px;min-width:0}.selected-name{padding:0;border:0;background:none;text-align:left;overflow-wrap:anywhere;font-size:13px;font-weight:800;cursor:pointer}.selected-name:hover{text-decoration:underline}.selected-copy span,.selected-copy small{color:var(--dl-muted);font-size:10px}.picker-preview,.picker-clear{height:32px;padding:0 8px;border:0;border-radius:8px;background:#e4edd8;color:#446329;font-size:10px;font-weight:700;cursor:pointer}.picker-clear{background:none;color:var(--dl-muted)}.picker-preview:hover{background:#d8e7c6}.picker-clear:hover{color:var(--dl-danger)}.draft-comparison{grid-column:1/-1;display:flex;flex-wrap:wrap;gap:12px 24px;padding-top:10px;border-top:1px solid #dfe7d7}.draft-comparison>span{color:var(--dl-muted);font-size:10px}.draft-comparison strong{margin-left:7px;color:var(--dl-text);font-size:12px}.draft-comparison small{flex-basis:100%;color:#a06c15;font-size:10px}button:focus-visible{outline:2px solid #7da642;outline-offset:3px}@media(max-width:600px){.selected-predecessor{grid-template-columns:56px minmax(0,1fr) auto}.picker-clear{grid-column:2/4;justify-self:end}.selected-cover{width:56px;height:64px}}
</style>
<style>
.el-select-dropdown__item.predecessor-option{height:auto;min-height:64px;padding:9px 16px}.option-row{display:flex;align-items:center;gap:12px;max-width:600px}.option-row>img{flex:none;width:38px;height:42px;border-radius:7px;object-fit:contain;background:#eef1e9}.option-row>img.option-empty-image{padding:9px;opacity:.4}.option-row>div{flex:1;min-width:0;display:flex;flex-direction:column;gap:5px}.option-row strong{overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:12px}.option-row small{color:#899180;font-size:10px}.option-row>span{flex:none;font-size:11px;color:#5a6851}
</style>
