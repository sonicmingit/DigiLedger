<template>
  <el-dialog v-model="visible" :title="title" width="520px" @closed="reset">
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" status-icon>
      <el-form-item label="类型" prop="type">
        <el-select v-model="form.type" @change="handleTypeChange">
          <el-option label="主商品" value="PRIMARY" />
          <el-option label="配件" value="ACCESSORY" />
          <el-option label="服务" value="SERVICE" />
        </el-select>
      </el-form-item>
      <el-form-item v-if="form.type !== 'PRIMARY'" label="名称" prop="name">
        <el-input v-model="form.name" placeholder="请输入配件/服务名称" />
      </el-form-item>
      <el-form-item label="平台">
        <el-select v-model="form.platformId" filterable clearable placeholder="来源平台">
          <el-option v-for="item in platforms" :key="item.id" :label="item.name" :value="item.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="价格" prop="price">
        <el-input-number v-model="form.price" :min="0" :precision="2" :step="100" />
      </el-form-item>
      <el-form-item label="运费">
        <el-input-number v-model="form.shippingCost" :min="0" :precision="2" :step="10" />
      </el-form-item>
      <el-form-item label="数量" prop="quantity">
        <el-input-number v-model="form.quantity" :min="1" :step="1" />
      </el-form-item>
      <el-form-item label="购买日期" prop="purchaseDate">
        <el-date-picker
          v-model="form.purchaseDate"
          type="date"
          value-format="YYYY-MM-DD"
          :shortcuts="dateShortcuts"
        />
      </el-form-item>
      <el-form-item label="卖家">
        <el-input v-model="form.seller" />
      </el-form-item>
      <el-form-item label="发票编号">
        <el-input v-model="form.invoiceNo" />
      </el-form-item>
      <el-form-item label="质保（月）">
        <el-input-number v-model="form.warrantyMonths" :min="0" :step="1" />
      </el-form-item>
      <el-form-item label="质保到期">
        <el-date-picker
          v-model="form.warrantyExpireDate"
          type="date"
          value-format="YYYY-MM-DD"
          :shortcuts="dateShortcuts"
          clearable
        />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.notes" type="textarea" rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import type { FormInstance } from 'element-plus'
import { useDictionaries } from '@/composables/useDictionaries'
import type { PurchaseRecord } from '@/types'

const emit = defineEmits<{
  (e: 'submit', payload: Partial<PurchaseRecord>): void
}>()

const visible = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()
const { load: loadDicts, platforms } = useDictionaries()

const form = reactive({
  id: 0,
  type: 'ACCESSORY' as PurchaseRecord['type'],
  name: '',
  platformId: undefined as number | undefined,
  price: 0,
  shippingCost: 0,
  quantity: 1,
  purchaseDate: new Date().toISOString().slice(0, 10),
  seller: '',
  invoiceNo: '',
  warrantyMonths: undefined as number | undefined,
  warrantyExpireDate: '',
  notes: ''
})

const dateShortcuts = [
  {
    text: '今天',
    value: () => new Date()
  }
]

const rules = {
  type: [{ required: true, message: '请选择类型', trigger: 'change' }],
  name: [
    {
      validator: (_: unknown, value: string, callback: (error?: Error) => void) => {
        if (form.type === 'PRIMARY' || (value && value.trim())) {
          callback()
        } else {
          callback(new Error('请填写名称'))
        }
      },
      trigger: 'blur'
    }
  ],
  price: [{ required: true, message: '请输入价格', trigger: 'blur' }],
  quantity: [{ required: true, message: '请输入数量', trigger: 'change' }],
  purchaseDate: [{ required: true, message: '请选择购买日期', trigger: 'change' }]
}

const title = ref('新增购买记录')

const open = async (purchase?: PurchaseRecord) => {
  await loadDicts()
  visible.value = true
  loading.value = false
  if (purchase) {
    title.value = '编辑购买记录'
    form.id = purchase.id
    form.type = purchase.type
    form.name = purchase.name || ''
    form.platformId = purchase.platformId
    form.price = purchase.price
    form.shippingCost = purchase.shippingCost
    form.quantity = purchase.quantity
    form.purchaseDate = purchase.purchaseDate
    form.seller = purchase.seller || ''
    form.invoiceNo = purchase.invoiceNo || ''
    form.warrantyMonths = purchase.warrantyMonths || undefined
    form.warrantyExpireDate = purchase.warrantyExpireDate || ''
    form.notes = purchase.notes || ''
  } else {
    title.value = '新增附属品'
    reset()
  }
}

const reset = () => {
  form.id = 0
  form.type = 'ACCESSORY'
  form.name = ''
  form.platformId = undefined
  form.price = 0
  form.shippingCost = 0
  form.quantity = 1
  form.purchaseDate = new Date().toISOString().slice(0, 10)
  form.seller = ''
  form.invoiceNo = ''
  form.warrantyMonths = undefined
  form.warrantyExpireDate = ''
  form.notes = ''
}

const handleTypeChange = () => {
  if (form.type === 'PRIMARY') {
    form.name = ''
  }
}

const submit = () => {
  formRef.value?.validate((valid) => {
    if (!valid) return
    loading.value = true
    emit('submit', { ...form })
  })
}

const setLoading = (value: boolean) => {
  loading.value = value
}

const close = () => {
  visible.value = false
}

defineExpose({ open, reset, setLoading, close })
</script>

<style scoped>
</style>
