<template>
  <el-dialog v-model="visible" title="出售向导" width="500px" @closed="reset">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
      <el-form-item v-if="accessoryPurchases.length" label="选择配件" prop="purchaseId">
        <el-select
          v-model="form.purchaseId"
          placeholder="请选择要出售的配件"
          filterable
          :loading="accessoryLoading"
          :disabled="!accessoryPurchases.length"
        >
          <el-option
            v-for="item in accessoryPurchases"
            :key="item.id"
            :label="`${item.name || '待处理采购'} - ${item.purchaseDate || '未知'}`"
            :value="item.id"
          />
        </el-select>
      </el-form-item>

      <el-form-item label="平台" prop="platformId">
        <el-select v-model="form.platformId" placeholder="选择平台" filterable clearable>
          <el-option v-for="item in platforms" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
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
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">确认出售</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue'
import { fetchAssetDetail, sellAsset } from '@/api/asset'
import { ElMessage } from 'element-plus'
import type { FormInstance } from 'element-plus'
import { useDictionaries } from '@/composables/useDictionaries'
import type { PurchaseRecord } from '@/types'

const emit = defineEmits<{ (e: 'success'): void }>()

const visible = ref(false)
const assetInfo = ref<{ id: number; name: string } | null>(null)
const loading = ref(false)

const { load: loadDicts, platforms } = useDictionaries()

const form = reactive({
  platformId: undefined as number | undefined,
  purchaseId: undefined as number | undefined,
  buyer: '',
  salePrice: 0,
  fee: 0,
  shippingCost: 0,
  otherCost: 0,
  saleDate: '',
  notes: ''
})

const formRef = ref<FormInstance>()

const rules = {
  salePrice: [{ required: true, message: '�������ۼ�', trigger: 'blur' }],
  saleDate: [{ required: true, message: '��ѡ���۳�����', trigger: 'change' }]
}

const accessoryPurchases = ref<PurchaseRecord[]>([])
const accessoryLoading = ref(false)

const loadAccessoryPurchases = async (assetId: number) => {
  accessoryLoading.value = true
  try {
    const detail = await fetchAssetDetail(assetId)
    accessoryPurchases.value = detail.purchases.filter((item) => item.type === 'ACCESSORY')
  } finally {
    accessoryLoading.value = false
  }
}

const open = (asset: { id: number; name: string }) => {
  assetInfo.value = asset
  visible.value = true
  accessoryPurchases.value = []
  if (asset.id) {
    void loadAccessoryPurchases(asset.id)
  }
}

const reset = () => {
  Object.assign(form, {
    platformId: undefined,
    purchaseId: undefined,
    buyer: '',
    salePrice: 0,
    fee: 0,
    shippingCost: 0,
    otherCost: 0,
    saleDate: '',
    notes: ''
  })
  accessoryPurchases.value = []
  formRef.value?.clearValidate()
}

const submit = () => {
  if (!assetInfo.value) return
  formRef.value?.validate(async (valid: boolean) => {
    if (!valid) return
    loading.value = true
    try {
      await sellAsset(assetInfo.value!.id, { ...form })
      ElMessage.success('售出成功')
      visible.value = false
      emit('success')
    } finally {
      loading.value = false
    }
  })
}

defineExpose({ open })

onMounted(async () => {
  await loadDicts()
})
</script>
