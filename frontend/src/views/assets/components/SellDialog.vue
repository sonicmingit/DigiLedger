<template>
  <el-dialog v-model="visible" :title="dialogTitle" width="520px" @closed="reset">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
      <el-form-item label="出售范围" prop="saleScope">
        <el-radio-group v-model="form.saleScope">
          <el-radio-button label="ASSET">主商品</el-radio-button>
          <el-radio-button label="ACCESSORY">配件</el-radio-button>
        </el-radio-group>
      </el-form-item>
      <el-form-item v-if="form.saleScope === 'ACCESSORY'" label="选择配件" prop="purchaseId">
        <el-select
          v-model="form.purchaseId"
          placeholder="请选择需要出售的配件"
          filterable
          :loading="accessoryLoading"
          :disabled="!accessoryPurchases.length"
        >
          <el-option
            v-for="item in accessoryPurchases"
            :key="item.id"
            :label="`${item.name || '未命名配件'} · 购买于 ${item.purchaseDate || '未知'}`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="平台" prop="platformId">
        <div class="selector-with-action">
          <el-select v-model="form.platformId" placeholder="选择平台" filterable clearable>
            <el-option v-for="item in platforms" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-button class="inline-action" text size="small" type="primary" @click="handleCreatePlatform">
            新建
          </el-button>
        </div>
      </el-form-item>
      <el-form-item label="买家" prop="buyer">
        <el-input v-model="form.buyer" placeholder="可选填写" />
      </el-form-item>
      <el-form-item label="售出价格" prop="salePrice">
        <el-input-number v-model="form.salePrice" :min="0" :precision="2" :step="100" />
      </el-form-item>
      <el-form-item label="手续费" prop="fee">
        <el-input-number v-model="form.fee" :min="0" :precision="2" :step="10" />
      </el-form-item>
      <el-form-item label="运费" prop="shippingCost">
        <el-input-number v-model="form.shippingCost" :min="0" :precision="2" :step="10" />
      </el-form-item>
      <el-form-item label="其他费用" prop="otherCost">
        <el-input-number v-model="form.otherCost" :min="0" :precision="2" :step="10" />
      </el-form-item>
      <el-form-item label="售出日期" prop="saleDate">
        <el-date-picker v-model="form.saleDate" type="date" value-format="YYYY-MM-DD" />
      </el-form-item>
      <el-form-item label="备注" prop="notes">
        <el-input v-model="form.notes" type="textarea" rows="3" placeholder="可记录交易说明" />
      </el-form-item>
      <el-form-item label="附件">
        <div class="attachments">
          <el-upload :http-request="uploadAttachment" :show-file-list="false" multiple>
            <el-button text type="primary">上传附件</el-button>
          </el-upload>
          <div v-for="(item, index) in form.attachments" :key="`${item}-${index}`" class="attachment">
            <el-image
              v-if="isImage(item)"
              :src="resolveOssUrl(item)"
              fit="cover"
              :preview-src-list="[resolveOssUrl(item)]"
              class="attachment-image"
            />
            <el-link v-else :href="resolveOssUrl(item)" target="_blank" type="primary">附件{{ index + 1 }}</el-link>
            <el-button text type="danger" size="small" @click="removeAttachment(index)">删除</el-button>
          </div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">
        {{ isEdit ? '保存' : '确认出售' }}
      </el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { computed, ref, reactive, onMounted, watch } from 'vue'
import { fetchAssetDetail, sellAsset, updateSale } from '@/api/asset'
import { ElMessage } from 'element-plus'
import type { FormInstance, UploadRequestOptions } from 'element-plus'
import { useDictionaries } from '@/composables/useDictionaries'
import { useDictionaryCreator } from '@/composables/useDictionaryCreator'
import type { PurchaseRecord, SaleRecord } from '@/types'
import { uploadFile } from '@/api/file'
import { buildOssUrl, extractObjectKeys } from '@/utils/storage'

const emit = defineEmits<{ (e: 'success'): void }>()

const visible = ref(false)
const assetInfo = ref<{ id: number; name: string } | null>(null)
const editingSaleId = ref<number | null>(null)
const loading = ref(false)

const { load: loadDicts, platforms } = useDictionaries()
const { promptPlatformCreation } = useDictionaryCreator()

const form = reactive({
  platformId: undefined as number | undefined,
  saleScope: 'ASSET' as 'ASSET' | 'ACCESSORY',
  purchaseId: undefined as number | undefined,
  buyer: '',
  salePrice: 0,
  fee: 0,
  shippingCost: 0,
  otherCost: 0,
  saleDate: '',
  notes: '',
  attachments: [] as string[]
})

const formRef = ref<FormInstance>()

const isEdit = computed(() => editingSaleId.value !== null)
const dialogTitle = computed(() => (isEdit.value ? '编辑售出记录' : '出售向导'))

