<template>
  <el-drawer v-model="workspace.assetEditorOpen" :title="workspace.editingAsset ? '编辑物品' : '新增物品'" size="min(840px, calc(100vw - 24px))" class="asset-editor-drawer" destroy-on-close @closed="reset">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="dl-form">
      <div class="form-grid">
        <el-form-item label="物品名称" prop="name"><el-input v-model="form.name" placeholder="例如：Sony WH-1000XM5" /></el-form-item>
        <el-form-item label="分类" prop="categoryId"><QuickCategorySelect v-model="form.categoryId" :categories="categoryOptions" @created="addCategory" /></el-form-item>
        <el-form-item label="品牌"><QuickBrandSelect v-model="form.brandId" :brands="brands" @created="addBrand" /></el-form-item>
        <el-form-item label="型号"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status"><el-option v-for="s in statuses" :key="s" :label="s" :value="s" /></el-select></el-form-item>
        <el-form-item label="购买日期"><el-date-picker v-model="form.purchaseDate" value-format="YYYY-MM-DD" placeholder="请选择购买日期" :shortcuts="dateShortcuts" /></el-form-item>
      </div>
      <el-form-item label="物品封面">
        <div class="upload-row"><img v-if="form.coverImageUrl" :src="form.coverImageUrl" alt="物品封面预览" /><AttachmentDropzone label="上传封面" hint="拖拽、点击选择，或直接粘贴图片" accept="image/*" :multiple="false" compact :disabled="uploading" @files="uploadCover" /></div>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.notes" type="textarea" :rows="3" /></el-form-item>
      <section class="form-section">
        <div class="section-heading"><div><strong>购买记录</strong><span>仅展示和维护配件、服务等附加记录</span></div><button type="button" class="text-button" @click="addPurchase">添加记录</button></div>
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
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { createAsset, updateAsset } from '@/api/assets'
import { fetchBrands, fetchCategories, fetchPlatforms } from '@/api/settings'
import { uploadFile } from '@/api/files'
import { useWorkspaceStore } from '@/stores/workspace'
import type { AssetPayload, AssetStatus, BrandItem, CategoryNode, PlatformItem, PurchaseRecord } from '@/types'
import PrimaryButton from './PrimaryButton.vue'
import AttachmentDropzone from './AttachmentDropzone.vue'
import QuickBrandSelect from './QuickBrandSelect.vue'
import QuickCategorySelect from './QuickCategorySelect.vue'

type AssetForm = Omit<AssetPayload, 'purchases'> & { purchases: PurchaseRecord[] }
const workspace = useWorkspaceStore(); const formRef = ref<FormInstance>(); const saving = ref(false); const uploading = ref(false)
const brands = ref<BrandItem[]>([]); const categoryOptions = ref<CategoryNode[]>([]); const platforms = ref<PlatformItem[]>([]); const statuses: AssetStatus[] = ['使用中', '已闲置', '待出售', '已出售', '已丢弃']
const dateShortcuts = [{ text: '今天', value: () => new Date() }]
const blankPurchase = (): PurchaseRecord => ({ type: 'PRIMARY', price: 0, shippingCost: 0, quantity: 1, purchaseDate: new Date().toISOString().slice(0, 10), warrantyMonths: 12, attachments: [] })
const blank = (): AssetForm => ({ name: '', categoryId: undefined as unknown as number, status: '使用中', purchases: [blankPurchase()] })
const form = reactive<AssetForm>(blank())
const extraPurchases = computed(() => form.purchases.map((purchase, index) => ({ purchase, index })).filter(({ purchase }) => purchase.type !== 'PRIMARY'))
const rules: FormRules = { name: [{ required: true, message: '请输入物品名称', trigger: 'blur' }], categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }] }

