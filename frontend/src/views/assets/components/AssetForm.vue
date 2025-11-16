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
              placeholder="请选择类别"
              style="width: 80%"
            />
            <el-button
              class="inline-action"
              text
              size="small"
              type="primary"
              @click="openCategoryDialog"
            >
              新建
            </el-button>
          </el-form-item>
        </el-col>
      </el-row>
      <el-row :gutter="16">
        <el-col :xs="24" :md="12">
          <el-form-item label="品牌">
            <div class="brand-field">
              <el-select
                v-model="form.brandId"
                filterable
                clearable
                placeholder="选择品牌"
                @change="handleBrandSelect"
              >
                <el-option
                  v-for="item in brandOptions"
                  :key="item.id"
                  :label="item.label"
                  :value="item.id"
                />
              </el-select>
              <el-button
                class="inline-action"
                text
                size="small"
                type="primary"
                @click="handleCreateBrand"
              >
                新建
              </el-button>
            </div>
            <!-- <p class="brand-hint">可选品牌后调整显示名称，留空表示不记录品牌。</p> -->
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
              placeholder="选择标签"
              style="width: 80%"
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
            <el-col :xs="24" :md="purchase.type !== 'PRIMARY' ? 6 : 8">
              <el-select v-model="purchase.type" placeholder="类型" @change="handlePurchaseTypeChange(purchase)">
                <el-option label="主商品" value="PRIMARY" />
                <el-option label="配件" value="ACCESSORY" />
                <el-option label="服务" value="SERVICE" />
              </el-select>
            </el-col>
            <el-col v-if="purchase.type !== 'PRIMARY'" :xs="24" :md="6">
              <el-input v-model="purchase.name" placeholder="配件/服务名称" />
            </el-col>
            <el-col :xs="24" :md="purchase.type !== 'PRIMARY' ? 6 : 8">
              <el-select
                v-model="purchase.platformId"
                placeholder="购买平台"
                filterable
                clearable
                style="width: 80%"
              >
                <el-option v-for="item in platforms" :key="item.id" :label="item.name" :value="item.id" />
              </el-select>
              <el-button
                class="inline-action"
                text
                size="small"
                type="primary"
                @click="handleCreatePlatform(purchase)"
              >
                新建
              </el-button>
            </el-col>
            <el-col :xs="24" :md="purchase.type !== 'PRIMARY' ? 6 : 8">
              <el-input-number v-model="purchase.price" :min="0" :precision="2" :step="100" />
            </el-col>
          </el-row>
          
          <el-row :gutter="12" class="mt">
            <el-col v-if="purchase.type !== 'PRIMARY'" :xs="24" :md="8">
              <el-input-number v-model="purchase.quantity" :min="1" :step="1" />
            </el-col>
            <el-col :xs="24" :md="8">
              <el-input v-model="purchase.seller" placeholder="卖家/店铺" />
            </el-col>
          </el-row>
          <el-row :gutter="12" class="mt">
            <el-col :xs="24" :md="8">
              <el-date-picker
                v-model="purchase.purchaseDate"
                type="date"
                value-format="YYYY-MM-DD"
                :shortcuts="dateShortcuts"
                placeholder="购买日期"
                clearable
                @change="() => syncPurchaseWarranty(purchase)"
              />
            </el-col>
            <el-col :xs="24" :md="8">
              <el-input-number
                v-model="purchase.warrantyMonths"
                :min="0"
                :step="1"
                placeholder="质保（月）"
                controls-position="right"
                @change="() => syncPurchaseWarranty(purchase)"
              />
            </el-col>
            <el-col :xs="24" :md="8">
              <el-date-picker
                v-model="purchase.warrantyExpireDate"
                type="date"
                value-format="YYYY-MM-DD"
                :shortcuts="dateShortcuts"
                placeholder="质保到期日"
                disabled
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
              <el-button text>上传附件</el-button>
            </el-upload>
            <div
              v-for="(url, idx) in purchase.attachments"
              :key="`${idx}-${url}`"
              class="attachment-thumb"
            >
              <el-image
                v-if="resolveOssUrl(url)"
                :src="resolveOssUrl(url)"
                fit="cover"
                class="attachment-image"
                :preview-src-list="[resolveOssUrl(url)]"
              />
              <div v-else class="attachment-fallback">附件{{ idx + 1 }}</div>
              <el-button
                text
                type="danger"
                size="small"
                class="attachment-remove"
                @click="removeAttachment(purchase, url)"
              >
                删除
              </el-button>
            </div>
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

    <category-create-dialog
      v-model="categoryDialogVisible"
      :default-parent-id="categoryDialogParentId"
      @success="handleCategoryCreated"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, UploadRequestOptions } from 'element-plus'