const rules = {
  saleScope: [{ required: true, message: '请选择出售范围', trigger: 'change' }],
  purchaseId: [
    {
      trigger: 'change',
      validator: (_rule: unknown, value: number | undefined, callback: (error?: Error) => void) => {
        if (form.saleScope === 'ACCESSORY' && !value) {
          callback(new Error('请选择配件'))
        } else {
          callback()
        }
      }
    }
  ],
  salePrice: [{ required: true, message: '请输入售价', trigger: 'blur' }],
  saleDate: [{ required: true, message: '请选择售出日期', trigger: 'change' }]
}

const accessoryPurchases = ref<PurchaseRecord[]>([])
const accessoryLoading = ref(false)

const loadAccessoryPurchases = async (assetId: number) => {
  accessoryLoading.value = true
  try {
    const detail = await fetchAssetDetail(assetId)
    const soldPurchaseIds = new Set(
      (detail.sales || [])
        .filter((sale) => sale.saleScope === 'ACCESSORY' && sale.purchaseId)
        .map((sale) => sale.purchaseId!)
    )
    if (form.purchaseId) {
      soldPurchaseIds.delete(form.purchaseId)
    }
    accessoryPurchases.value = detail.purchases
      .filter((item) => item.type === 'ACCESSORY')
      .filter((item) => !soldPurchaseIds.has(item.id))
  } finally {
    accessoryLoading.value = false
  }
}

const fillFormFromSale = (sale: SaleRecord) => {
  form.platformId = sale.platformId
  form.saleScope = sale.saleScope
  form.purchaseId = sale.purchaseId
  form.buyer = sale.buyer || ''
  form.salePrice = sale.salePrice
  form.fee = sale.fee
  form.shippingCost = sale.shippingCost
  form.otherCost = sale.otherCost
  form.saleDate = sale.saleDate
  form.notes = sale.notes || ''
  form.attachments = Array.isArray(sale.attachments) ? [...sale.attachments] : []
}

const open = (asset: { id: number; name: string }, sale?: SaleRecord) => {
  assetInfo.value = asset
  visible.value = true
  accessoryPurchases.value = []
  reset()
  if (sale) {
    editingSaleId.value = sale.id
    fillFormFromSale(sale)
  }
  if (asset.id) {
    void loadAccessoryPurchases(asset.id)
  }
}

const reset = () => {
  Object.assign(form, {
    platformId: undefined,
    saleScope: 'ASSET' as 'ASSET' | 'ACCESSORY',
    purchaseId: undefined,
    buyer: '',
    salePrice: 0,
    fee: 0,
    shippingCost: 0,
    otherCost: 0,
    saleDate: '',
    notes: '',
    attachments: [] as string[]
  })
  accessoryPurchases.value = []
  editingSaleId.value = null
  formRef.value?.clearValidate()
}

const submit = () => {
  if (!assetInfo.value) return
  if (form.saleScope === 'ACCESSORY' && !accessoryPurchases.value.length) {
    ElMessage.warning('当前资产暂无可出售的配件')
    return
  }
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    loading.value = true
    try {
      const payload = { ...form, attachments: extractObjectKeys(form.attachments) }
      if (editingSaleId.value) {
        await updateSale(assetInfo.value!.id, editingSaleId.value, payload)
        ElMessage.success('售出记录已更新')
      } else {
        await sellAsset(assetInfo.value!.id, payload)
        ElMessage.success('售出成功')
      }
      visible.value = false
      emit('success')
    } finally {
      loading.value = false
    }
  })
}

const handleCreatePlatform = async () => {
  try {
    const result = await promptPlatformCreation()
    if (result) {
      form.platformId = result.id
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '创建平台失败')
  }
}

defineExpose({ open })

onMounted(async () => {
  await loadDicts()
})

watch(
  () => form.saleScope,
  (scope) => {
    if (scope === 'ASSET') {
      form.purchaseId = undefined
      formRef.value?.clearValidate('purchaseId')
    } else if (scope === 'ACCESSORY' && assetInfo.value) {
      if (!accessoryPurchases.value.length && !accessoryLoading.value) {
        void loadAccessoryPurchases(assetInfo.value.id)
      }
    }
  }
)

const uploadAttachment = async (options: UploadRequestOptions) => {
  try {
    const { objectKey, url } = await uploadFile(options.file)
    const stored = url || objectKey
    if (stored) {
      form.attachments.push(stored)
    }
    ElMessage.success('附件上传成功')
    options.onSuccess?.(objectKey)
  } catch (error: any) {
    options.onError?.(error)
    ElMessage.error(error?.message || '附件上传失败')
  }
}

const removeAttachment = (index: number) => {
  form.attachments.splice(index, 1)
}

const resolveOssUrl = (value?: string | null) => buildOssUrl(value)

const isImage = (value: string) => {
  const text = (value || '').toLowerCase()
  const pure = text.split('?')[0]
  return /(\.png|\.jpe?g|\.gif|\.bmp|\.webp|\.svg)$/.test(pure)
}
</script>

<style scoped>
.selector-with-action {
  display: flex;
  align-items: center;
  gap: 8px;
}

.inline-action {
  padding: 0 6px;
}

.attachments {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.attachment {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.attachment-image {
  width: 96px;
  height: 96px;
  border-radius: 8px;
  object-fit: cover;
}
</style>
