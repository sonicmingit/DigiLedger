<template>
  <div>
    <el-dialog
      v-model="visible"
      :title="null"
      width="820px"
      class="asset-dialog"
      @closed="reset"
      destroy-on-close
    >
      <div class="dialog-hero">
        <div>
          <h3>{{ isEdit ? '编辑物品' : '新建物品' }}</h3>
          <p>上传封面、补充品牌与购买记录，系统会帮你算出日均成本。</p>
        </div>
        <el-tag effect="plain" round type="success">物品卡片</el-tag>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-width="auto"
        label-position="top"
        status-icon
        class="asset-form"
      >
        <div class="form-grid">
          <section class="media-panel">
            <div class="panel-title">封面</div>
            <el-form-item label="封面图" class="cover-item">
              <div class="cover-upload">
                <div class="upload-drop">
                  <UnifiedUploader
                    :http-request="handleUpload"
                    :show-file-list="false"
                    accept="image/*"
                    capture="environment"
                    @progress="coverProgress = $event.percent"
                  />
                  <div class="upload-hint">拖拽/点击上传，推荐 3:2 比例</div>
                </div>
                <el-progress
                  v-if="coverProgress && coverProgress < 100"
                  :percentage="Math.round(coverProgress)"
                  :stroke-width="4"
                  status="success"
                />
                <img v-if="coverImagePreview" :src="coverImagePreview" class="cover" />
                <div class="cover-actions">
                  <el-button
                    type="primary"
                    link
                    size="small"
                    :disabled="!form.id"
                    @click="openCoverSuggestionDialog"
                  >
                    智能找图设封面
                  </el-button>
                </div>
              </div>
            </el-form-item>
          </section>

          <section class="fields-panel">
            <div class="panel-title">基础信息</div>
            <el-row :gutter="16">
              <el-col :xs="24" :md="12">
                <el-form-item label="物品名称" prop="name">
                  <el-input v-model="form.name" placeholder="请输入物品名称" />
                </el-form-item>
              </el-col>
              <el-col :xs="24" :md="12">
                <el-form-item label="物品类别" prop="categoryId">
                  <div class="field-inline">
                    <el-cascader
                      v-model="form.categoryId"
                      :options="categoryOptions"
                      :props="cascaderProps"
                      filterable
                      clearable
                      placeholder="请选择类别"
                      class="field-grow"
                    />
                    <el-tooltip content="新建类别" placement="top">
                      <el-button
                        class="label-action"
                        circle
                        text
                        type="success"
                        :icon="CirclePlus"
                        @click="openCategoryDialog"
                      />
                    </el-tooltip>
                  </div>
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
                    <el-tooltip content="新建品牌" placement="top">
                      <el-button
                        class="label-action"
                        circle
                        text
                        type="success"
                        :icon="CirclePlus"
                        @click="handleCreateBrand"
                      />
                    </el-tooltip>
                  </div>
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
                <el-form-item label="购买日期" prop="purchaseDate">
                  <el-date-picker
                    v-model="form.purchaseDate"
                    type="date"
                    value-format="YYYY-MM-DD"
                    :shortcuts="dateShortcuts"
                    placeholder="选择购买日期"
                  />
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :xs="24" :md="12">
                <el-form-item label="标签">
                  <div class="field-inline">
                    <el-tree-select
                      v-model="form.tagIds"
                      :data="tagOptions"
                      :props="treeProps"
                      multiple
                      show-checkbox
                      filterable
                      placeholder="选择标签"
                      class="field-grow"
                    />
                    <el-tooltip content="新建标签" placement="top">
                      <el-button
                        class="label-action"
                        circle
                        text
                        type="success"
                        :icon="CirclePlus"
                        @click="handleCreateTag"
                      />
                    </el-tooltip>
                  </div>
                </el-form-item>
              </el-col>
            </el-row>
            <el-row :gutter="16">
              <el-col :xs="24">
                <el-form-item label="备注" class="note-item">
                  <el-input
                    v-model="form.notes"
                    type="textarea"
                    :rows="5"
                    placeholder="记录特殊说明、保养节点或使用心得"
                  />
                </el-form-item>
              </el-col>
            </el-row>
          </section>
        </div>

        <el-card class="purchase-card" shadow="never">
          <template #header>
            <div class="purchase-header">
              <div>
                <div class="panel-title">购买记录</div>
                <p class="panel-subtitle">拆分主商品、配件、服务，方便核算投入</p>
              </div>
              <el-button type="primary" :icon="Plus" @click="addPurchase">添加购买</el-button>
            </div>
          </template>
          <el-empty v-if="!form.purchases.length" description="尚未添加购买记录" />
          <el-collapse v-else v-model="activePurchasePanel" accordion>
            <el-collapse-item
              v-for="(purchase, index) in form.purchases"
              :key="index"
              :name="String(index)"
            >
              <template #title>
                <div class="purchase-title">
                  <div class="purchase-title-left">
                    <el-tag
                      size="small"
                      effect="light"
                      round
                      :type="purchaseTypeTagType(purchase.type)"
                      class="purchase-type-tag"
                    >
                      {{ purchaseTypeLabel(purchase.type) }}
                    </el-tag>
                    <span class="purchase-title-main">
                      记录 {{ index + 1 }}
                      <span v-if="purchase.type !== 'PRIMARY' && purchase.name">· {{ purchase.name }}</span>
                    </span>
                  </div>
                  <div class="purchase-title-actions">
                    <span class="purchase-title-meta">{{ formatMoney(purchase.price) }}</span>
                    <span v-if="platformNameById(purchase.platformId)" class="purchase-title-meta">
                      {{ platformNameById(purchase.platformId) }}
                    </span>
                    <span v-if="purchase.purchaseDate" class="purchase-title-meta">{{ purchase.purchaseDate }}</span>
                    <el-button type="danger" text @click.stop="removePurchase(index)">删除记录</el-button>
                  </div>
                </div>
              </template>

              <el-row :gutter="12">
                <el-col :xs="24" :md="purchase.type !== 'PRIMARY' ? 6 : 8">
                  <el-form-item label="类型">
                    <el-select v-model="purchase.type" placeholder="类型" @change="handlePurchaseTypeChange(purchase)">
                      <el-option label="主商品" value="PRIMARY" />
                      <el-option label="配件" value="ACCESSORY" />
                      <el-option label="服务" value="SERVICE" />
                    </el-select>
                  </el-form-item>
                </el-col>
                <el-col v-if="purchase.type !== 'PRIMARY'" :xs="24" :md="6">
                  <el-form-item label="名称">
                    <el-input v-model="purchase.name" placeholder="配件/服务名称" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="purchase.type !== 'PRIMARY' ? 6 : 8">
                  <el-form-item label="购买平台">
                    <div class="field-inline">
                      <el-select
                        v-model="purchase.platformId"
                        placeholder="购买平台"
                        filterable
                        clearable
                        class="field-grow"
                      >
                        <el-option v-for="item in platforms" :key="item.id" :label="item.name" :value="item.id" />
                      </el-select>
                      <el-tooltip content="新建平台" placement="top">
                        <el-button
                          class="inline-action"
                          circle
                          text
                          type="success"
                          :icon="CirclePlus"
                          @click="handleCreatePlatform(purchase)"
                        />
                      </el-tooltip>
                    </div>
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="purchase.type !== 'PRIMARY' ? 6 : 8">
                  <el-form-item label="金额">
                    <el-input-number v-model="purchase.price" :min="0" :precision="2" :step="100" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="12" class="mt">
                <el-col v-if="purchase.type !== 'PRIMARY'" :xs="24" :md="8">
                  <el-form-item label="数量">
                    <el-input-number v-model="purchase.quantity" :min="1" :step="1" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="卖家/店铺">
                    <el-input v-model="purchase.seller" placeholder="卖家/店铺" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="购买链接">
                    <el-input v-model="purchase.productLink" placeholder="购买链接" />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="12" class="mt">
                <el-col :xs="24" :md="8">
                  <el-form-item label="购买日期">
                    <el-date-picker
                      v-model="purchase.purchaseDate"
                      type="date"
                      value-format="YYYY-MM-DD"
                      :shortcuts="dateShortcuts"
                      placeholder="购买日期"
                      clearable
                      @change="() => syncPurchaseWarranty(purchase)"
                    />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="质保（月）">
                    <el-input-number
                      v-model="purchase.warrantyMonths"
                      :min="0"
                      :step="1"
                      placeholder="质保（月）"
                      controls-position="right"
                      @change="() => syncPurchaseWarranty(purchase)"
                    />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="8">
                  <el-form-item label="质保到期日">
                    <el-date-picker
                      v-model="purchase.warrantyExpireDate"
                      type="date"
                      value-format="YYYY-MM-DD"
                      :shortcuts="dateShortcuts"
                      placeholder="质保到期日"
                      disabled
                    />
                  </el-form-item>
                </el-col>
              </el-row>

              <el-row :gutter="12" class="mt">
                <el-col :xs="24" :md="12" class="purchase-notes-col">
                  <el-form-item label="备注">
                    <el-input v-model="purchase.notes" placeholder="备注" type="textarea" :rows="6" />
                  </el-form-item>
                </el-col>
                <el-col :xs="24" :md="12" class="purchase-attachments-col">
                  <el-form-item label="附件">
                    <div
                      class="attachments-grid"
                      :class="{ 'attachments-grid--empty': !purchase.attachments?.length }"
                    >
                      <div class="attachment-add" :class="{ 'attachment-add--full': !purchase.attachments?.length }">
                        <UnifiedUploader
                          :http-request="(options) => uploadAttachment(options, purchase)"
                          :show-file-list="false"
                          accept="image/*"
                          capture="environment"
                        />
                        <div v-if="purchase.attachments?.length" class="attachment-add-label">添加</div>
                      </div>
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
                  </el-form-item>
                </el-col>
              </el-row>
            </el-collapse-item>
          </el-collapse>
        </el-card>
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
    <cover-suggestion-dialog
      v-model="suggestionDialogVisible"
      :asset-id="form.id || undefined"
      :query="coverSuggestionQuery"
      @select="handleCoverSuggestionSelected"
    />
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref } from 'vue'
import UnifiedUploader from '@/components/UnifiedUploader.vue'
import { ElMessage } from 'element-plus'
import type { FormInstance, UploadRequestOptions } from 'element-plus'
import { CirclePlus, Plus } from '@element-plus/icons-vue'
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
import { setCoverFromUrl } from '@/api/asset'
import CoverSuggestionDialog from '@/components/CoverSuggestionDialog.vue'
import type { CoverSuggestion } from '@/types'

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
const activePurchasePanel = ref<string>('')

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
    productLink?: string
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

