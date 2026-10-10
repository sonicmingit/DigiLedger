<template>
  <el-dialog :model-value="modelValue" title="上代产品预览" width="min(780px, calc(100vw - 32px))" align-center destroy-on-close @update:model-value="$emit('update:modelValue', $event)" @closed="cancelRequest">
    <AsyncState :loading="loading" :error="error" :empty="!asset" @retry="load">
      <div v-if="asset" class="asset-quick-preview">
        <div class="preview-cover"><img v-if="asset.coverImageUrl" :src="asset.coverImageUrl" :alt="asset.name" /><div v-else class="missing-cover"><img :src="assetIcon" alt="" /><span>暂无封面</span></div></div>
        <div class="preview-copy">
          <span class="preview-category">{{ categoryLabel || '上代物品档案' }}</span>
          <h2>{{ asset.name }}</h2><span class="tag">{{ asset.status }}</span>
          <div class="preview-price"><span>主商品购买价</span><strong>{{ nullableMoney(primary?.price) }}</strong></div>
          <dl><div><dt>购买日期</dt><dd>{{ primary?.purchaseDate || '未填写' }}</dd></div><div><dt>品牌</dt><dd>{{ asset.brand?.name || asset.brandName || '未填写' }}</dd></div><div><dt>型号</dt><dd>{{ asset.model || '未填写' }}</dd></div><div><dt>购买平台</dt><dd>{{ primary?.platformName || primary?.seller || '未填写' }}</dd></div></dl>
        </div>
        <div v-if="asset.specifications" class="preview-description"><strong>配置规格</strong><p>{{ asset.specifications }}</p></div>
        <div v-if="asset.notes" class="preview-description"><strong>备注</strong><p>{{ asset.notes }}</p></div>
      </div>
    </AsyncState>
    <template #footer><div class="preview-footer"><button type="button" class="secondary-button" @click="$emit('update:modelValue', false)">关闭预览</button><RouterLink v-if="asset" class="full-detail-link" :to="`/assets/${asset.id}`" :target="newTab ? '_blank' : undefined" :rel="newTab ? 'noopener' : undefined" @click="navigate">查看完整详情{{ newTab ? '（新窗口）' : '' }}</RouterLink></div></template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import type { AssetDetail } from '@/types'
import { fetchAsset } from '@/api/assets'
import { nullableMoney, primaryPurchaseRecord } from '@/utils/predecessor'
import AsyncState from './AsyncState.vue'
import assetIcon from '@/assets/icons/assets.svg'
const props = defineProps<{ modelValue: boolean; assetId?: number; categoryLabel?: string; newTab?: boolean }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean] }>()
const asset = ref<AssetDetail>(), loading = ref(false), error = ref('')
let requestId = 0
const primary = computed(() => primaryPurchaseRecord(asset.value?.purchases))
function cancelRequest() { requestId++; loading.value = false }
async function load() {
  const id = props.assetId, current = ++requestId
  asset.value = undefined; error.value = ''; loading.value = true
  if (!id) { loading.value = false; return }
  try { const result = await fetchAsset(id); if (current === requestId && props.modelValue) asset.value = result }
  catch (e) { if (current === requestId) error.value = (e as Error).message }
  finally { if (current === requestId) loading.value = false }
}
function navigate() { if (!props.newTab) emit('update:modelValue', false) }
watch(() => [props.modelValue, props.assetId] as const, ([open]) => { if (open) load(); else cancelRequest() })
</script>

<style scoped>
.asset-quick-preview{display:grid;grid-template-columns:280px minmax(0,1fr);gap:28px;padding:8px 4px}.preview-cover{display:grid;place-items:center;min-height:280px;overflow:hidden;border-radius:20px;background:#f3f5ef}.preview-cover>img{width:100%;height:280px;padding:14px;object-fit:contain}.missing-cover{display:grid;place-items:center;gap:12px;color:var(--dl-muted);font-size:12px}.missing-cover img{width:40px;height:40px;opacity:.35}.preview-category{color:#6e825b;font-size:12px}.preview-copy h2{margin:10px 0 12px;font-size:23px;line-height:1.4;overflow-wrap:anywhere}.preview-copy .tag{font-size:11px}.preview-price{margin-top:20px}.preview-price span{display:block;color:var(--dl-muted);font-size:12px}.preview-price strong{display:block;margin-top:6px;font-size:25px}.preview-copy dl{margin:20px 0 0;display:grid;gap:14px}.preview-copy dl div{display:grid;grid-template-columns:76px minmax(0,1fr);gap:10px;font-size:12px}.preview-copy dt{color:var(--dl-muted)}.preview-copy dd{margin:0;overflow-wrap:anywhere}.preview-description{grid-column:1/-1;padding-top:18px;border-top:1px solid #e8ece3}.preview-description strong{font-size:12px}.preview-description p{margin:8px 0 0;color:var(--dl-text-secondary);font-size:12px;line-height:1.8;white-space:pre-wrap;overflow-wrap:anywhere}.preview-footer{display:flex;justify-content:flex-end;align-items:center;gap:12px}.preview-footer .secondary-button{height:40px;border-radius:12px;font-size:12px}.full-detail-link{display:inline-flex;align-items:center;min-height:40px;padding:0 18px;border-radius:12px;background:var(--dl-lime);color:var(--dl-text);font-size:12px;font-weight:700;text-decoration:none}.full-detail-link:hover{background:#a5ed29}.full-detail-link:focus-visible{outline:2px solid #5e8a25;outline-offset:3px}@media(max-width:760px){.asset-quick-preview{grid-template-columns:minmax(0,1fr)}.preview-cover>img{height:220px}.preview-cover{min-height:220px}}
</style>
