<template>
  <div>
    <el-dialog
      v-model="visible"
      :title="isEdit ? '编辑物品' : '新建物品'"
      width="760px"
      @closed="reset"
      destroy-on-close
    >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" status-icon>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-form-item label="物品名称" prop="name">
            <el-input v-model="form.name" placeholder="请输入物品名称" />
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="物品类别" prop="categoryId" class="with-extra">
            <el-tree-select
              v-model="form.categoryId"
              :data="categoryOptions"
              :props="treeProps"
              check-strictly
              filterable
              placeholder="请选择类别（叶子节点）"
              style="width: 100%"
            />
            <el-button
              class="inline-action"
              text
              size="small"
              type="primary"
              @click="handleCreateCategory"
            >
              新建
            </el-button>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-form-item label="品牌">
            <el-select
              v-model="form.brand"
              filterable
              allow-create
              default-first-option
              placeholder="选择或输入品牌"
              @change="ensureBrandOption"
              style="width: 100%"
            >
              <el-option v-for="item in brandOptions" :key="item" :label="item" :value="item" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :xs="24" :md="12">
          <el-form-item label="型号">
            <el-input v-model="form.model" placeholder="型号/配置" />
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-form-item label="序列号">
            <el-input v-model="form.serialNo" placeholder="如 SN / IMEI" />
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
            <el-date-picker
              v-model="form.purchaseDate"
              type="date"
              value-format="YYYY-MM-DD"
              :shortcuts="dateShortcuts"
              placeholder="选择购买日期"
              @change="syncEnabledDate"
            />
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
            <el-button
              class="inline-action"
              text
              size="small"
              type="primary"
              @click="handleCreateTag"
            >
              新建
            </el-button>
          </el-form-item>
        </el-col>
      </el-row>
      <el-form-item label="封面图">
        <div class="cover-upload">
          <el-upload
            :http-request="handleUpload"
            :show-file-list="false"
            accept="image/*"
            capture="environment"
            @progress="coverProgress = $event.percent"
          >
            <el-button type="primary">上传封面</el-button>
          </el-upload>
          <el-progress
            v-if="coverProgress && coverProgress < 100"
            :percentage="Math.round(coverProgress)"
            :stroke-width="4"
            status="success"
          />
          <img v-if="coverImagePreview" :src="coverImagePreview" class="cover" />
        </div>
      </el-form-item>
      <el-form-item label="备注">
        <el-input v-model="form.notes" type="textarea" rows="3" placeholder="记录特殊说明" />
      </el-form-item>
      <div class="section-header">购买记录</div>
      <el-button type="primary" text @click="addPurchase">添加购买</el-button>
      <el-empty v-if="!form.purchases.length" description="尚未添加购买记录" />
      <el-collapse v-else accordion>
        <el-collapse-item
          v-for="(purchase, index) in form.purchases"
          :key="index"
          :title="`记录 ${index + 1} · ${purchase.type}`"
        >
          <el-row :gutter="12">
            <el-col :xs="24" :md="6">
              <el-select v-model="purchase.type" placeholder="类型" @change="handlePurchaseTypeChange(purchase)">
                <el-option label="主商品" value="PRIMARY" />
                <el-option label="配件" value="ACCESSORY" />
                <el-option label="服务" value="SERVICE" />
              </el-select>
            </el-col>
            <el-col :xs="24" :md="6">
              <el-select
                v-model="purchase.platformId"
                placeholder="来源平台"
                filterable
                clearable
                style="width: 100%"
              >
                <el-option v-for="item in platforms" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
              <el-button
                class="inline-action"
                text
                size="small"
                type="primary"
                @click="handleCreatePlatform"
              >
                新建平台
              </el-button>
            </el-col>
            <el-col :xs="24" :md="6">
              <el-input-number v-model="purchase.price" :min="0" :precision="2" :step="100" />
            </el-col>
            <el-col :xs="24" :md="6">
              <el-input-number v-model="purchase.shippingCost" :min="0" :precision="2" :step="10" placeholder="运费" />
            </el-col>
          </el-row>
          <el-row :gutter="12" class="mt">
            <el-col :xs="24" :md="6">
              <el-input v-model="purchase.currency" placeholder="币种（如 CNY）" />
            </el-col>
            <el-col :xs="24" :md="6">
              <el-input-number v-model="purchase.quantity" :min="1" :step="1" />
            </el-col>
            <el-col :xs="24" :md="6">
              <el-input v-model="purchase.seller" placeholder="卖家/店铺" />
            </el-col>
            <el-col :xs="24" :md="6" v-if="purchase.type !== 'PRIMARY'">
              <el-input v-model="purchase.name" placeholder="配件/服务名称" />
            </el-col>
          </el-row>
          <el-row :gutter="12" class="mt">
            <el-col :xs="24" :md="6">
              <el-date-picker
                v-model="purchase.purchaseDate"
                type="date"
                value-format="YYYY-MM-DD"
                :shortcuts="dateShortcuts"
                placeholder="购买日期"
                clearable
              />
            </el-col>
            <el-col :xs="24" :md="6">
              <el-input v-model="purchase.invoiceNo" placeholder="发票编号" />
            </el-col>
            <el-col :xs="24" :md="6">
              <el-input-number
                v-model="purchase.warrantyMonths"
                :min="0"
                :step="1"
                placeholder="质保（月）"
                controls-position="right"
              />
            </el-col>
            <el-col :xs="24" :md="6">
              <el-date-picker
                v-model="purchase.warrantyExpireDate"
                type="date"
                value-format="YYYY-MM-DD"
                :shortcuts="dateShortcuts"
                placeholder="质保到期日"
                clearable
              />
            </el-col>
          </el-row>
          <el-row :gutter="12" class="mt">
            <el-col :xs="24">
              <el-input v-model="purchase.notes" placeholder="备注" />
            </el-col>
          </el-row>
          <div class="attachments">
            <el-upload
              :http-request="(options) => uploadAttachment(options, purchase)"
              :show-file-list="false"
              accept="image/*"
              capture="environment"
            >
              <el-button text>上传凭证</el-button>
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
          </div>
          <div class="purchase-actions">
            <el-button type="danger" text @click="removePurchase(index)">删除记录</el-button>
          </div>
        </el-collapse-item>
      </el-collapse>
    </el-form>
    <template #footer>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="loading" @click="submit">保存</el-button>
    </template>
    </el-dialog>
    <CategoryCreateDialog
      v-model="categoryDialogVisible"
      :default-parent-id="form.categoryId"
      @success="handleCategoryDialogSuccess"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, UploadRequestOptions } from 'element-plus'
