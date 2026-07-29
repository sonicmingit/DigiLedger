<template>
  <el-drawer v-model="workspace.assetEditorOpen" :title="workspace.editingAsset ? '编辑物品' : '新增物品'" size="min(840px, calc(100vw - 24px))" class="asset-editor-drawer" destroy-on-close @closed="reset">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="dl-form">
      <section class="form-section">
        <div class="section-heading"><div><strong>基础信息</strong><span>先建立物品的基本档案，必填项已标记。</span></div></div>
        <div class="form-grid">
          <el-form-item label="物品名称" prop="name"><el-input v-model="form.name" placeholder="例如：Sony WH-1000XM5" /></el-form-item>
          <el-form-item label="分类" prop="categoryId"><QuickCategorySelect v-model="form.categoryId" :categories="categoryOptions" @created="addCategory" /></el-form-item>
          <el-form-item label="品牌"><QuickBrandSelect v-model="form.brandId" :brands="brands" @created="addBrand" /></el-form-item>
          <el-form-item label="型号"><el-input v-model="form.model" placeholder="例如：WH-1000XM5" /></el-form-item>
          <el-form-item label="状态"><el-select v-model="form.status"><el-option v-for="s in statuses" :key="s" :label="s" :value="s" /></el-select></el-form-item>
          <el-form-item label="标签"><el-select v-model="form.tagIds" multiple filterable clearable collapse-tags :max-collapse-tags="3" placeholder="选择标签（可多选）"><el-option v-for="tag in flatTags" :key="tag.id" :label="tag.name" :value="tag.id" /></el-select></el-form-item>
        </div>
      </section>
      <section class="form-section">
        <div class="section-heading"><div><strong>购买与质保</strong><span>填写主商品的订单信息；质保到期日会自动计算。</span></div></div>
        <div class="form-grid">
          <el-form-item label="购买日期"><el-date-picker v-model="form.purchaseDate" value-format="YYYY-MM-DD" placeholder="请选择购买日期" :shortcuts="dateShortcuts" /></el-form-item>
          <el-form-item label="购买平台"><el-select v-model="primaryPurchase.platformId" clearable placeholder="请选择平台"><el-option v-for="platform in platforms" :key="platform.id" :label="platform.name" :value="platform.id" /></el-select></el-form-item>
          <el-form-item label="购买价格"><el-input-number v-model="primaryPurchase.price" :min="0" :precision="2" placeholder="请输入购买价格" /></el-form-item>
          <el-form-item label="质保月数"><el-input-number v-model="primaryPurchase.warrantyMonths" :min="0" :precision="0" placeholder="例如：12" /></el-form-item>
          <el-form-item label="质保到期"><el-date-picker v-model="primaryPurchase.warrantyExpireDate" value-format="YYYY-MM-DD" disabled placeholder="根据购买日期和质保月数自动计算" /></el-form-item>
        </div>
      </section>
      <section class="form-section form-section--details">
        <div class="section-heading"><div><strong>封面与备注</strong><span>补充易识别的图片和使用说明。</span></div></div>
        <el-form-item label="物品封面"><div class="upload-row"><button v-if="form.coverImageUrl" type="button" class="cover-preview has-image" @click="coverPreviewOpen=true"><img :src="form.coverImageUrl" alt="物品封面预览" /><span>点击放大预览</span></button><div v-else class="cover-preview empty"><strong>尚未添加封面</strong><small>上传图片或从外接服务搜图</small></div><div class="cover-tools"><AttachmentDropzone label="上传封面" hint="拖拽、点击选择，或直接粘贴图片" accept="image/*" :multiple="false" compact :disabled="uploading" @files="uploadCover" /><div class="cover-actions"><button type="button" class="cover-action-button search" @click="openImageSearch"><span>⌕</span>从外接服务搜图</button><button type="button" class="cover-action-button remove-bg" :disabled="!form.coverImageUrl || removeBgLoading" @click="openRemoveBgPreview"><span>✦</span>{{ removeBgLoading ? '正在抠图…' : 'remove.bg 抠图' }}</button></div></div></div></el-form-item>
        <el-form-item label="其他图片与附件" class="asset-attachments-field"><div class="asset-attachments"><AttachmentDropzone label="添加图片或附件" hint="可多选；支持拖拽、点击选择或粘贴截图" :disabled="uploading" @files="files => uploadPurchaseAttachments(files, primaryPurchase)" /><div v-for="(attachment, attachmentIndex) in primaryPurchase.attachments || []" :key="`${attachment}-${attachmentIndex}`" class="asset-attachment-card"><button type="button" class="asset-attachment-preview" :aria-label="`预览附件 ${attachmentIndex + 1}`" @click="openAttachmentPreview(attachmentIndex)"><img v-if="isImageAttachment(attachment)" :src="attachment" :alt="`附件 ${attachmentIndex + 1}`" /><span v-else class="attachment-file-mark">附件</span><small>附件 {{ attachmentIndex + 1 }}</small></button><button type="button" :aria-label="`移除附件 ${attachmentIndex + 1}`" @click="removePurchaseAttachment(primaryPurchase, attachment)">×</button></div></div></el-form-item>
        <div class="detail-grid">
          <el-form-item label="使用时间"><div class="manual-use-inputs"><label><el-input-number v-model="manualUseYears" :min="0" :precision="0" :controls="false" /><span>年</span></label><i>+</i><label><el-input-number v-model="manualUseRemainingMonths" :min="0" :max="11" :precision="0" :controls="false" /><span>个月</span></label></div><small class="field-hint">选填，按已实际使用时长记录。</small></el-form-item>
          <el-form-item label="相关链接"><div class="related-links"><div class="related-link-row purchase-link"><span>购买链接</span><el-input v-model="primaryPurchase.productLink" placeholder="选填，方便日后查看订单或商品" /></div><div v-for="(link, index) in form.relatedLinks || []" :key="index" class="related-link-row"><el-input v-model="link.url" class="related-link-url" placeholder="https://" /><div class="related-link-meta"><el-input v-model="link.description" placeholder="链接说明（选填）" /><button type="button" class="remove-link-button" :aria-label="`删除相关链接 ${index + 1}`" @click="removeRelatedLink(index)">×</button></div></div><button type="button" class="text-button add-link-button" @click="addRelatedLink">＋ 添加其他链接</button></div></el-form-item>
        </div>
        <el-form-item label="备注"><el-input v-model="form.notes" type="textarea" :rows="3" placeholder="记录购买缘由、使用状态或其他需要留存的信息" /></el-form-item>
      </section>
      <section class="form-section">
        <div class="section-heading"><div><strong>附加购买记录</strong><span>可添加配件、服务等额外支出。</span></div><button type="button" class="text-button" @click="addPurchase">添加记录</button></div>
        <div v-for="({ purchase, index }, displayIndex) in extraPurchases" :key="index" class="purchase-row">
          
          <div class="purchase-row-heading"><div class="record-title"><span class="record-kind" :class="purchase.type.toLowerCase()">{{ purchaseTypeLabel(purchase.type) }}</span><strong>记录 {{ displayIndex + 1 }}</strong></div><div class="record-summary"><span>¥ {{ safeAmount(purchase.price) }}</span><span>{{ purchase.purchaseDate || '未填写日期' }}</span><button type="button" class="text-button danger" @click="form.purchases.splice(index, 1)">删除记录</button></div></div>
          <div class="purchase-grid">
            <el-form-item label="类型"><span v-if="purchase.type === 'PRIMARY'" class="primary-record-label">主商品</span><el-select v-else v-model="purchase.type"><el-option label="配件" value="ACCESSORY" /><el-option label="服务" value="SERVICE" /></el-select></el-form-item>
            <el-form-item label="平台"><el-select v-model="purchase.platformId" clearable placeholder="请选择平台"><el-option v-for="platform in platforms" :key="platform.id" :label="platform.name" :value="platform.id" /></el-select></el-form-item>
            <el-form-item label="金额"><el-input-number v-model="purchase.price" :min="0" :precision="2" /></el-form-item>
            <el-form-item label="名称"><el-input v-model="purchase.name" :placeholder="purchase.type === 'PRIMARY' ? form.name || '物品名称' : '配件或服务名称'" :disabled="purchase.type === 'PRIMARY'" /></el-form-item>
            <el-form-item label="卖家/店铺"><el-input v-model="purchase.seller" placeholder="选填" /></el-form-item>
            <el-form-item label="购买链接"><el-input v-model="purchase.productLink" placeholder="选填" /></el-form-item>
            <el-form-item label="运费"><el-input-number v-model="purchase.shippingCost" :min="0" :precision="2" /></el-form-item>
            <el-form-item label="购买日期"><el-date-picker v-model="purchase.purchaseDate" value-format="YYYY-MM-DD" placeholder="请选择购买日期" :shortcuts="dateShortcuts" /></el-form-item>
            <el-form-item label="质保月数"><el-input-number v-model="purchase.warrantyMonths" :min="0" :precision="0" /></el-form-item>
            <el-form-item label="质保到期"><el-date-picker v-model="purchase.warrantyExpireDate" value-format="YYYY-MM-DD" clearable placeholder="请选择质保到期日" :shortcuts="dateShortcuts" /></el-form-item>
            <el-form-item label="数量"><el-input-number v-model="purchase.quantity" :min="1" :precision="0" /></el-form-item>
          </div>
          <div class="purchase-bottom-grid"><el-form-item label="备注"><el-input v-model="purchase.notes" type="textarea" :rows="5" placeholder="记录补充说明、订单信息或使用场景" /></el-form-item><el-form-item label="附件"><div class="purchase-attachment"><AttachmentDropzone panel label="添加附件" hint="拖拽文件、点击选择，或直接粘贴截图" @files="files => uploadPurchaseAttachments(files, purchase)" /><span v-for="(attachment, attachmentIndex) in purchase.attachments || []" :key="`${attachment}-${attachmentIndex}`" class="attachment-chip">附件 {{ attachmentIndex + 1 }}<button type="button" @click="removePurchaseAttachment(purchase, attachment)">×</button></span></div></el-form-item></div>
        </div>
      </section>
    </el-form>
    <template #footer><button class="secondary-button" @click="workspace.closeAssetEditor()">取消</button><PrimaryButton label="保存物品" :loading="saving" @click="submit" /></template>
  </el-drawer>
  <el-dialog v-model="imageSearchOpen" title="从外接服务搜图" width="760px">
    <div class="image-search-bar">
      <el-select v-model="imageProvider" @change="resetImageSearchResults">
        <el-option v-for="item in enabledImageProviders" :key="item.name" :label="item.displayName" :value="item.name" />
      </el-select>
      <el-input v-model="imageQuery" :placeholder="form.name || '输入品牌 + 型号'" @keyup.enter="searchExternalImage(1)" />
      <PrimaryButton label="搜索" :loading="imageSearching" @click="searchExternalImage(1)" />
    </div>
    <div v-if="isMtPhotos" class="mt-search-options">
      <span>搜索方式</span>
      <div class="mt-search-mode" role="group" aria-label="MT Photos 搜索方式">
        <button type="button" :class="{ active: imageSearchMode==='KEYWORD' }" :aria-pressed="imageSearchMode==='KEYWORD'" @click="setMtSearchMode('KEYWORD')">关键字</button>
        <button type="button" :class="{ active: imageSearchMode==='CLIP' }" :aria-pressed="imageSearchMode==='CLIP'" @click="setMtSearchMode('CLIP')">CLIP语义</button>
      </div>
      <small>{{ imageSearchMode === 'CLIP' ? '按画面语义匹配，需图库已完成 CLIP 索引。' : '按文件名、元数据和已识别文本匹配。' }}</small>
    </div>
    <div v-if="isMtPhotos && imageSearched" class="image-search-summary">
      <span>共 {{ imageSearchTotalCount }} 条结果</span>
      <span>{{ imageSearchMode === 'CLIP' ? 'CLIP语义' : '关键字' }} · 第 {{ imageSearchPage }} / {{ imageSearchTotalPages }} 页</span>
    </div>
    <div class="image-search-grid">
      <button v-for="(item,index) in imageResults" :key="item.originalUrl || index" type="button" :disabled="imageSelecting" @click="selectExternalImage(item.originalUrl || undefined)">
        <img :src="item.thumbnailUrl || item.originalUrl || ''" :alt="item.title || '搜索结果'"/>
        <span>{{ imageSelecting ? '正在保存图片…' : item.title || '选择此图片' }}</span>
      </button>
      <p v-if="imageSearched&&!imageResults.length">没有可用图片，请检查服务凭据或换一个关键字。</p>
    </div>
    <div v-if="isMtPhotos && imageSearched && imageSearchTotalPages > 1" class="image-search-pagination">
      <button type="button" :disabled="imageSearchPage <= 1 || imageSearching" @click="searchExternalImage(imageSearchPage - 1)">上一页</button>
      <span>第 {{ imageSearchPage }} / {{ imageSearchTotalPages }} 页</span>
      <button type="button" :disabled="imageSearchPage >= imageSearchTotalPages || imageSearching" @click="searchExternalImage(imageSearchPage + 1)">下一页</button>
    </div>
  </el-dialog>
  <el-dialog v-model="coverPreviewOpen" title="物品封面预览" width="min(760px, calc(100vw - 32px))" class="cover-preview-dialog"><div class="cover-preview-stage"><img v-if="form.coverImageUrl" :src="form.coverImageUrl" alt="物品封面大图预览" /></div></el-dialog>
  <el-dialog v-model="attachmentPreviewOpen" :title="`附件 ${attachmentPreviewIndex + 1} / ${primaryPurchase.attachments?.length || 0}`" width="min(900px, calc(100vw - 32px))" class="attachment-preview-dialog" @closed="attachmentPreviewIndex=0"><div class="attachment-preview-stage"><button type="button" class="attachment-nav previous" :disabled="attachmentPreviewIndex === 0" aria-label="上一张附件" @click="attachmentPreviewIndex--">‹</button><img v-if="currentAttachment && isImageAttachment(currentAttachment)" :src="currentAttachment" :alt="`附件 ${attachmentPreviewIndex + 1}`" /><iframe v-else-if="currentAttachment" :src="currentAttachment" title="附件预览" /><div v-else class="attachment-preview-empty">暂无可预览附件</div><button type="button" class="attachment-nav next" :disabled="attachmentPreviewIndex >= (primaryPurchase.attachments?.length || 1) - 1" aria-label="下一张附件" @click="attachmentPreviewIndex++">›</button></div></el-dialog>
  <el-dialog v-model="removeBgPreviewOpen" title="remove.bg 抠图预览" width="620px" @closed="discardRemoveBgPreview"><div class="remove-bg-preview"><p>透明背景预览仅用于确认；点击“替换当前封面”后才会上传并保存。</p><img v-if="removeBgPreviewUrl" :src="removeBgPreviewUrl" alt="抠图预览" @error="handlePreviewImageError" /></div><template #footer><div class="remove-bg-dialog-footer"><button type="button" class="secondary-button" @click="removeBgPreviewOpen=false">取消并放弃</button><PrimaryButton label="替换当前封面" :loading="removeBgApplying" @click="applyRemoveBgPreview" /></div></template></el-dialog>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createAsset, updateAsset } from '@/api/assets'