const suggestionDialogVisible = ref(false)

const coverSuggestionQuery = computed(() => {
  const parts = [
    form.name?.trim(),
    form.brandName?.trim(),
    resolveCategoryName(form.categoryId)
  ].filter((value): value is string => !!value)
  return parts.join(' ') || '物品封面'
})

const openCoverSuggestionDialog = () => {
  if (!form.id) {
    ElMessage.warning('请先保存物品后再使用智能找图')
    return
  }
  suggestionDialogVisible.value = true
}

const handleCoverSuggestionSelected = async (suggestion: CoverSuggestion) => {
  if (!form.id) return
  try {
    const result = await setCoverFromUrl(form.id, { sourceUrl: suggestion.sourceUrl })
    form.coverImageKey = extractObjectKey(result.url)
    coverProgress.value = 100
    ElMessage.success('封面设置成功')
  } catch (error: any) {
    ElMessage.error(error?.message || '设置封面失败')
  }
}

const resolveCategoryName = (categoryId: number | null) => {
  if (categoryId == null) return null
  let name: string | null = null
  const stack: CategoryNode[] = [...categoryTree.value]
  while (stack.length) {
    const node = stack.pop()
    if (!node) continue
    if (node.id === categoryId) {
      name = node.name
      break
    }
    if (node.children?.length) {
      stack.push(...node.children)
    }
  }
  return name
}


