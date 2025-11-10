<template>
  <el-dialog v-model="visible" :title="isEdit ? '编辑资产' : '新建资产'" width="720px" @closed="reset">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="110px" status-icon>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-form-item label="资产名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入资产名称" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="资产类别" prop="category">
            <el-input v-model="form.category" placeholder="如：笔记本/相机" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-form-item label="品牌" prop="brand">
            <el-input v-model="form.brand" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="型号" prop="model">
            <el-input v-model="form.model" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-form-item label="序列号" prop="serialNo">
            <el-input v-model="form.serialNo" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="状态" prop="status">
            <el-select v-model="form.status" placeholder="选择状态">
              <el-option v-for="item in statuses" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-form-item label="购买日期" prop="purchaseDate">
            <el-date-picker v-model="form.purchaseDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="启用日期" prop="enabledDate">
            <el-date-picker v-model="form.enabledDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-form-item label="报废日期" prop="retiredDate">
            <el-date-picker v-model="form.retiredDate" type="date" value-format="YYYY-MM-DD" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="标签">
            <el-select v-model="form.tags" multiple filterable allow-create default-first-option placeholder="输入标签">
              <el-option v-for="tag in form.tags" :key="tag" :label="tag" :value="tag" />
            </el-select>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="封面图">
        <el-upload :http-request="handleUpload" :show-file-list="false" accept="image/*">
          <el-button type="primary">上传封面</el-button>
        </el-upload>
        <img v-if="form.coverImageUrl" :src="form.coverImageUrl" class="cover" />
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.notes" type="textarea" rows="3" placeholder="记录特殊说明" />
      </el-form-item>
      <div class="section-header">购买记录（可选）</div>
      <el-button type="primary" text @click="addPurchase">添加购买</el-button>
      <el-timeline v-if="form.purchases.length">
        <el-timeline-item v-for="(purchase, index) in form.purchases" :key="index" :timestamp="purchase.purchaseDate || '未选择'">
          <div class="purchase-card">
            <el-row :gutter="12">
              <el-col :xs="24" :md="8">
                <el-select v-model="purchase.type" placeholder="类型">
                  <el-option label="主购" value="PRIMARY" />
                  <el-option label="配件" value="ACCESSORY" />
                  <el-option label="服务" value="SERVICE" />
                </el-select>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-input v-model="purchase.platform" placeholder="平台" />
              </el-col>
              <el-col :xs="24" :md="8">
                <el-input-number v-model="purchase.price" :min="0" :precision="2" :step="100" />
              </el-col>
            </el-row>
            <el-row :gutter="12" class="mt">
              <el-col :xs="24" :md="8">
                <el-input-number v-model="purchase.shippingCost" :min="0" :precision="2" :step="10" placeholder="运费" />
              </el-col>
              <el-col :xs="24" :md="8">
                <el-date-picker v-model="purchase.purchaseDate" type="date" value-format="YYYY-MM-DD" />
              </el-col>
              <el-col :xs="24" :md="8">
                <el-upload :http-request="(options) => uploadAttachment(options, purchase)" :show-file-list="false" accept="image/*">
                  <el-button text>上传附件</el-button>
                </el-upload>
                <el-tag v-for="url in purchase.attachments" :key="url" size="small" class="tag">附件</el-tag>
              </el-col>
            </el-row>
            <div class="purchase-actions">
              <el-button type="danger" text @click="removePurchase(index)">删除</el-button>
            </div>
          </div>
        </el-timeline-item>
      </el-timeline>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, UploadRequestOptions } from 'element-plus'
import { uploadFile } from '@/api/file'
import { createAsset, updateAsset } from '@/api/asset'
import type { AssetDetail } from '@/types'

const statuses = ['使用中', '已闲置', '待出售', '已出售', '已丢弃']

const emit = defineEmits<{ (e: 'success'): void }>()

const visible = ref(false)
const loading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()