import { uploadFile } from '@/api/file'
import { createAsset, updateAsset } from '@/api/asset'
import type { AssetDetail } from '@/types'
import type { AssetPayload } from '@/api/asset'
import { useDictionaries } from '@/composables/useDictionaries'
import type { CategoryNode, TagNode } from '@/api/dict'
import CategoryCreateDialog from '@/components/CategoryCreateDialog.vue'
import { useDictionaryCreator } from '@/composables/useDictionaryCreator'
import { buildOssUrl, extractObjectKey, extractObjectKeys } from '@/utils/storage'
import { calcWarrantyExpireDate } from '@/utils/date'

const emit = defineEmits<{ (e: 'success', assetId?: number): void }>()

const visible = ref(false)
const loading = ref(false)
const isEdit = ref(false)
const formRef = ref<FormInstance>()
const coverProgress = ref(0)
const customSubmit = ref<((payload: AssetPayload) => Promise<any>) | null>(null)
const customSuccessMessage = ref<string | null>(null)
const customAfterSuccess = ref<((result: any) => void) | null>(null)
//const categoryDialogVisible = ref(false)

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
  brandId: null as number | null,
  brandName: '',
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
    quantity: number
    purchaseDate: string
    warrantyMonths?: number
    warrantyExpireDate?: string
    notes?: string
    name?: string
    attachments: string[]
  }>
})

type AssetFormState = typeof form

type AssetFormPrefill = Partial<
  Pick<
    AssetFormState,
    | 'name'
    | 'categoryId'
    | 'brandId'
    | 'brandName'
    | 'model'
    | 'serialNo'
    | 'status'
    | 'purchaseDate'
    | 'enabledDate'
    | 'coverImageKey'
    | 'tagIds'
    | 'notes'
    | 'purchases'
  >
>

type AssetFormOpenOptions = {
  prefill?: AssetFormPrefill
  submit?: (payload: AssetPayload) => Promise<any>
  successMessage?: string
  onSuccess?: (result: any) => void
}

const coverImagePreview = computed(() => buildOssUrl(form.coverImageKey))
const resolveOssUrl = (value?: string | null) => buildOssUrl(value)

const rules = {
  name: [{ required: true, message: '请输入名称', trigger: 'blur' }],
  categoryId: [{ required: true, message: '请选择类别', trigger: 'change' }],
  purchaseDate: [{ required: true, message: '请选择购买日期', trigger: 'change' }]
}

const { load: loadDicts, categoryTree, tagTree, platforms, brands, brandMap } = useDictionaries()
const { promptPlatformCreation, promptBrandCreation, promptTagCreation } = useDictionaryCreator()

const brandOptions = computed(() =>
  brands.value.map((item) => ({
    id: item.id,
    label: (item.alias && item.alias.trim()) || item.name
  }))
)

const normalizeBrandName = () => {
  form.brandName = form.brandName.trim()
  if (form.brandId && !form.brandName) {
    const brand = brandMap.value.get(form.brandId)
    if (brand) {
      form.brandName = (brand.alias?.trim() || brand.name || '').trim()
    }
  }
}

const handleBrandSelect = (id: number | null) => {
  if (!id) {
    form.brandId = null
    normalizeBrandName()
    return
  }
  const brand = brandMap.value.get(id)
  if (brand) {
    form.brandName = (brand.alias?.trim() || brand.name || '').trim()
  }
}
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