import { uploadFile } from '@/api/file'
import { createAsset, updateAsset } from '@/api/asset'
import type { AssetDetail } from '@/types'
import { useDictionaries } from '@/composables/useDictionaries'
import type { CategoryNode, TagNode } from '@/api/dict'
import { createCategory, createPlatform, createTag } from '@/api/dict'
import { buildOssUrl, extractObjectKey, extractObjectKeys } from '@/utils/storage'

const statuses = ['使用中', '已闲置', '待出售', '已出售', '已丢弃']

const emit = defineEmits<{ (e: 'success'): void }>()

const visible = ref(false)
const loading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const coverProgress = ref(0)

const today = () => new Date().toISOString().slice(0, 10)

const dateShortcuts = [
  {
    text: '今天',
    value: () => new Date()
  },
  {
    text: '7 天前',
    value: () => {
      const date = new Date()
      date.setDate(date.getDate() - 7)
      return date
    }
  }
]

const form = reactive({
  id: 0,
  name: '',
  categoryId: null as number | null,
  brand: '',
  model: '',
  serialNo: '',
  status: '使用中',
  purchaseDate: today(),
  enabledDate: today(),
  coverImageKey: '',
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
    name?: string
    attachments: string[]
  }>
})

const coverImagePreview = computed(() => buildOssUrl(form.coverImageKey))

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择类别', trigger: 'change' }],
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  purchaseDate: [{ required: true, message: '请选择购买日期', trigger: 'change' }]
}

const brandOptions = ref<string[]>([])

const ensureBrandOption = (value: string) => {
  if (!value) return
  if (!brandOptions.value.includes(value)) {
    brandOptions.value.push(value)
  }
}

const { load: loadDicts, categoryTree, tagTree, platforms } = useDictionaries()
const categoryDialogVisible = ref(false)

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
    disabled: node.children?.length ? false : false,
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
    ensureBrandOption(form.brand)
    form.model = asset.model || ''
    form.serialNo = asset.serialNo || ''
    form.status = asset.status
    form.purchaseDate = asset.purchaseDate || today()
    form.enabledDate = form.purchaseDate
    form.coverImageKey = extractObjectKey(asset.coverImageUrl)
    form.tagIds = asset.tags ? asset.tags.map((tag) => tag.id) : []
    form.notes = asset.notes || ''
    form.purchases = asset.purchases
      ? asset.purchases.map((p) => ({
          type: p.type,
          platformId: p.platformId,
          seller: p.seller || '',
          price: p.price,
          shippingCost: p.shippingCost ?? 0,
          currency: p.currency || 'CNY',
          quantity: p.quantity || 1,
          purchaseDate: p.purchaseDate || today(),
          invoiceNo: p.invoiceNo || '',
          warrantyMonths: p.warrantyMonths ?? undefined,
          warrantyExpireDate: p.warrantyExpireDate || '',
          notes: p.notes || '',
          name: p.type === 'PRIMARY' ? undefined : p.name || '',
          attachments: extractObjectKeys(p.attachments || [])
        }))
      : []
  } else {
    reset()
    form.purchaseDate = today()
    form.enabledDate = today()
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
  form.purchaseDate = today()
  form.enabledDate = today()
  form.coverImageKey = ''
  form.tagIds = []
  form.notes = ''
  form.purchases = []
  coverProgress.value = 0
}

