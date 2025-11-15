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
              />
            </el-col>
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

    <!-- 新建类别对话框：可在树上选择父节点，然后输入名称创建 -->
    <el-dialog v-model="categoryDialogVisible" title="新建类别" width="480px" :destroy-on-close="true">
      <div style="display:flex;gap:12px">
        <div style="flex:1; max-height:320px; overflow:auto">
          <div style="margin-bottom:8px;color:var(--el-text-color-secondary)">选择父节点（不选则创建为根节点）</div>
          <el-tree
            :data="categoryOptions"
            node-key="value"
            :props="{ children: 'children', label: 'label' }"
            :highlight-current="true"
            :default-expand-all="false"
            :expand-on-click-node="false"
            :check-strictly="true"
            @node-click="onCategoryNodeClick"
          />
        </div>
        <div style="width:260px">
          <el-form label-width="0">
            <el-form-item label="">
              <el-input v-model="newCategoryName" placeholder="新类别名称" />
            </el-form-item>
            <el-form-item label="">
              <el-button type="primary" @click="createCategoryConfirm">创建并回显</el-button>
              <el-button @click="categoryDialogVisible = false">取消</el-button>
            </el-form-item>
          </el-form>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import type { FormInstance, UploadRequestOptions } from 'element-plus'
import { uploadFile } from '@/api/file'
import { createAsset, updateAsset } from '@/api/asset'
import type { AssetDetail } from '@/types'
import type { AssetPayload } from '@/api/asset'
import { useDictionaries } from '@/composables/useDictionaries'
import type { CategoryNode, TagNode } from '@/api/dict'
import { createCategory, createPlatform, createTag, createBrand } from '@/api/dict'
import { buildOssUrl, extractObjectKey, extractObjectKeys } from '@/utils/storage'

const statuses = ['使用中', '已闲置', '待出售', '已出售', '已丢弃']

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
  status: [{ required: true, message: '请选择状态', trigger: 'change' }],
  purchaseDate: [{ required: true, message: '请选择购买日期', trigger: 'change' }]
}

const { load: loadDicts, refresh: refreshDicts, categoryTree, tagTree, platforms, brands, brandMap } = useDictionaries()

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
  }
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
    quantity: 1,
    purchaseDate: form.purchaseDate || today(),
    warrantyMonths: undefined,
    warrantyExpireDate: '',
    notes: '',
    name: form.purchases.length ? '' : undefined,
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
    const { objectKey, url } = await uploadFile(options.file)
    form.coverImageKey = url || buildOssUrl(objectKey) || objectKey
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
    purchase.attachments.push(url || buildOssUrl(objectKey) || objectKey)
    ElMessage.success('附件上传成功')
    options.onSuccess(objectKey)
  } catch (err: any) {
    options.onError(err)
    ElMessage.error('附件上传失败')
  }
}
}

const removeAttachment = (purchase: any, url: string) => {
  purchase.attachments = purchase.attachments.filter((item: string) => item !== url)
}

const findCategoryNode = (id: number | null, nodes: CategoryNode[] = categoryTree.value): CategoryNode | null => {
  if (id == null) return null
  const stack = [...nodes]
  while (stack.length) {
    const node = stack.pop()!
    if (node.id === id) {
      return node
    }
    if (node.children?.length) {
      stack.push(...node.children)
    }
  }
  return null
}

const getCategorySiblings = (parentId: number | null): CategoryNode[] => {
  if (parentId == null) {
    return categoryTree.value
  }
  const stack = [...categoryTree.value]
  while (stack.length) {
    const node = stack.pop()!
    if (node.id === parentId) {
      return node.children || []
    }
    if (node.children?.length) {
      stack.push(...node.children)
    }
  }
  return []
}

const nextCategorySort = (siblings: CategoryNode[]) =>
  siblings.reduce((max, item) => Math.max(max, item.sort ?? 0), 0) + 10

const handleCreateCategory = async () => {
  try {
    const { value } = await ElMessageBox.prompt('请输入类别名称', '新建类别', {
      confirmButtonText: '创建',
      cancelButtonText: '取消',
      inputPlaceholder: '例如：笔记本电脑'
    })
    const trimmed = value?.trim()
    if (!trimmed) return

    const currentNode = findCategoryNode(form.categoryId)
    const parentId = currentNode?.parentId ?? null
    const siblings = getCategorySiblings(parentId)
    const id = await createCategory({ name: trimmed, parentId, sort: nextCategorySort(siblings) })
    await refreshDicts()
    form.categoryId = id
    ElMessage.success('类别已创建')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error('创建类别失败')
  }
}

const handleCreatePlatform = async (purchase: any) => {
   try {
     const { value } = await ElMessageBox.prompt('请输入平台名称', '新建平台', {
       confirmButtonText: '创建',
       cancelButtonText: '取消',
       inputPlaceholder: '例如：闲鱼'
     })
     if (!value) return
     const id = await createPlatform({ name: value })
     await refreshDicts()
     purchase.platformId = id
     ElMessage.success('平台已创建')
   } catch (error) {
     if (error === 'cancel' || error === 'close') return
     ElMessage.error('创建平台失败')
   }
 }
 
// 新建品牌函数（保持不变）
 const handleCreateBrand = async () => {
   try {
     const { value } = await ElMessageBox.prompt('请输入品牌名称', '新建品牌', {
       confirmButtonText: '创建',
       cancelButtonText: '取消',
       inputPlaceholder: '例如：Apple'
     })
     const trimmed = value?.trim()
     if (!trimmed) return
     const id = await createBrand({ name: trimmed })
     await refreshDicts()
     form.brandId = id
     form.brandName = trimmed
     ElMessage.success('品牌已创建')
   } catch (error) {
     if (error === 'cancel' || error === 'close') return
     ElMessage.error('创建品牌失败')
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
    await refreshDicts()
    form.tagIds = Array.from(new Set([...form.tagIds, id]))
    ElMessage.success('标签已创建')
  } catch (error) {
    if (error === 'cancel' || error === 'close') return
    ElMessage.error('创建标签失败')
  }
}

// ------ 新增：可在树上选择父节点并创建任意层级节点 ------
const categoryDialogVisible = ref(false)
const newCategoryName = ref('')
const selectedCategoryNode = ref<number | null>(null)

const openCategoryDialog = () => {
  // 默认将当前选择的 categoryId 作为初始父节点
  selectedCategoryNode.value = form.categoryId ?? null
  newCategoryName.value = ''
  categoryDialogVisible.value = true
}

const onCategoryNodeClick = (node: any) => {
  // node.value 对应 buildCategoryOptions 的 value
  selectedCategoryNode.value = node?.value ?? null
}

const createCategoryConfirm = async () => {
  try {
    const name = newCategoryName.value?.trim()
    if (!name) {
      ElMessage.warning('请输入类别名称')
      return
    }
    // 计算 parentId 与排序位置
    const parentId = selectedCategoryNode.value ?? null
    const siblings = getCategorySiblings(parentId)
    const id = await createCategory({ name, parentId, sort: nextCategorySort(siblings) })
    await refreshDicts()
    form.categoryId = id
    categoryDialogVisible.value = false
    ElMessage.success('类别已创建并回显')
  } catch (err) {
    ElMessage.error('创建类别失败')
  }
}
// ------ 结束新增 ------

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
