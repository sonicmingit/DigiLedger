<template>
  <el-dialog
    :model-value="modelValue"
    :title="asset.predecessorAssetId ? '更换上代产品' : '关联上代产品'"
    width="min(720px, calc(100vw - 32px))"
    align-center
    destroy-on-close
    @update:model-value="$emit('update:modelValue', $event)"
  >
    <div class="manage-intro">
      <span class="manage-category">{{ categoryLabel }}</span>
      <strong>{{ asset.name }}</strong>
      <p>搜索并选择同类别物品。保存只会更新上代产品关联。</p>
    </div>
    <el-form label-position="top">
      <el-form-item label="上代产品">
        <AssetPredecessorPicker
          v-model="selectedId"
          :category-id="asset.categoryId"
          :exclude-asset-id="asset.id"
          :selected="asset.predecessorAsset"
          :current-purchase="currentPurchase"
          @preview="preview"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <div class="manage-footer">
        <button type="button" class="secondary-button" :disabled="saving" @click="$emit('update:modelValue', false)">取消</button>
        <button type="button" class="primary-button" :disabled="saving || !changed" @click="save">
          {{ saving ? '正在保存…' : '保存关联' }}
        </button>
      </div>
    </template>
  </el-dialog>
  <AssetPreviewDialog v-model="previewOpen" :asset-id="previewId" :category-label="categoryLabel" new-tab />
</template>

<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { updateAssetPredecessor } from '@/api/assets'
import type { AssetDetail } from '@/types'
import { primaryPurchaseRecord } from '@/utils/predecessor'
import AssetPredecessorPicker from './AssetPredecessorPicker.vue'
import AssetPreviewDialog from './AssetPreviewDialog.vue'

const props = defineProps<{ modelValue: boolean; asset: AssetDetail; categoryLabel: string }>()
const emit = defineEmits<{ 'update:modelValue': [value: boolean]; saved: [] }>()
const selectedId = ref<number | null>(null)
const saving = ref(false)
const previewOpen = ref(false)
const previewId = ref<number>()
const currentPurchase = computed(() => primaryPurchaseRecord(props.asset.purchases))
const changed = computed(() => selectedId.value !== (props.asset.predecessorAssetId ?? null))

watch(() => [props.modelValue, props.asset.id] as const, ([open]) => {
  if (open) selectedId.value = props.asset.predecessorAssetId ?? null
  else previewOpen.value = false
})

function preview(id: number) {
  previewId.value = id
  previewOpen.value = true
}

async function save() {
  if (!changed.value || saving.value) return
  saving.value = true
  try {
    await updateAssetPredecessor(props.asset.id, selectedId.value)
    ElMessage.success('上代产品关联已保存')
    emit('update:modelValue', false)
    emit('saved')
  } catch (error) {
    ElMessage.error((error as Error).message)
  } finally {
    saving.value = false
  }
}
</script>

<style scoped>
.manage-intro{display:flex;flex-direction:column;align-items:flex-start;gap:8px;margin-bottom:22px}.manage-category{padding:5px 10px;border-radius:999px;background:var(--dl-accent-soft);color:#3f601e;font-size:11px;font-weight:700}.manage-intro>strong{font-size:18px;overflow-wrap:anywhere}.manage-intro p{margin:0;color:var(--dl-muted);font-size:12px;line-height:1.6}.manage-footer{display:flex;justify-content:flex-end;gap:10px}.manage-footer button{min-width:96px;height:40px;padding:0 16px;border:0;border-radius:11px;font-size:12px;font-weight:700;cursor:pointer}.manage-footer .primary-button{background:var(--dl-lime);color:var(--dl-text)}.manage-footer .primary-button:hover:not(:disabled){background:#a5ed29}.manage-footer button:disabled{cursor:not-allowed;opacity:.55}.manage-footer button:focus-visible{outline:2px solid #7da642;outline-offset:3px}
</style>