const syncEnabledDate = () => {
  form.enabledDate = form.purchaseDate || today()
}

const addPurchase = () => {
  form.purchases.push({
    type: form.purchases.length ? 'ACCESSORY' : 'PRIMARY',
    platformId: undefined,
    seller: '',
    price: 0,
    shippingCost: 0,
    currency: 'CNY',
    quantity: 1,
    purchaseDate: form.purchaseDate || today(),
    invoiceNo: '',
    warrantyMonths: undefined,
    warrantyExpireDate: '',
    notes: '',
    name: undefined,
    attachments: []
  })
}

const removePurchase = (index: number) => {
  form.purchases.splice(index, 1)
}

const handlePurchaseTypeChange = (purchase: any) => {
  if (purchase.type === 'PRIMARY') {
    purchase.name = undefined
  } else if (!purchase.name) {
    purchase.name = ''
  }
}

const handleUpload = async (options: UploadRequestOptions) => {
  try {
    const { objectKey } = await uploadFile(options.file)
    form.coverImageKey = objectKey
    coverProgress.value = 100
    ElMessage.success('上传成功')
    options.onSuccess(objectKey)
  } catch (err: any) {
    coverProgress.value = 0
    ElMessage.error(err.message || '上传失败')
    options.onError(err)
  }
}

const uploadAttachment = async (options: UploadRequestOptions, purchase: any) => {
  try {
    const { objectKey } = await uploadFile(options.file)
    purchase.attachments.push(objectKey)
    ElMessage.success('附件上传成功')
    options.onSuccess(objectKey)
  } catch (err: any) {
    options.onError(err)
    ElMessage.error('附件上传失败')
  }
}

const removeAttachment = (purchase: any, url: string) => {
  purchase.attachments = purchase.attachments.filter((item: string) => item !== url)
}

const handleCreateCategory = () => {
  categoryDialogVisible.value = true
}

const handleCategoryDialogSuccess = (payload: { id: number }) => {
  form.categoryId = payload.id
}

const handleCreatePlatform = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入平台名称', '新建平台', {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：闲鱼'
    })
    if (!value) return
    await createPlatform({ name: value })
    await loadDicts()
    ElMessage.success('平台已创建')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error('创建平台失败')
  }
}

const handleCreateTag = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入标签名称', '新建标签', {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：主力设备'
    })
    if (!value) return
    const id = await createTag({ name: value, parentId: null })
    await loadDicts()
    form.tagIds.push(id)
    ElMessage.success('标签已创建')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error('创建标签失败')
  }
}

const submit = () => {
  formRef.value?.validate(async (valid) => {
    if (!valid) return

    const invalidPurchase = form.purchases.find(
      (purchase) =>
        (purchase.type === 'ACCESSORY' || purchase.type === 'SERVICE') && !purchase.name?.trim()
    )
    if (invalidPurchase) {
      ElMessage.warning('配件/服务需要填写名称')
      return
    }

    loading.value = true
    try {
      const payload = {
        name: form.name,
        categoryId: form.categoryId!,
        brand: form.brand || undefined,
        model: form.model || undefined,
        serialNo: form.serialNo || undefined,
        status: form.status,
        purchaseDate: form.purchaseDate || undefined,
        enabledDate: form.purchaseDate || undefined,
        coverImageUrl: extractObjectKey(form.coverImageKey) || undefined,
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
          name: p.type === 'PRIMARY' ? undefined : p.name,
          attachments: extractObjectKeys(p.attachments)
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

.cover-upload {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.cover {
  width: 120px;
  height: 120px;
  border-radius: 12px;
  object-fit: cover;
  border: 1px solid rgba(56, 189, 248, 0.35);
}

.mt {
  margin-top: 12px;
}

.tag {
  margin-right: 6px;
  margin-top: 4px;
}

.attachments {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-wrap: wrap;
  margin-top: 12px;
}

.purchase-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.inline-action {
  margin-top: 4px;
  padding: 0 6px;
}

.with-extra {
  position: relative;
  padding-bottom: 12px;
}

.with-extra .inline-action {
  position: absolute;
  right: 0;
  bottom: -6px;
}

@media (max-width: 768px) {
  :deep(.el-dialog) {
    width: 94vw !important;
  }
}
</style>
