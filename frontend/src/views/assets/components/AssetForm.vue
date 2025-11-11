<template>
  <el-dialog v-model="visible" :title="isEdit ? '编辑物品' : '新建物品'" width="720px" @closed="reset">
    <el-form :model="form" :rules="rules" ref="formRef" label-width="110px" status-icon>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-form-item label="物品名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入物品名称" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="物品类别" prop="categoryId">
            <el-tree-select
              v-model="form.categoryId"
              :data="categoryOptions"
              :props="treeProps"
              check-strictly
              filterable
              placeholder="请选择类别（叶子节点）"
              style="width: 100%"
            />
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
            <el-tree-select
              v-model="form.tagIds"
              :data="tagOptions"
              :props="treeProps"
              multiple
              show-checkbox
              filterable
              collapse-tags
              placeholder="选择标签"
              style="width: 100%"
            />
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
                <el-select v-model="purchase.platformId" placeholder="来源平台" filterable clearable>
                  <el-option v-for="item in platforms" :key="item.id" :label="item.name" :value="item.id" />
                </el-select>
              </el-col>
              <el-col :xs="24" :md="8">
                <el-input-number v-model="purchase.price" :min="0" :precision="2" :step="100" />
              </el-col>
            </el-row>
            <el-row :gutter="12" class="mt">
              <el-col :xs="24" :md="8">
                <el-input v-model="purchase.currency" placeholder="币种（如 CNY）" />
              </el-col>
              <el-col :xs="24" :md="8">
                <el-input-number v-model="purchase.quantity" :min="1" :step="1" />
              </el-col>
              <el-col :xs="24" :md="8">
                <el-input v-model="purchase.seller" placeholder="卖家/店铺" />
              </el-col>
            </el-row>
            <el-row :gutter="12" class="mt">
              <el-col :xs="24" :md="8">
                <el-input-number v-model="purchase.shippingCost" :min="0" :precision="2" :step="10" placeholder="运费" />
              </el-col>
              <el-col :xs="24" :md="8">
                <el-date-picker v-model="purchase.purchaseDate" type="date" value-format="YYYY-MM-DD" placeholder="购买日期" clearable />
              </el-col>
              <el-col :xs="24" :md="8">
                <el-input v-model="purchase.invoiceNo" placeholder="发票编号" />
              </el-col>
            </el-row>
            <el-row :gutter="12" class="mt">
              <el-col :xs="24" :md="8">
                <el-input-number
                  v-model="purchase.warrantyMonths"
                  :min="0"
                  :step="1"
                  placeholder="质保（月）"
                  controls-position="right"
                />
              </el-col>
              <el-col :xs="24" :md="8">
                <el-date-picker
                  v-model="purchase.warrantyExpireDate"
                  type="date"
                  value-format="YYYY-MM-DD"
                  placeholder="质保到期日"
                  clearable
                />
              </el-col>
              <el-col :xs="24" :md="8">
                <el-input v-model="purchase.notes" placeholder="备注" />
              </el-col>
            </el-row>
            <el-row :gutter="12" class="mt">
              <el-col :xs="24">
                <el-upload :http-request="(options) => uploadAttachment(options, purchase)" :show-file-list="false" accept="image/*">
                  <el-button text>上传附件</el-button>
                </el-upload>
                <el-tag
                  v-for="(url, idx) in purchase.attachments"
                  :key="url"
                  size="small"
                  class="tag"
                  closable
                  @close="removeAttachment(purchase, url)"
                >
                  附件{{ idx + 1 }}
                </el-tag>
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
import { reactive, ref, computed, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, UploadRequestOptions } from 'element-plus'
import { uploadFile } from '@/api/file'
import { createAsset, updateAsset } from '@/api/asset'
import type { AssetDetail } from '@/types'
import { useDictionaries } from '@/composables/useDictionaries'
import type { CategoryNode, TagNode } from '@/api/dict'

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
  categoryId: null as number | null,
  brand: '',
  model: '',
  serialNo: '',
  status: '使用中',
  purchaseDate: '',
  enabledDate: '',
  retiredDate: '',
  coverImageUrl: '',
  tagIds: [] as number[],
  notes: '',
  purchases: [] as Array<{
    type: 'PRIMARY' | 'ACCESSORY' | 'SERVICE'
    platformId?: number
    seller?: string
    price: number
    shippingCost: number
    currency: string
    quantity: number
    purchaseDate: string
    invoiceNo?: string
    warrantyMonths?: number
    warrantyExpireDate?: string
    notes?: string
    attachments: string[]
  }>
})

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择类别', trigger: 'change' }],
  enabledDate: [{ required: true, message: '请选择启用日期', trigger: 'change' }]
}