const open = (asset?: AssetDetail | null, options?: AssetFormOpenOptions) => {
  customSubmit.value = options?.submit ?? null
  customSuccessMessage.value = options?.successMessage ?? null
  customAfterSuccess.value = options?.onSuccess ?? null
  visible.value = true
  isEdit.value = !!asset
  if (asset) {
    form.id = asset.id
    form.name = asset.name
    form.categoryId = asset.categoryId ?? null
    form.brandId = asset.brand?.id ?? null
    form.brandName =
      (asset.brand?.alias && asset.brand.alias.trim()) ||
      (asset.brand?.name && asset.brand.name.trim()) ||
      ''
    normalizeBrandName()
    form.model = asset.model || ''
    form.serialNo = asset.serialNo || ''
    form.status = asset.status
    form.purchaseDate = asset.purchaseDate || today()
    form.enabledDate = form.purchaseDate
    form.coverImageKey = asset.coverImageUrl || ''
    form.tagIds = asset.tags ? asset.tags.map((tag) => tag.id) : []
    form.notes = asset.notes || ''
    form.purchases = asset.purchases
      ? asset.purchases.map((p) => ({
          type: p.type,
          platformId: p.platformId,
          seller: p.seller || '',
          price: p.price,
          shippingCost: p.shippingCost ?? 0,
          quantity: p.quantity || 1,
          purchaseDate: p.purchaseDate || today(),
          warrantyMonths: p.warrantyMonths ?? undefined,
          warrantyExpireDate: p.warrantyExpireDate || '',
          notes: p.notes || '',
          name: p.type === 'PRIMARY' ? undefined : p.name || '',
          attachments: Array.isArray(p.attachments)
            ? p.attachments.filter((item): item is string => !!item)
            : []
        }))
      : []
    form.purchases.forEach((purchase) => syncPurchaseWarranty(purchase))
  } else {
    reset()
    form.purchaseDate = today()
    form.enabledDate = today()
    if (options?.prefill) {
      applyPrefill(options.prefill)
    }
  }
}