watch(() => workspace.assetEditorOpen, (open) => { if (open) Object.assign(form, blank(), workspace.editingAsset ? { ...workspace.editingAsset, brandId: workspace.editingAsset.brand?.id ?? undefined, purchases: workspace.editingAsset.purchases.map(p => ({ ...p })) } : {}) })
onMounted(async () => { const [categories, brandList, platformList] = await Promise.allSettled([fetchCategories(), fetchBrands(), fetchPlatforms()]); if (categories.status === 'fulfilled') categoryOptions.value = categories.value; if (brandList.status === 'fulfilled') brands.value = brandList.value; if (platformList.status === 'fulfilled') platforms.value = platformList.value })
const addPurchase = () => form.purchases.push({ ...blankPurchase(), type: 'ACCESSORY', name: '' })
function addBrand(brand: BrandItem) { brands.value = [...brands.value, brand].sort((a, b) => a.name.localeCompare(b.name, 'zh-CN')) }
function insertCategory(nodes: CategoryNode[], category: CategoryNode): CategoryNode[] { if (!category.parentId) return [...nodes, category]; return nodes.map(node => node.id === category.parentId ? { ...node, children: [...(node.children || []), category] } : { ...node, children: insertCategory(node.children || [], category) }) }
function addCategory(category: CategoryNode) { categoryOptions.value = insertCategory(categoryOptions.value, category) }
const purchaseTypeLabel = (type: PurchaseRecord['type']) => ({ PRIMARY: '主商品', ACCESSORY: '配件', SERVICE: '服务' }[type])
const safeAmount = (value: unknown) => Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
async function uploadPurchaseAttachments(files: File[], purchase: PurchaseRecord) { try { const results = await Promise.all(files.map(uploadFile)); purchase.attachments = [...(purchase.attachments || []), ...results.map(result => result.url)]; ElMessage.success(`已上传 ${results.length} 个附件`) } catch (error) { ElMessage.error((error as Error).message) } }
function removePurchaseAttachment(purchase: PurchaseRecord, attachment: string) { purchase.attachments = (purchase.attachments || []).filter(item => item !== attachment) }
const uploadCover = async (files: File[]) => { const file = files[0]; if (!file) return; uploading.value = true; try { form.coverImageUrl = (await uploadFile(file)).url; ElMessage.success('封面上传成功') } catch (e) { ElMessage.error((e as Error).message) } finally { uploading.value = false } }
const reset = () => { formRef.value?.resetFields(); Object.assign(form, blank()) }
const submit = async () => { if (!await formRef.value?.validate().catch(() => false)) return; saving.value = true; try { if (workspace.editingAsset) await updateAsset(workspace.editingAsset.id, form); else await createAsset(form); ElMessage.success('物品已保存'); workspace.closeAssetEditor(true) } catch (e) { ElMessage.error((e as Error).message) } finally { saving.value = false } }
</script>

<style scoped>
.dl-form>.form-grid{grid-template-columns:repeat(2,minmax(0,1fr));gap:0 22px}.form-section{margin-top:28px;padding-top:24px;border-top:1px solid #e4e8df}.section-heading{display:flex;align-items:flex-start;justify-content:space-between;gap:14px;margin-bottom:16px}.section-heading>div{display:flex;flex-direction:column;gap:4px}.section-heading strong{font-size:16px}.section-heading span{color:var(--dl-muted);font-size:11px}.purchase-row{display:block;margin-top:16px;padding:0 22px 22px;border:1px solid #e0e5da;border-radius:20px;background:#fafbf8}.purchase-row-heading{display:flex;align-items:center;justify-content:space-between;gap:16px;min-height:68px;margin:0 -22px 20px;padding:0 22px;border-bottom:1px solid #e4e8df;background:#fff;border-radius:20px 20px 0 0}.record-title,.record-summary{display:flex;align-items:center;gap:12px}.record-title strong{font-size:16px}.record-kind{display:inline-flex;align-items:center;min-height:28px;padding:0 10px;border-radius:999px;background:#eef7e7;color:#58a727;font-size:11px;font-weight:800}.record-kind.accessory{background:#fff2df;color:#b87500}.record-kind.service{background:#eaf3ff;color:#367cc7}.record-summary{color:var(--dl-text-secondary);font-size:12px;font-weight:700}.record-summary .danger{margin-left:7px}.purchase-grid{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:0 20px}.purchase-row :deep(.el-form-item){min-width:0;margin-bottom:14px}.purchase-row :deep(.el-input-number),.purchase-row :deep(.el-date-editor),.purchase-row :deep(.el-select){width:100%}.primary-record-label{display:flex;align-items:center;height:42px;padding:0 14px;border-radius:12px;background:var(--dl-accent-soft);color:#4d6e0b;font-size:12px;font-weight:700}.purchase-bottom-grid{display:grid;grid-template-columns:1fr 1fr;align-items:start;gap:20px}.purchase-bottom-grid :deep(.el-form-item){margin-bottom:0}.purchase-bottom-grid :deep(.el-textarea__inner){min-height:164px;box-sizing:border-box;resize:vertical}.purchase-attachment{display:flex;flex-wrap:wrap;gap:8px;width:100%}.purchase-attachment :deep(.attachment-dropzone){flex:1 1 100%}.attachment-chip{display:inline-flex;align-items:center;gap:6px;height:28px;padding:0 9px;border-radius:999px;background:#edf1e8;color:var(--dl-text-secondary);font-size:10px;font-weight:700}.attachment-chip button{width:16px;height:16px;border:0;border-radius:50%;background:#fff;color:var(--dl-danger);cursor:pointer}.upload-row{display:grid;grid-template-columns:116px minmax(0,1fr);align-items:center;gap:16px}.upload-row>img{width:116px;height:116px;object-fit:cover;border-radius:18px}.upload-row :deep(.attachment-dropzone){max-width:none}
@media(max-width:760px){.dl-form>.form-grid,.purchase-grid,.purchase-bottom-grid{grid-template-columns:1fr}.purchase-row{padding:0 16px 16px}.purchase-row-heading{align-items:flex-start;flex-direction:column;margin-right:-16px;margin-left:-16px;padding:14px 16px}.record-summary{flex-wrap:wrap}.upload-row{grid-template-columns:1fr}.upload-row>img{width:100%;height:200px}.upload-row :deep(.attachment-dropzone){max-width:none}}
</style>