const { load: loadDicts, categoryTree, tagTree, platforms } = useDictionaries()

const treeProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  disabled: 'disabled'
}

const buildCategoryOptions = (nodes: CategoryNode[]): any[] =>
  nodes.map((node) => ({
    value: node.id,
    label: node.name,
    disabled: node.children?.length ? true : false,
    children: node.children ? buildCategoryOptions(node.children) : []
  }))

const buildTagOptions = (nodes: TagNode[]): any[] =>
  nodes.map((node) => ({
    value: node.id,
    label: node.name,
    children: node.children ? buildTagOptions(node.children) : []
  }))

const categoryOptions = computed(() => buildCategoryOptions(categoryTree.value))
const tagOptions = computed(() => buildTagOptions(tagTree.value))

const open = (asset?: AssetDetail) => {
  visible.value = true
  isEdit.value = !!asset
  if (asset) {
    form.id = asset.id
    form.name = asset.name
    form.categoryId = asset.categoryId ?? null
    form.brand = asset.brand || ''
    form.model = asset.model || ''
    form.serialNo = asset.serialNo || ''
    form.status = asset.status
    form.purchaseDate = asset.purchaseDate || ''
    form.enabledDate = asset.enabledDate
    form.retiredDate = (asset as AssetDetail).retiredDate || ''
    form.coverImageUrl = asset.coverImageUrl || ''
    form.tagIds = asset.tags ? asset.tags.map((tag) => tag.id) : []
    form.notes = (asset as AssetDetail).notes || ''
    form.purchases = asset.purchases
      ? asset.purchases.map((p) => ({
          type: p.type,
          platformId: p.platformId,
          seller: p.seller || '',
          price: p.price,
          shippingCost: p.shippingCost ?? 0,
          currency: p.currency || 'CNY',
          quantity: p.quantity || 1,
          purchaseDate: p.purchaseDate,
          invoiceNo: p.invoiceNo || '',
          warrantyMonths: p.warrantyMonths ?? undefined,
          warrantyExpireDate: p.warrantyExpireDate || '',
          notes: p.notes || '',
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
  form.categoryId = null
  form.brand = ''
  form.model = ''
  form.serialNo = ''
  form.status = '使用中'
  form.purchaseDate = ''
  form.enabledDate = ''
  form.retiredDate = ''
  form.coverImageUrl = ''
  form.tagIds = []
  form.notes = ''
  form.purchases = []
}

const addPurchase = () => {
  form.purchases.push({
    type: 'PRIMARY',
    platformId: undefined,
    seller: '',
    price: 0,
    shippingCost: 0,
    currency: 'CNY',
    quantity: 1,
    purchaseDate: '',
    invoiceNo: '',
    warrantyMonths: undefined,
    warrantyExpireDate: '',
    notes: '',
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

const removeAttachment = (purchase: any, url: string) => {
  purchase.attachments = purchase.attachments.filter((item: string) => item !== url)
}

const submit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return
    loading.value = true
    try {
      const payload = {
        name: form.name,
        categoryId: form.categoryId!,
        brand: form.brand,
        model: form.model,
        serialNo: form.serialNo,
        status: form.status,
        purchaseDate: form.purchaseDate || undefined,
        enabledDate: form.enabledDate,
        retiredDate: form.retiredDate || undefined,
        coverImageUrl: form.coverImageUrl || undefined,
        notes: form.notes || undefined,
        tagIds: form.tagIds,
        purchases: form.purchases.map((p) => ({
          type: p.type,
          platformId: p.platformId,
          seller: p.seller || undefined,
          price: p.price,
          shippingCost: p.shippingCost,
          currency: p.currency || 'CNY',
          quantity: p.quantity,
          purchaseDate: p.purchaseDate,
          invoiceNo: p.invoiceNo || undefined,
          warrantyMonths: p.warrantyMonths ?? undefined,
          warrantyExpireDate: p.warrantyExpireDate || undefined,
          notes: p.notes || undefined,
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

onMounted(async () => {
  await loadDicts()
})
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
  display: flex;
  flex-direction: column;
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
