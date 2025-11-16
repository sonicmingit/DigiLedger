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
        <div class="platform-field">
          <el-select v-model="form.platformId" filterable clearable placeholder="来源平台">
            <el-option v-for="item in platforms" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-button class="inline-action" text size="small" type="primary" @click="handleCreatePlatform">
            新建
          </el-button>
        </div>
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
          @change="syncWarranty"
        />
      </el-form-item>
      <el-form-item label="卖家">
        <el-input v-model="form.seller" />
      </el-form-item>
      <el-form-item label="质保（月）">
        <el-input-number v-model="form.warrantyMonths" :min="0" :step="1" @change="syncWarranty" />
      </el-form-item>
      <el-form-item label="质保到期日">
        <el-date-picker
          v-model="form.warrantyExpireDate"
          type="date"
          value-format="YYYY-MM-DD"
          :shortcuts="dateShortcuts"
          disabled
        />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.notes" type="textarea" rows="2" />
      </el-form-item>
      <el-form-item label="附件">
        <div class="attachments">
          <el-upload
            :http-request="uploadAttachment"
            :show-file-list="false"
            multiple
          >
            <el-button text type="primary">上传图片</el-button>
          </el-upload>
          <div
            v-for="(item, index) in form.attachments"
            :key="`${item}-${index}`"
            class="attachment"
          >
            <el-image
              v-if="isImage(item)"
              :src="resolveOssUrl(item)"
              fit="cover"
              :preview-src-list="[resolveOssUrl(item)]"
              class="attachment-image"
            />
            <el-link v-else :href="resolveOssUrl(item)" target="_blank" type="primary">附件{{ index + 1 }}</el-link>
            <el-button
              class="remove"
              text
              type="danger"
              size="small"
              @click="removeAttachment(index)"
            >
              删除
            </el-button>
          </div>
        </div>
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, UploadRequestOptions } from 'element-plus'
import { uploadFile } from '@/api/file'
import { useDictionaries } from '@/composables/useDictionaries'
import { useDictionaryCreator } from '@/composables/useDictionaryCreator'
import type { PurchaseRecord } from '@/types'
import { buildOssUrl, extractObjectKey } from '@/utils/storage'
import { calcWarrantyExpireDate } from '@/utils/date'

const emit = defineEmits<{
  (e: 'submit', payload: Partial<PurchaseRecord>): void
}>()

const visible = ref(false)
const loading = ref(false)
const formRef = ref<FormInstance>()
const { load: loadDicts, platforms } = useDictionaries()
const { promptPlatformCreation } = useDictionaryCreator()

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
  warrantyMonths: undefined as number | undefined,
  warrantyExpireDate: '',
  notes: '',
  attachments: [] as string[]
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

const syncWarranty = () => {
  form.warrantyExpireDate = calcWarrantyExpireDate(form.purchaseDate, form.warrantyMonths)
}

watch(
  () => [form.purchaseDate, form.warrantyMonths],
  () => syncWarranty()
)

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
    form.warrantyMonths = purchase.warrantyMonths || undefined
    form.warrantyExpireDate = purchase.warrantyExpireDate || ''
    form.notes = purchase.notes || ''
    form.attachments = Array.isArray(purchase.attachments)
      ? purchase.attachments.map((item) => extractObjectKey(item) || item)
      : []
  } else {
    title.value = '新增附属品'
    reset()
  }
  syncWarranty()
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
  form.warrantyMonths = undefined
  form.warrantyExpireDate = ''
  form.notes = ''
  form.attachments = []
  syncWarranty()
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
    emit('submit', { ...form, attachments: [...form.attachments] })
  })
}

const setLoading = (value: boolean) => {
  loading.value = value
}

const close = () => {
  visible.value = false
}

const uploadAttachment = async (options: UploadRequestOptions) => {
  try {
    const { objectKey, url } = await uploadFile(options.file)
    const stored = objectKey || extractObjectKey(url) || url
    form.attachments.push(stored)
    ElMessage.success('附件上传成功')
    options.onSuccess(objectKey)
  } catch (error: any) {
    options.onError(error)
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

defineExpose({ open, reset, setLoading, close })
</script>

<style scoped>
.platform-field {
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
  position: relative;
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

.remove {
  padding: 0;
}
</style>