const { load: loadDicts, categoryTree, tagTree, platforms, brands, brandMap } = useDictionaries()
const { promptPlatformCreation, promptBrandCreation, promptTagCreation } = useDictionaryCreator()

const platformNameMap = computed(() => {
  const map = new Map<number, string>()
  platforms.value.forEach((platform) => map.set(platform.id, platform.name))
  return map
})

const platformNameById = (platformId?: number) => {
  if (!platformId) return ''
  return platformNameMap.value.get(platformId) || ''
}

const purchaseTypeLabel = (type: AssetFormState['purchases'][number]['type']) => {
  if (type === 'PRIMARY') return '主商品'
  if (type === 'ACCESSORY') return '配件'
  return '服务'
}

const purchaseTypeTagType = (type: AssetFormState['purchases'][number]['type']) => {
  if (type === 'PRIMARY') return 'success'
  if (type === 'ACCESSORY') return 'warning'
  return 'info'
}

const moneyFormatter = new Intl.NumberFormat('zh-CN', {
  style: 'currency',
  currency: 'CNY',
  minimumFractionDigits: 0,
  maximumFractionDigits: 2
})

const formatMoney = (value: unknown) => {
  const numberValue = typeof value === 'number' ? value : Number(value)
  if (!Number.isFinite(numberValue)) return '¥0'
  return moneyFormatter.format(numberValue)
}

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