import { fetchBrands, fetchCategories, fetchImageSearchProviders, fetchPlatforms, fetchTags, testExternalApiConfig, testMtPhotosSearch } from '@/api/settings'
import { importRemoteImage, previewRemoveBackground, uploadFile } from '@/api/files'
import { useWorkspaceStore } from '@/stores/workspace'
import type { AssetPayload, AssetRelatedLink, AssetStatus, BrandItem, CategoryNode, ExternalApiTestItem, ImageSearchProvider, PlatformItem, PurchaseRecord, TagNode } from '@/types'
import PrimaryButton from './PrimaryButton.vue'
import AttachmentDropzone from './AttachmentDropzone.vue'
import QuickBrandSelect from './QuickBrandSelect.vue'
import QuickCategorySelect from './QuickCategorySelect.vue'

type AssetForm = Omit<AssetPayload, 'purchases'> & { purchases: PurchaseRecord[] }
const workspace = useWorkspaceStore(); const formRef = ref<FormInstance>(); const saving = ref(false); const uploading = ref(false)
const brands = ref<BrandItem[]>([]); const categoryOptions = ref<CategoryNode[]>([]); const platforms = ref<PlatformItem[]>([]); const tags = ref<TagNode[]>([]); const statuses: AssetStatus[] = ['使用中', '已闲置', '待出售', '已出售', '已丢弃']
const dateShortcuts = [{ text: '今天', value: () => new Date() }]
const blankPurchase = (): PurchaseRecord => ({ type: 'PRIMARY', price: 0, shippingCost: 0, quantity: 1, purchaseDate: new Date().toISOString().slice(0, 10), warrantyMonths: 12, attachments: [] })
const blank = (): AssetForm => ({ name: '', categoryId: undefined as unknown as number, status: '使用中', tagIds: [], relatedLinks: [], purchases: [blankPurchase()] })
const form = reactive<AssetForm>(blank())
const imageSearchOpen=ref(false),imageSearching=ref(false),imageSearched=ref(false),imageSelecting=ref(false),imageQuery=ref(''),imageProvider=ref(''),imageResults=ref<ExternalApiTestItem[]>([]),enabledImageProviders=ref<ImageSearchProvider[]>([])
const imageSearchMode=ref<'KEYWORD'|'CLIP'>('CLIP'),imageSearchPage=ref(1),imageSearchTotalPages=ref(1),imageSearchTotalCount=ref(0)
const coverPreviewOpen=ref(false)
const attachmentPreviewOpen=ref(false),attachmentPreviewIndex=ref(0)
const currentAttachment = computed(() => primaryPurchase.value.attachments?.[attachmentPreviewIndex.value])
const removeBgPreviewOpen=ref(false),removeBgLoading=ref(false),removeBgApplying=ref(false),removeBgPreviewUrl=ref(''),removeBgPreviewFile=ref<File>()
const isMtPhotos = computed(() => imageProvider.value === 'MT_PHOTOS')
const primaryPurchase = computed<PurchaseRecord>(() => form.purchases.find(purchase => purchase.type === 'PRIMARY') || form.purchases[0]!)
const manualUseYears = computed({ get: () => Math.floor((form.manualUseMonths || 0) / 12), set: value => setManualUseDuration(value, manualUseRemainingMonths.value) })
const manualUseRemainingMonths = computed({ get: () => (form.manualUseMonths || 0) % 12, set: value => setManualUseDuration(manualUseYears.value, value) })
const extraPurchases = computed(() => form.purchases.map((purchase, index) => ({ purchase, index })).filter(({ purchase }) => purchase.type !== 'PRIMARY'))
const flatTags = computed(() => flattenTags(tags.value))
const rules: FormRules = { name: [{ required: true, message: '请输入物品名称', trigger: 'blur' }], categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }] }