const reset = () => {
  form.id = 0
  form.name = ''
  form.categoryId = null
  form.brandId = null
  form.brandName = ''
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

const applyPrefill = (prefill: AssetFormPrefill) => {
  if (prefill.name !== undefined) form.name = prefill.name
  if (prefill.categoryId !== undefined) form.categoryId = prefill.categoryId
  if (prefill.brandId !== undefined) form.brandId = prefill.brandId
  if (prefill.brandName !== undefined) form.brandName = prefill.brandName
  if (prefill.model !== undefined) form.model = prefill.model
  if (prefill.serialNo !== undefined) form.serialNo = prefill.serialNo
  if (prefill.status !== undefined) form.status = prefill.status
  if (prefill.purchaseDate !== undefined) form.purchaseDate = prefill.purchaseDate
  if (prefill.enabledDate !== undefined) form.enabledDate = prefill.enabledDate
  if (prefill.coverImageKey !== undefined) form.coverImageKey = prefill.coverImageKey
  if (prefill.tagIds !== undefined) form.tagIds = Array.isArray(prefill.tagIds) ? [...prefill.tagIds] : []
  if (prefill.notes !== undefined) form.notes = prefill.notes
  if (prefill.purchases !== undefined) {
    form.purchases = prefill.purchases.map((purchase) => ({
      ...purchase,
      attachments: Array.isArray(purchase.attachments) ? [...purchase.attachments] : []
    })) as AssetFormState['purchases']
    form.purchases.forEach((purchase) => syncPurchaseWarranty(purchase))
  }
}

const syncEnabledDate = () => {
  form.enabledDate = form.purchaseDate || today()
}

const syncPurchaseWarranty = (purchase: AssetFormState['purchases'][number]) => {
  if (!purchase) return
  purchase.warrantyExpireDate = calcWarrantyExpireDate(purchase.purchaseDate, purchase.warrantyMonths)
}

const addPurchase = () => {
  form.purchases.push({
    type: form.purchases.length ? 'ACCESSORY' : 'PRIMARY',
    platformId: undefined,
    seller: '',
    price: 0,
    shippingCost: 0,
    quantity: 1,
    purchaseDate: form.purchaseDate || today(),
    warrantyMonths: undefined,
    warrantyExpireDate: '',
    notes: '',
    name: form.purchases.length ? '' : undefined,
    attachments: []
  })
  const last = form.purchases[form.purchases.length - 1]
  syncPurchaseWarranty(last)
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
    const { objectKey, url } = await uploadFile(options.file)
    const stored = objectKey || extractObjectKey(url) || url
    form.coverImageKey = stored
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
    const { objectKey, url } = await uploadFile(options.file)
    const stored = objectKey || extractObjectKey(url) || url
    purchase.attachments.push(stored)
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

const handleCreatePlatform = async (purchase: any) => {
  try {
    const result = await promptPlatformCreation()
    if (result) {
      purchase.platformId = result.id
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '创建平台失败')
  }
}

const handleCreateBrand = async () => {
  try {
    const result = await promptBrandCreation()
    if (result) {
      form.brandId = result.id
      form.brandName = result.name
      normalizeBrandName()
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '创建品牌失败')
  }
}

const handleCreateTag = async () => {
  try {
    const result = await promptTagCreation()
    if (result) {
      form.tagIds = Array.from(new Set([...form.tagIds, result.id]))
    }
  } catch (error: any) {
    ElMessage.error(error?.message || '创建标签失败')
  }
}

const categoryDialogVisible = ref(false)
const categoryDialogParentId = ref<number | null>(null)

const openCategoryDialog = () => {
  categoryDialogParentId.value = form.categoryId ?? null
  categoryDialogVisible.value = true
}

const handleCategoryCreated = (payload: { id: number }) => {
  form.categoryId = payload.id
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
      normalizeBrandName()
      const brandText = form.brandName.trim()
      const payload = {
        name: form.name,
        categoryId: form.categoryId!,
        brandId: form.brandId || undefined,
        brand: brandText || undefined,
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
          quantity: p.quantity,
          purchaseDate: p.purchaseDate,
          warrantyMonths: p.warrantyMonths ?? undefined,
          warrantyExpireDate: p.warrantyExpireDate || undefined,
          notes: p.notes || undefined,
          name: p.type === 'PRIMARY' ? undefined : p.name,
          attachments: extractObjectKeys(p.attachments)
        }))
      }
      let result: any
      if (customSubmit.value) {
        result = await customSubmit.value(payload)
        if (customSuccessMessage.value) {
          ElMessage.success(customSuccessMessage.value)
        } else {
          ElMessage.success('操作成功')
        }
      } else if (isEdit.value) {
        await updateAsset(form.id, payload)
        ElMessage.success('更新成功')
        result = form.id
      } else {
        const assetId = await createAsset(payload)
        ElMessage.success('创建成功')
        result = assetId
      }
      visible.value = false
      emit('success', result)
      customAfterSuccess.value?.(result)
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

.attachment-thumb {
  position: relative;
  width: 80px;
  height: 80px;
}

.attachment-image,
.attachment-fallback {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  border: 1px solid rgba(56, 189, 248, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

.attachment-remove {
  position: absolute;
  top: -8px;
  right: -6px;
  padding: 0;
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

.brand-field {
  display: flex;
  gap: 8px;
  align-items: center;
}

.brand-field :deep(.el-select) {
  width: 180px;
}

.brand-field :deep(.el-input) {
  flex: 1;
  min-width: 0;
}

.brand-hint {
  margin: 4px 0 0;
  font-size: 12px;
  color: var(--el-text-color-secondary);
}

@media (max-width: 768px) {
  .brand-field {
    flex-direction: column;
    align-items: stretch;
  }

  .brand-field :deep(.el-select) {
    width: 100%;
  }
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