// cascader 使用以支持仅返回最终节点的值（categoryId），不返回完整路径数组
const cascaderProps = {
  value: 'value',
  label: 'label',
  children: 'children',
  emitPath: false
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
          productLink: p.productLink || '',
          name: p.type === 'PRIMARY' ? undefined : p.name || '',
          attachments: Array.isArray(p.attachments)
            ? p.attachments.filter((item): item is string => !!item)
            : []
        }))
      : []
    form.purchases.forEach((purchase) => syncPurchaseWarranty(purchase))
    setDefaultPurchasePanel()
  } else {
    reset()
    form.purchaseDate = today()
    if (options?.prefill) {
      applyPrefill(options.prefill)
    }
    if (!form.purchases.length) {
      addPurchase()
    } else {
      setDefaultPurchasePanel()
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
  form.coverImageKey = ''
  form.tagIds = []
  form.notes = ''
  form.purchases = []
  coverProgress.value = 0
  activePurchasePanel.value = ''
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

const setDefaultPurchasePanel = () => {
  if (!form.purchases.length) {
    activePurchasePanel.value = ''
    return
  }
  const primaryIndex = form.purchases.findIndex((purchase) => purchase.type === 'PRIMARY')
  activePurchasePanel.value = String(primaryIndex >= 0 ? primaryIndex : 0)
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
  activePurchasePanel.value = String(form.purchases.length - 1)
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
    const stored = url || objectKey || extractObjectKey(url)
    if (stored) {
      form.coverImageKey = stored
    }
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
    const stored = url || objectKey || extractObjectKey(url)
    if (stored) {
      purchase.attachments.push(stored)
    }
    ElMessage.success('附件上传成功')
    options.onSuccess(objectKey)
  } catch (err: any) {
    options.onError(err)
    ElMessage.error('附件上传失败')
  }
}

/**
 * 直接上传单个 file 对象（用于 JS 触发的“换一个”按钮）
 */
const uploadAttachmentRaw = async (file: File, purchase: any) => {
  try {
    const { objectKey, url } = await uploadFile(file)
    const stored = url || objectKey || extractObjectKey(url)
    if (stored) {
      purchase.attachments.push(stored)
    }
    ElMessage.success('附件上传成功')
  } catch (err: any) {
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
          productLink: p.productLink || undefined,
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
.asset-dialog :deep(.el-dialog__body) {
  padding-top: 4px;
}

.asset-dialog :deep(.el-dialog__footer) {
  padding-top: 10px;
}

.asset-dialog :deep(.el-input__wrapper),
.asset-dialog :deep(.el-select .el-input__wrapper),
.asset-dialog :deep(.el-cascader .el-input__wrapper),
.asset-dialog :deep(.el-date-editor .el-input__wrapper),
.asset-dialog :deep(.el-input-number .el-input__wrapper),
.asset-dialog :deep(.el-select__wrapper) {
  background-color: var(--el-fill-color-blank);
}

.dialog-hero {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 18px;
  background: linear-gradient(135deg, var(--dl-card), var(--el-color-primary-light-9));
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--dl-radius-lg);
  margin-bottom: 16px;
}

.dialog-hero h3 {
  margin: 0 0 6px;
  font-size: 20px;
  color: var(--el-color-primary-dark-2);
}

.dialog-hero p {
  margin: 0;
  color: var(--dl-muted);
  font-size: 13px;
}

.asset-form {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.field-inline {
  display: flex;
  gap: 8px;
  align-items: center;
  min-width: 0;
}

.field-grow {
  flex: 1;
  min-width: 0;
  width: auto;
}

.form-grid {
  display: grid;
  grid-template-columns: 320px 1fr;
  gap: 16px;
  align-items: stretch;
}

.media-panel,
.fields-panel {
  background: var(--dl-card);
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--dl-radius-lg);
  padding: 16px;
  box-shadow: var(--dl-shadow-md);
  display: flex;
  flex-direction: column;
}

/* 使基础信息面板内的列顶部对齐，label 与控件间距一致 */
.fields-panel :deep(.el-row) {
  align-items: flex-start;
}
.fields-panel :deep(.el-col) {
  display: flex;
  flex-direction: column;
}
.fields-panel :deep(.el-form-item) {
  margin-bottom: 12px;
}
.fields-panel :deep(.el-form-item__label) {
  font-weight: 600;
  margin-bottom: 8px;
  line-height: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}
.fields-panel :deep(.el-form-item__content) {
  min-height: 36px; /* 保证控件高度一致，避免因行高影响对齐 */
}
.fields-panel :deep(.label-with-action) {
  align-items: center;
}

.panel-title {
  font-weight: 700;
  color: var(--el-color-primary-dark-2);
  margin-bottom: 10px;
}

.panel-subtitle {
  margin: 4px 0 0;
  color: var(--dl-muted);
  font-size: 12px;
}

.cover-upload {
  display: flex;
  flex-direction: column;
  gap: 10px;
  flex: 1;
}

.cover-item {
  flex: 1;
}

.cover-item :deep(.el-form-item__content) {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.cover-actions {
  display: flex;
  justify-content: flex-end;
}

.upload-drop {
  border: 1px dashed color-mix(in srgb, var(--dl-accent) 38%, transparent);
  border-radius: 14px;
  padding: 18px;
  background: var(--dl-bg-alt);
  text-align: center;
  color: var(--el-color-primary-dark-2);
}

.upload-hint {
  margin-top: 8px;
  color: var(--dl-muted);
  font-size: 12px;
}

.cover {
  width: 100%;
  border-radius: 12px;
  object-fit: cover;
  border: 1px solid color-mix(in srgb, var(--dl-accent) 20%, transparent);
  box-shadow: var(--dl-shadow-sm);
}

.note-item :deep(.el-textarea__inner) {
  border-radius: 12px;
}

.mt {
  margin-top: 12px;
}

.attachments-grid {
  --attachment-size: 92px;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(var(--attachment-size), 1fr));
  gap: 10px;
  margin-top: 10px;
  align-items: start;
}

.attachment-add {
  height: var(--attachment-size);
  border-radius: 12px;
  border: 1px dashed color-mix(in srgb, var(--dl-accent) 38%, transparent);
  background: var(--dl-bg-alt);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: var(--el-text-color-secondary);
  position: relative;
  overflow: hidden;
}

.attachment-add :deep(.el-upload) {
  display: flex;
  align-items: center;
  justify-content: center;
}

.attachment-add-label {
  font-size: 12px;
}

.attachment-thumb {
  position: relative;
  width: 100%;
  height: var(--attachment-size);
}

.attachment-image,
.attachment-fallback {
  width: 100%;
  height: 100%;
  border-radius: 10px;
  border: 1px solid color-mix(in srgb, var(--dl-accent) 18%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: var(--dl-bg-alt);
}

.attachment-remove {
  position: absolute;
  top: -8px;
  right: -6px;
  padding: 0;
}

.purchase-card {
  --purchase-bottom-height: 160px;
  border: 1px solid var(--el-border-color-lighter);
  border-radius: var(--dl-radius-lg);
  box-shadow: var(--dl-shadow-md);
}

.purchase-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.purchase-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.purchase-title {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.purchase-title-left {
  display: flex;
  align-items: center;
  gap: 10px;
  min-width: 0;
}

.purchase-title-main {
  font-weight: 600;
  color: var(--el-text-color-primary);
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.purchase-type-tag {
  flex: 0 0 auto;
}

.purchase-title-meta {
  color: var(--el-text-color-secondary);
  font-size: 12px;
  white-space: nowrap;
}
.purchase-title-actions {
  display: flex;
  gap: 8px;
  align-items: center;
}

.purchase-attachments-col .attachments-grid {
  /* 保持最小高度，但使用弹性布局使上传区可以撑满与备注框一致 */
  min-height: var(--purchase-bottom-height);
  align-content: start;
  display: flex;
  gap: 12px;
  align-items: flex-start;
  flex-wrap: wrap;
  margin-top: 0;
}

/* 上传添加框，视觉上与备注框保持一致 */
.purchase-attachments-col .attachment-add {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 12px;
  min-width: 150px;
  min-height: 136px;
  border: 1px dashed color-mix(in srgb, var(--dl-accent) 36%, transparent);
  border-radius: 12px;
  background: var(--dl-bg-alt);
  box-shadow: var(--dl-shadow-sm);
}

.purchase-attachments-col .attachment-add--full {
  width: 100%;
  min-width: 0;
  min-height: var(--purchase-bottom-height);
  padding: 0;
}

.purchase-attachments-col .attachment-add--full :deep(.unified-uploader),
.purchase-attachments-col .attachment-add--full :deep(.el-upload),
.purchase-attachments-col .attachment-add--full :deep(.el-upload-dragger) {
  width: 100%;
  height: 100%;
}

.purchase-attachments-col .attachment-add--full :deep(.el-upload-dragger) {
  border: none;
  padding: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 12px;
}

.purchase-attachments-col .attachment-add--full :deep(.upload-drag-content) {
  padding: 0 18px;
}

.purchase-notes-col :deep(.el-textarea__inner) {
  min-height: var(--purchase-bottom-height);
  resize: none;
}

.purchase-attachments-col .attachment-add .attachment-add-label {
  margin-top: 8px;
  color: var(--dl-muted);
  font-size: 12px;
}

.purchase-attachments-col .attachment-add :deep(.el-button) {
  border-radius: 20px;
  padding: 6px 14px;
  box-shadow: var(--dl-shadow-sm);
}

.purchase-attachments-col .attachment-thumb {
  position: relative;
  width: 88px;
  height: 88px;
}

.purchase-attachments-col .attachment-image,
.purchase-attachments-col .attachment-fallback {
  width: 100%;
  height: 100%;
  border-radius: 8px;
  border: 1px solid color-mix(in srgb, var(--dl-accent) 18%, transparent);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--el-text-color-secondary);
  background: var(--dl-bg-alt);
}

.purchase-attachments-col .attachment-remove {
  position: absolute;
  top: -8px;
  right: -6px;
  padding: 0;
}

@media (max-width: 768px) {
  .purchase-attachments-col .attachments-grid,
  .purchase-notes-col .el-input__inner,
  .purchase-notes-col .el-textarea__inner {
    min-height: 120px;
  }
}

@media (max-width: 768px) {
  .purchase-attachments-col .attachments-grid,
  .purchase-notes-col .el-input__inner {
    min-height: 120px;
  }
}


.brand-field :deep(.el-select) {
  width: 100%;
  min-width: 0;
  flex: 1;
}

.brand-field :deep(.el-input) {
  flex: 1;
  min-width: 0;
}

.fields-panel :deep(.el-input),
.fields-panel :deep(.el-select),
.fields-panel :deep(.el-date-editor),
.fields-panel :deep(.el-input-number),
.purchase-card :deep(.el-input),
.purchase-card :deep(.el-select),
.purchase-card :deep(.el-date-editor),
.purchase-card :deep(.el-input-number) {
  width: 100%;
  min-width: 0;
}

.purchase-card .el-form-item {
  margin-bottom: 8px;
  align-items: center;
  gap: 8px;
}

.label-action {
  margin-left: 4px;
}

.label-action :deep(.el-icon) {
  font-size: 14px;
}

.inline-action {
  padding: 0 6px;
}

@media (max-width: 960px) {
  .form-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 768px) {
  .purchase-title {
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }

  .purchase-title-actions {
    width: 100%;
    justify-content: space-between;
  }

  .brand-field {
    flex-direction: column;
    align-items: stretch;
  }

  .brand-field :deep(.el-select) {
    width: 100%;
  }

  :deep(.el-dialog) {
    width: 94vw !important;
  }
}
</style>