function normalizePurchases(purchases: PurchaseRecord[] = []) {
  const copied = purchases.map(purchase => ({ ...purchase }))
  const primaryIndex = copied.findIndex(purchase => purchase.type === 'PRIMARY')
  if (primaryIndex > 0) copied.unshift(copied.splice(primaryIndex, 1)[0])
  if (primaryIndex < 0) copied.unshift(blankPurchase())
  return copied
}
/**
 * Reactive objects keep keys that are absent from Object.assign's source. Remove
 * those keys first so a new form cannot inherit optional values from the last
 * asset (for example cover, notes, model, or purchase records).
 */
function replaceForm(values: AssetForm) {
  const target = form as unknown as Record<string, unknown>
  Object.keys(target).forEach(key => delete target[key])
  Object.assign(form, values)
}
function editorValues(): AssetForm {
  if (!workspace.editingAsset) return blank()
  return {
    ...workspace.editingAsset,
    categoryId: workspace.editingAsset.categoryId ?? (undefined as unknown as number),
    brand: undefined,
    brandId: workspace.editingAsset.brand?.id ?? undefined,
    tagIds: workspace.editingAsset.tags?.map(tag => tag.id) || [],
    purchases: normalizePurchases(workspace.editingAsset.purchases)
  }
}
watch(() => workspace.assetEditorOpen, open => { if (open) replaceForm(editorValues()) })
function fillBlankNameFromBrandAndModel(){if(form.name.trim())return;const brand=brands.value.find(item=>item.id===form.brandId)?.name?.trim();const model=form.model?.trim();if(brand&&model)form.name=`${brand}-${model}`}
watch([()=>form.brandId,()=>form.model,brands],fillBlankNameFromBrandAndModel)
// 后端会以主商品购买记录的日期回写物品购买日期，两个字段必须始终保持一致。
watch(() => form.purchaseDate, purchaseDate => {
  if (purchaseDate && primaryPurchase.value) primaryPurchase.value.purchaseDate = purchaseDate
})
function warrantyExpiryDate(purchaseDate?: string, warrantyMonths?: number) {
  if (!purchaseDate || warrantyMonths === undefined || warrantyMonths === null) return undefined
  const [year, month, day] = purchaseDate.split('-').map(Number)
  if (!year || !month || !day) return undefined
  const targetMonth = month - 1 + warrantyMonths
  const targetYear = year + Math.floor(targetMonth / 12)
  const normalizedMonth = ((targetMonth % 12) + 12) % 12
  const lastDay = new Date(targetYear, normalizedMonth + 1, 0).getDate()
  return `${targetYear}-${String(normalizedMonth + 1).padStart(2, '0')}-${String(Math.min(day, lastDay)).padStart(2, '0')}`
}
watch(
  () => [primaryPurchase.value.purchaseDate, primaryPurchase.value.warrantyMonths] as const,
  ([purchaseDate, warrantyMonths]) => { primaryPurchase.value.warrantyExpireDate = warrantyExpiryDate(purchaseDate, warrantyMonths) }
)
async function loadImageSearchProviders(){const response=await fetchImageSearchProviders();enabledImageProviders.value=response.providers.filter(provider=>provider.available&&response.enabledProviders.includes(provider.name));if(!enabledImageProviders.value.some(provider=>provider.name===imageProvider.value))imageProvider.value=enabledImageProviders.value[0]?.name||''}
async function openImageSearch(){try{await loadImageSearchProviders();resetImageSearchResults();imageSearchOpen.value=true}catch(error){ElMessage.error((error as Error).message)}}
onMounted(async () => { const [categories, brandList, platformList, tagList] = await Promise.allSettled([fetchCategories(), fetchBrands(), fetchPlatforms(), fetchTags()]); if (categories.status === 'fulfilled') categoryOptions.value = categories.value; if (brandList.status === 'fulfilled') brands.value = brandList.value; if (platformList.status === 'fulfilled') platforms.value = platformList.value; if (tagList.status === 'fulfilled') tags.value = tagList.value })
function flattenTags(nodes: TagNode[]): TagNode[] { return nodes.flatMap(tag => [tag, ...flattenTags(tag.children || [])]) }
const addPurchase = () => form.purchases.push({ ...blankPurchase(), type: 'ACCESSORY', name: '' })
function setManualUseDuration(years: number | undefined, months: number | undefined) { const total = Math.max(0, Number(years) || 0) * 12 + Math.max(0, Number(months) || 0); form.manualUseMonths = total || undefined }
function addRelatedLink() { form.relatedLinks = [...(form.relatedLinks || []), { url: '', description: '' }] }
function removeRelatedLink(index: number) { form.relatedLinks = (form.relatedLinks || []).filter((_, currentIndex) => currentIndex !== index) }
function addBrand(brand: BrandItem) { brands.value = [...brands.value, brand].sort((a, b) => a.name.localeCompare(b.name, 'zh-CN')) }
function insertCategory(nodes: CategoryNode[], category: CategoryNode): CategoryNode[] { if (!category.parentId) return [...nodes, category]; return nodes.map(node => node.id === category.parentId ? { ...node, children: [...(node.children || []), category] } : { ...node, children: insertCategory(node.children || [], category) }) }
function addCategory(category: CategoryNode) { categoryOptions.value = insertCategory(categoryOptions.value, category) }
const purchaseTypeLabel = (type: PurchaseRecord['type']) => ({ PRIMARY: '主商品', ACCESSORY: '配件', SERVICE: '服务' }[type])
const safeAmount = (value: unknown) => Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const isImageAttachment = (url: string) => /\.(avif|gif|jpe?g|png|svg|webp)(?:$|[?#])/i.test(url)
function openAttachmentPreview(index: number) { attachmentPreviewIndex.value = index; attachmentPreviewOpen.value = true }
async function uploadPurchaseAttachments(files: File[], purchase: PurchaseRecord) { try { const results = await Promise.all(files.map(uploadFile)); purchase.attachments = [...(purchase.attachments || []), ...results.map(result => result.url)]; ElMessage.success(`已上传 ${results.length} 个附件`) } catch (error) { ElMessage.error((error as Error).message) } }
function removePurchaseAttachment(purchase: PurchaseRecord, attachment: string) { purchase.attachments = (purchase.attachments || []).filter(item => item !== attachment) }
const uploadCover = async (files: File[]) => { const file = files[0]; if (!file) return; uploading.value = true; try { form.coverImageUrl = (await uploadFile(file)).url; ElMessage.success('封面上传成功') } catch (e) { ElMessage.error((e as Error).message) } finally { uploading.value = false } }
function resetImageSearchResults(){imageResults.value=[];imageSearched.value=false;imageSearchPage.value=1;imageSearchTotalPages.value=1;imageSearchTotalCount.value=0}
function setMtSearchMode(mode:'KEYWORD'|'CLIP'){if(imageSearchMode.value===mode)return;imageSearchMode.value=mode;resetImageSearchResults()}
async function searchExternalImage(page=1){if(!imageProvider.value){ElMessage.warning('请先在系统设置启用一个搜图服务');return}const query=(imageQuery.value||form.name).trim();if(!query){ElMessage.warning('请输入搜索内容');return}imageSearching.value=true;try{if(isMtPhotos.value){const result=await testMtPhotosSearch({query,mode:imageSearchMode.value,page});imageResults.value=result.items.map(item=>({thumbnailUrl:item.thumbnailUrl,originalUrl:item.thumbnailUrl,title:item.fileName||`MT Photos 文件 #${item.id}`,sourceUrl:`MT Photos · ID ${item.id}`}));imageSearchPage.value=result.page;imageSearchTotalPages.value=result.totalPages;imageSearchTotalCount.value=result.totalCount}else{const result=await testExternalApiConfig(imageProvider.value,{query});imageResults.value=result.items;imageSearchPage.value=1;imageSearchTotalPages.value=1;imageSearchTotalCount.value=result.resultCount}imageSearched.value=true}catch(e){ElMessage.error((e as Error).message)}finally{imageSearching.value=false}}
async function selectExternalImage(url?:string|null){if(!url||imageSelecting.value)return;imageSelecting.value=true;try{form.coverImageUrl=(await importRemoteImage(url)).url;imageSearchOpen.value=false;ElMessage.success('已将搜图结果保存为封面')}catch(error){ElMessage.error((error as Error).message)}finally{imageSelecting.value=false}}
async function openRemoveBgPreview(){if(!form.coverImageUrl)return;removeBgLoading.value=true;try{const blob=await previewRemoveBackground({assetId:workspace.editingAsset?.id,coverUrl:form.coverImageUrl});discardRemoveBgPreview();removeBgPreviewFile.value=new File([blob],'cover-no-bg.png',{type:blob.type||'image/png'});removeBgPreviewUrl.value=URL.createObjectURL(blob);removeBgPreviewOpen.value=true}catch(error){ElMessage.error((error as Error).message)}finally{removeBgLoading.value=false}}
async function applyRemoveBgPreview(){if(!removeBgPreviewFile.value)return;removeBgApplying.value=true;try{form.coverImageUrl=(await uploadFile(removeBgPreviewFile.value)).url;removeBgPreviewOpen.value=false;ElMessage.success('抠图结果已替换为当前封面')}catch(error){ElMessage.error((error as Error).message)}finally{removeBgApplying.value=false}}
function handlePreviewImageError(){ElMessage.error('抠图预览格式无法显示，请检查 remove.bg 服务配置')}
function discardRemoveBgPreview(){if(removeBgPreviewUrl.value)URL.revokeObjectURL(removeBgPreviewUrl.value);removeBgPreviewUrl.value='';removeBgPreviewFile.value=undefined}
const reset = () => { discardRemoveBgPreview(); imageSearchOpen.value=false; coverPreviewOpen.value=false; attachmentPreviewOpen.value=false; attachmentPreviewIndex.value=0; removeBgPreviewOpen.value=false; formRef.value?.resetFields(); replaceForm(blank()) }
function buildAssetPayload(): AssetPayload {
  return {
    name: form.name.trim(),
    categoryId: form.categoryId,
    brandId: form.brandId || undefined,
    model: form.model?.trim() || undefined,
    serialNo: form.serialNo?.trim() || undefined,
    status: form.status,
    purchaseDate: form.purchaseDate || undefined,
    retiredDate: form.retiredDate || undefined,
    coverImageUrl: form.coverImageUrl || undefined,
    relatedLinks: (form.relatedLinks || []).filter(link => link.url.trim()).map(link => ({ url: link.url.trim(), description: link.description?.trim() || undefined })) as AssetRelatedLink[],
    manualUseMonths: form.manualUseMonths,
    notes: form.notes?.trim() || undefined,
    tagIds: [...(form.tagIds || [])],
    purchases: form.purchases.map(({ type, name, platformId, seller, price, shippingCost, quantity, purchaseDate, warrantyMonths, warrantyExpireDate, productLink, attachments, notes }) => ({
      type,
      name: name?.trim() || undefined,
      platformId: platformId || undefined,
      seller: seller?.trim() || undefined,
      price,
      shippingCost,
      quantity,
      purchaseDate: type === 'PRIMARY' ? form.purchaseDate || purchaseDate : purchaseDate,
      warrantyMonths,
      warrantyExpireDate,
      productLink: productLink?.trim() || undefined,
      attachments: [...(attachments || [])],
      notes: notes?.trim() || undefined
    }))
  }
}
const submit = async () => { if (!await formRef.value?.validate().catch(() => false)) return; saving.value = true; try { const payload = buildAssetPayload(); if (workspace.editingAsset) await updateAsset(workspace.editingAsset.id, payload); else await createAsset(payload); ElMessage.success('物品已保存'); workspace.closeAssetEditor(true) } catch (e) { ElMessage.error((e as Error).message) } finally { saving.value = false } }
</script>

<style scoped>
.dl-form{display:flex;flex-direction:column;gap:18px}.form-section{padding:22px;border:1px solid #e1e7db;border-radius:20px;background:#fafcf8;box-shadow:0 1px 1px rgba(35,49,31,.02)}.form-grid,.detail-grid{display:grid;grid-template-columns:repeat(2,minmax(0,1fr));gap:0 28px}.form-grid :deep(.el-select),.form-grid :deep(.el-date-editor),.form-grid :deep(.el-input-number){width:100%}.form-grid :deep(.el-input-number .el-input__wrapper){width:100%;box-sizing:border-box}.form-section :deep(.el-form-item){min-width:0;margin-bottom:16px}.form-section :deep(.el-form-item:last-child){margin-bottom:0}.section-heading{display:flex;align-items:flex-start;justify-content:space-between;gap:14px;margin-bottom:18px}.section-heading>div{display:flex;flex-direction:column;gap:4px}.section-heading strong{color:#273124;font-size:16px;letter-spacing:.01em}.section-heading span{color:var(--dl-muted);font-size:11px}.form-section--details :deep(.el-form-item){margin-bottom:20px}.manual-use-inputs{display:inline-flex;align-items:center;gap:9px;padding:7px 10px;border:1px solid #e0e6da;border-radius:14px;background:#fff}.manual-use-inputs :deep(.el-input-number){width:112px}.manual-use-inputs :deep(.el-input__wrapper){box-shadow:none!important;padding-right:0}.manual-use-inputs>span{color:#566052;font-size:12px;font-weight:700;white-space:nowrap}.field-hint{display:block;margin-top:7px;color:var(--dl-muted);font-size:10px}.related-links{display:flex;flex-direction:column;gap:10px}.related-link-row{display:grid;grid-template-columns:minmax(0,1.15fr) minmax(0,.85fr) 42px;gap:9px;align-items:center}.related-link-row.purchase-link{grid-template-columns:78px minmax(0,1fr);min-height:52px;box-sizing:border-box;padding:7px 10px;border:1px solid #dfe7d6;border-radius:14px;background:#f6faef}.related-link-row.purchase-link>span{color:#547417;font-size:11px;font-weight:800}.remove-link-button{display:grid;width:42px;height:42px;padding:0;place-items:center;border:0;border-radius:12px;background:#f8eff0;color:var(--dl-danger);font-size:19px;cursor:pointer;transition:.16s ease}.remove-link-button:hover{background:#f5dfe1}.add-link-button{align-self:flex-start;margin-top:2px}.asset-attachments{display:flex;flex-wrap:wrap;align-items:center;gap:10px;width:100%}.asset-attachments :deep(.attachment-dropzone){flex:1 1 360px;min-height:78px}.asset-attachment-card{position:relative;flex:0 0 96px;width:96px;height:78px;overflow:hidden;border:1px solid #e0e6da;border-radius:12px;background:#fff}.asset-attachment-card a{display:grid;width:100%;height:100%;place-items:center;overflow:hidden;color:var(--dl-text-secondary);text-decoration:none}.asset-attachment-card img{width:100%;height:100%;object-fit:cover}.asset-attachment-card small{position:absolute;right:0;bottom:0;left:0;padding:4px 6px;background:rgba(26,35,24,.72);color:#fff;font-size:9px;line-height:1.2}.attachment-file-mark{display:grid;place-items:center;width:36px;height:36px;border-radius:10px;background:#eef4e8;color:#5e7d25;font-size:10px;font-weight:800}.asset-attachment-card>button:not(.asset-attachment-preview){position:absolute;top:4px;right:4px;display:grid;width:18px;height:18px;padding:0;place-items:center;border:0;border-radius:50%;background:rgba(28,35,26,.8);color:#fff;font-size:14px;line-height:1;cursor:pointer;opacity:0;transition:opacity .16s ease}.asset-attachment-card:hover>button:not(.asset-attachment-preview),.asset-attachment-card:focus-within>button:not(.asset-attachment-preview){opacity:1}.purchase-row{display:block;margin-top:16px;padding:0 22px 22px;border:1px solid #e0e5da;border-radius:20px;background:#fff}.purchase-row-heading{display:flex;align-items:center;justify-content:space-between;gap:16px;min-height:68px;margin:0 -22px 20px;padding:0 22px;border-bottom:1px solid #e4e8df;background:#fff;border-radius:20px 20px 0 0}.record-title,.record-summary{display:flex;align-items:center;gap:12px}.record-title strong{font-size:16px}.record-kind{display:inline-flex;align-items:center;min-height:28px;padding:0 10px;border-radius:999px;background:#eef7e7;color:#58a727;font-size:11px;font-weight:800}.record-kind.accessory{background:#fff2df;color:#b87500}.record-kind.service{background:#eaf3ff;color:#367cc7}.record-summary{color:var(--dl-text-secondary);font-size:12px;font-weight:700}.record-summary .danger{margin-left:7px}.purchase-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:0 20px}.purchase-row :deep(.el-form-item){min-width:0;margin-bottom:14px}.purchase-row :deep(.el-input-number),.purchase-row :deep(.el-date-editor),.purchase-row :deep(.el-select){width:100%}.primary-record-label{display:flex;align-items:center;height:42px;padding:0 14px;border-radius:12px;background:var(--dl-accent-soft);color:#4d6e0b;font-size:12px;font-weight:700}.purchase-bottom-grid{display:grid;grid-template-columns:1fr 1fr;align-items:start;gap:20px}.purchase-bottom-grid :deep(.el-form-item){margin-bottom:0}.purchase-bottom-grid :deep(.el-textarea__inner){min-height:164px;box-sizing:border-box;resize:vertical}.purchase-attachment{display:flex;flex-wrap:wrap;gap:8px;width:100%}.purchase-attachment :deep(.attachment-dropzone){flex:1 1 100%}.attachment-chip{display:inline-flex;align-items:center;gap:6px;height:28px;padding:0 9px;border-radius:999px;background:#edf1e8;color:var(--dl-text-secondary);font-size:10px;font-weight:700}.attachment-chip button{width:16px;height:16px;border:0;border-radius:50%;background:#fff;color:var(--dl-danger);cursor:pointer}.upload-row{display:grid;grid-template-columns:116px minmax(0,1fr);align-items:stretch;gap:16px}.cover-preview{position:relative;min-height:116px;overflow:hidden;border:0;border-radius:18px;background:#fff}.cover-preview.empty{display:flex;align-items:center;justify-content:center;flex-direction:column;gap:5px;border:1px dashed #d9dfd4;background:#fafbf8;color:var(--dl-text-secondary);text-align:center}.cover-preview.empty strong{font-size:12px}.cover-preview.empty small{max-width:88px;font-size:10px;line-height:1.45}.cover-preview.has-image{padding:0;cursor:zoom-in}.cover-preview.has-image img{display:block;width:100%;height:100%;object-fit:cover;transition:transform .2s ease}.cover-preview.has-image:hover img{transform:scale(1.04)}.cover-preview.has-image span{position:absolute;right:7px;bottom:7px;padding:3px 6px;border-radius:5px;background:rgba(20,29,20,.72);color:#fff;font-size:9px;opacity:0;transition:opacity .18s ease}.cover-preview.has-image:hover span,.cover-preview.has-image:focus-visible span{opacity:1}.cover-tools{display:flex;min-height:116px;flex-direction:column}.upload-row :deep(.attachment-dropzone){max-width:none}.cover-tools .cover-actions{margin-top:10px}.cover-preview-stage{display:grid;min-height:240px;max-height:70vh;place-items:center;background:conic-gradient(#f0f2ee 25%,#fff 0 50%,#f0f2ee 0 75%,#fff 0) 0/20px 20px;border-radius:14px;overflow:hidden}.cover-preview-stage img{display:block;max-width:100%;max-height:68vh;object-fit:contain}
.cover-actions{display:flex;flex-wrap:wrap;gap:9px;margin-top:10px}.cover-action-button{display:inline-flex;align-items:center;justify-content:center;gap:6px;min-height:36px;padding:0 14px;border:1px solid #d7e0cf;border-radius:10px;background:#fff;color:#3e5140;font-size:12px;font-weight:800;cursor:pointer;transition:.18s ease}.cover-action-button span{font-size:18px;line-height:1}.cover-action-button.search{border-color:#a7d64c;background:#f5ffe2;color:#3f620c}.cover-action-button.search:hover{background:#eaffba;box-shadow:0 5px 12px rgba(101,151,26,.14)}.cover-action-button.remove-bg{border-color:#d9d6ef;background:#f8f7ff;color:#5f578a}.cover-action-button.remove-bg:hover:not(:disabled){background:#efedff;box-shadow:0 5px 12px rgba(89,75,164,.13)}.cover-action-button:disabled{opacity:.48;cursor:not-allowed}.image-search-bar{display:grid;grid-template-columns:220px minmax(0,1fr) 124px;align-items:center;gap:12px}.image-search-bar :deep(.el-select),.image-search-bar :deep(.el-input),.image-search-bar :deep(.primary-button){width:100%;height:44px}.image-search-bar :deep(.el-select__wrapper),.image-search-bar :deep(.el-input__wrapper){box-sizing:border-box;min-height:44px;border-radius:12px}.image-search-bar :deep(.primary-button){min-width:0;padding:0;border-radius:12px}.mt-search-options{display:flex;align-items:center;gap:10px;margin-top:12px;padding:9px 11px;border:1px solid #e2e8da;border-radius:12px;background:#f8faf5}.mt-search-options>span{color:#566052;font-size:11px;font-weight:800;white-space:nowrap}.mt-search-options small{margin-left:auto;color:var(--dl-text-secondary);font-size:10px}.mt-search-mode{display:flex;gap:4px;padding:3px;border-radius:9px;background:#e9eee4}.mt-search-mode button{height:28px;padding:0 11px;border:0;border-radius:7px;background:transparent;color:#6a7465;font-size:10px;font-weight:800;cursor:pointer}.mt-search-mode button.active{background:#263025;color:#dfffab;box-shadow:0 2px 7px rgba(38,48,37,.16)}.image-search-summary{display:flex;justify-content:space-between;align-items:center;margin-top:13px;color:var(--dl-text-secondary);font-size:10px}.image-search-summary span:first-child{font-weight:800;color:#4f5b4a}.image-search-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:10px;margin-top:10px}.image-search-grid button{border:1px solid #e1e4dd;border-radius:10px;background:#fff;overflow:hidden;text-align:left;cursor:pointer}.image-search-grid button:disabled{cursor:wait;opacity:.65}.image-search-grid img{width:100%;height:105px;object-fit:cover}.image-search-grid span{display:block;padding:7px;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:10px}.image-search-pagination{display:flex;align-items:center;justify-content:center;gap:12px;margin-top:16px;padding-top:12px;border-top:1px solid #edf0ea}.image-search-pagination button{height:32px;padding:0 14px;border:1px solid #dfe5d8;border-radius:9px;background:#fff;color:#344132;font-size:10px;font-weight:800;cursor:pointer}.image-search-pagination button:hover:not(:disabled){border-color:#93c74f;background:#f4ffe4}.image-search-pagination button:disabled{cursor:not-allowed;opacity:.42}.image-search-pagination span{min-width:90px;color:var(--dl-text-secondary);font-size:10px;text-align:center}.remove-bg-preview{display:flex;flex-direction:column;gap:14px}.remove-bg-preview p{margin:0;color:var(--dl-text-secondary);font-size:12px;line-height:1.6}.remove-bg-preview img{display:block;max-width:100%;max-height:430px;margin:0 auto;border:1px solid #e4e8df;border-radius:14px;background:conic-gradient(#f1f2ef 25%,#fff 0 50%,#f1f2ef 0 75%,#fff 0) 0/20px 20px;object-fit:contain}.remove-bg-dialog-footer{display:flex;justify-content:flex-end;align-items:center;gap:10px}.remove-bg-dialog-footer .secondary-button,.remove-bg-dialog-footer :deep(.primary-button){display:inline-flex;align-items:center;justify-content:center;width:146px;height:42px;min-height:42px;padding:0;border-radius:12px;font-size:13px;line-height:1}.remove-bg-dialog-footer .secondary-button{margin:0}
.manual-use-inputs{gap:10px;padding:8px 10px;background:#f5f8f1}.manual-use-inputs label{display:flex;align-items:center;gap:6px;min-width:0}.manual-use-inputs :deep(.el-input-number){width:76px}.manual-use-inputs :deep(.el-input__wrapper){padding:0 6px;background:#fff;box-shadow:0 0 0 1px #dfe7d6 inset!important}.manual-use-inputs label span{color:#547417;font-size:12px;font-weight:800;white-space:nowrap}.manual-use-inputs>i{color:#a7b09f;font-size:13px;font-style:normal}.related-link-row:not(.purchase-link){display:flex;flex-direction:column;gap:8px;padding:10px;border:1px solid #e1e7db;border-radius:14px;background:#fff}.related-link-url{width:100%}.related-link-meta{display:grid;grid-template-columns:minmax(0,1fr) 42px;gap:9px;align-items:center}.related-link-meta :deep(.el-input__wrapper){background:#fafcf8}.asset-attachment-preview{display:grid;width:100%;height:100%;padding:0;place-items:center;overflow:hidden;border:0;background:transparent;color:var(--dl-text-secondary);cursor:zoom-in}.asset-attachment-preview img{width:100%;height:100%;object-fit:cover}.asset-attachment-preview small{position:absolute;right:0;bottom:0;left:0;padding:4px 6px;background:rgba(26,35,24,.72);color:#fff;font-size:9px;line-height:1.2}.attachment-preview-stage{position:relative;display:grid;min-height:440px;max-height:72vh;place-items:center;overflow:hidden;border-radius:16px;background:#f4f6f2}.attachment-preview-stage img{display:block;max-width:100%;max-height:70vh;object-fit:contain}.attachment-preview-stage iframe{width:100%;height:70vh;border:0;background:#fff}.attachment-preview-empty{color:var(--dl-muted);font-size:13px}.attachment-nav{position:absolute;z-index:1;top:50%;display:grid;width:42px;height:42px;padding:0;place-items:center;border:0;border-radius:50%;background:rgba(27,36,25,.78);color:#fff;font-size:34px;line-height:1;cursor:pointer;transform:translateY(-50%);transition:.16s ease}.attachment-nav:hover:not(:disabled){background:#23321e}.attachment-nav:disabled{cursor:not-allowed;opacity:.25}.attachment-nav.previous{left:16px}.attachment-nav.next{right:16px}
@media(max-width:760px){.form-section{padding:18px}.form-grid,.detail-grid,.purchase-grid,.purchase-bottom-grid,.image-search-bar{grid-template-columns:1fr}.manual-use-inputs{width:max-content}.related-link-row{grid-template-columns:minmax(0,1fr) 42px}.related-link-row :deep(.el-input):nth-child(2){grid-column:1}.asset-attachments :deep(.attachment-dropzone){flex-basis:100%}.purchase-row{padding:0 16px 16px}.purchase-row-heading{align-items:flex-start;flex-direction:column;margin-right:-16px;margin-left:-16px;padding:14px 16px}.record-summary{flex-wrap:wrap}.upload-row{grid-template-columns:1fr}.cover-preview{min-height:200px}.upload-row :deep(.attachment-dropzone){max-width:none}.mt-search-options{align-items:flex-start;flex-wrap:wrap}.mt-search-options small{flex:1 1 100%;margin-left:0}.image-search-grid{grid-template-columns:repeat(2,1fr)}.related-link-row:not(.purchase-link){display:flex}.attachment-preview-stage{min-height:300px}.attachment-preview-stage iframe{height:56vh}}
</style>