// 表单数据
const form = reactive({
  id: 0,
  name: '',
  category: '',
  brand: '',
  model: '',
  serialNo: '',
  status: '使用中',
  purchaseDate: '',
  enabledDate: '',
  retiredDate: '',
  coverImageUrl: '',
  tags: [] as string[],
  notes: '',
  purchases: [] as Array<{
    type: 'PRIMARY' | 'ACCESSORY' | 'SERVICE'
    platform?: string
    price: number
    shippingCost: number
    purchaseDate: string
    attachments: string[]
  }>
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  category: [{ required: true, message: '请输入类别', trigger: 'blur' }],
  enabledDate: [{ required: true, message: '请选择启用日期', trigger: 'change' }]
}

const open = (asset?: AssetDetail) => {
  visible.value = true
  isEdit.value = !!asset
  if (asset) {
    form.id = asset.id
    form.name = asset.name
    form.category = asset.category
    form.brand = asset.brand || ''
    form.model = asset.model || ''
    form.serialNo = asset.serialNo || ''
    form.status = asset.status
    form.purchaseDate = asset.purchaseDate || ''
    form.enabledDate = asset.enabledDate
    form.retiredDate = (asset as AssetDetail).retiredDate || ''
    form.coverImageUrl = asset.coverImageUrl || ''
    form.tags = [...(asset.tags || [])]
    form.notes = (asset as AssetDetail).notes || ''
    form.purchases = asset.purchases
      ? asset.purchases.map((p) => ({
          type: p.type,
          platform: p.platform,
          price: p.price,
          shippingCost: p.shippingCost,
          purchaseDate: p.purchaseDate,
          attachments: [...(p.attachments || [])]
        }))
      : []
  } else {
    reset()
  }
}

const reset = () => {
  form.id = 0
  form.name = ''
  form.category = ''
  form.brand = ''
  form.model = ''
  form.serialNo = ''
  form.status = '使用中'
  form.purchaseDate = ''
  form.enabledDate = ''
  form.retiredDate = ''
  form.coverImageUrl = ''
  form.tags = []
  form.notes = ''
  form.purchases = []
}

const addPurchase = () => {
  form.purchases.push({
    type: 'PRIMARY',
    platform: '',
    price: 0,
    shippingCost: 0,
    purchaseDate: '',
    attachments: []
  })
}

const removePurchase = (index: number) => {
  form.purchases.splice(index, 1)
}

// 上传封面
const handleUpload = async (options: UploadRequestOptions) => {
  try {
    const { url } = await uploadFile(options.file)
    form.coverImageUrl = url
    ElMessage.success('上传成功')
    options.onSuccess(url)
  } catch (err: any) {
    ElMessage.error(err.message || '上传失败')
    options.onError(err)
  }
}

const uploadAttachment = async (options: UploadRequestOptions, purchase: any) => {
  try {
    const { url } = await uploadFile(options.file)
    purchase.attachments.push(url)
    ElMessage.success('附件上传成功')
    options.onSuccess(url)
  } catch (err: any) {
    options.onError(err)
    ElMessage.error('附件上传失败')
  }
}

const submit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const payload = {
        name: form.name,
        category: form.category,
        brand: form.brand,
        model: form.model,
        serialNo: form.serialNo,
        status: form.status,
        purchaseDate: form.purchaseDate || undefined,
        enabledDate: form.enabledDate,
        retiredDate: form.retiredDate || undefined,
        coverImageUrl: form.coverImageUrl || undefined,
        notes: form.notes || undefined,
        tags: form.tags,
        purchases: form.purchases.map((p) => ({
          type: p.type,
          platform: p.platform,
          price: p.price,
          shippingCost: p.shippingCost,
          purchaseDate: p.purchaseDate,
          attachments: p.attachments
        }))
      }
      if (isEdit.value) {
        await updateAsset(form.id, payload)
        ElMessage.success('更新成功')
      } else {
        await createAsset(payload)
        ElMessage.success('创建成功')
      }
      visible.value = false
      emit('success')
    } finally {
      loading.value = false
    }
  })
}

defineExpose({ open })
</script>

<style scoped>
.section-header {
  margin-top: 16px;
  margin-bottom: 8px;
  font-weight: 600;
  color: #38bdf8;
}

.cover {
  width: 96px;
  height: 96px;
  border-radius: 8px;
  margin-left: 16px;
  object-fit: cover;
}

.purchase-card {
  background: rgba(15, 23, 42, 0.4);
  padding: 12px;
  border-radius: 12px;
}

.mt {
  margin-top: 12px;
}

.tag {
  margin-right: 6px;
}

.purchase-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 8px;
}
</style>
