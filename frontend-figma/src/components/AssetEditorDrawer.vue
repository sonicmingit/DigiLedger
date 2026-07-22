<template>
  <el-drawer v-model="workspace.assetEditorOpen" :title="workspace.editingAsset ? '编辑物品' : '新增物品'" size="560px" destroy-on-close @closed="reset">
    <el-form ref="formRef" :model="form" :rules="rules" label-position="top" class="dl-form">
      <div class="form-grid">
        <el-form-item label="物品名称" prop="name"><el-input v-model="form.name" placeholder="例如：Sony WH-1000XM5" /></el-form-item>
        <el-form-item label="分类" prop="categoryId"><el-tree-select v-model="form.categoryId" :data="categoryOptions" check-strictly :props="{ label: 'name', value: 'id', children: 'children' }" placeholder="选择分类" /></el-form-item>
        <el-form-item label="品牌"><el-select v-model="form.brandId" clearable filterable><el-option v-for="b in brands" :key="b.id" :label="b.name" :value="b.id" /></el-select></el-form-item>
        <el-form-item label="型号"><el-input v-model="form.model" /></el-form-item>
        <el-form-item label="状态"><el-select v-model="form.status"><el-option v-for="s in statuses" :key="s" :label="s" :value="s" /></el-select></el-form-item>
        <el-form-item label="购买日期"><el-date-picker v-model="form.purchaseDate" value-format="YYYY-MM-DD" /></el-form-item>
      </div>
      <el-form-item label="物品封面">
        <div class="upload-row"><img v-if="form.coverImageUrl" :src="form.coverImageUrl" alt="物品封面预览" /><el-upload :show-file-list="false" :http-request="uploadCover" accept="image/*"><span class="secondary-button upload-trigger" :class="{ disabled: uploading }">{{ uploading ? '上传中' : '选择图片' }}</span></el-upload></div>
      </el-form-item>
      <el-form-item label="备注"><el-input v-model="form.notes" type="textarea" :rows="3" /></el-form-item>
      <section class="form-section">
        <div class="section-heading"><div><strong>购买记录</strong><span>主商品、配件和服务可一起录入</span></div><button type="button" class="text-button" @click="addPurchase">添加记录</button></div>
        <div v-for="(purchase, index) in form.purchases" :key="index" class="purchase-row">
          <el-select v-model="purchase.type"><el-option label="主商品" value="PRIMARY" /><el-option label="配件" value="ACCESSORY" /><el-option label="服务" value="SERVICE" /></el-select>
          <el-input v-model="purchase.name" placeholder="记录名称" />
          <el-input-number v-model="purchase.price" :min="0" :precision="2" controls-position="right" />
          <el-date-picker v-model="purchase.purchaseDate" value-format="YYYY-MM-DD" />
          <button type="button" class="text-button danger" @click="form.purchases.splice(index, 1)">移除</button>
        </div>
      </section>
    </el-form>
    <template #footer><button class="secondary-button" @click="workspace.closeAssetEditor()">取消</button><PrimaryButton label="保存物品" :loading="saving" @click="submit" /></template>
  </el-drawer>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage, type FormInstance, type FormRules, type UploadRequestOptions } from 'element-plus'
import { createAsset, updateAsset } from '@/api/assets'
import { fetchBrands, fetchCategories } from '@/api/settings'
import { uploadFile } from '@/api/files'
import { useWorkspaceStore } from '@/stores/workspace'
import type { AssetPayload, AssetStatus, BrandItem, CategoryNode, PurchaseRecord } from '@/types'
import PrimaryButton from './PrimaryButton.vue'

type AssetForm = Omit<AssetPayload, 'purchases'> & { purchases: PurchaseRecord[] }
const workspace = useWorkspaceStore(); const formRef = ref<FormInstance>(); const saving = ref(false); const uploading = ref(false)
const brands = ref<BrandItem[]>([]); const categoryOptions = ref<CategoryNode[]>([]); const statuses: AssetStatus[] = ['使用中', '已闲置', '待出售', '已出售', '已丢弃']
const blank = (): AssetForm => ({ name: '', categoryId: undefined as unknown as number, status: '使用中', purchases: [] })
const form = reactive<AssetForm>(blank())
const rules: FormRules = { name: [{ required: true, message: '请输入物品名称', trigger: 'blur' }], categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }] }

watch(() => workspace.assetEditorOpen, (open) => { if (open) Object.assign(form, blank(), workspace.editingAsset ? { ...workspace.editingAsset, brandId: workspace.editingAsset.brand?.id ?? undefined, purchases: workspace.editingAsset.purchases.map(p => ({ ...p })) } : {}) })
onMounted(async () => { const [categories, brandList] = await Promise.allSettled([fetchCategories(), fetchBrands()]); if (categories.status === 'fulfilled') categoryOptions.value = categories.value; if (brandList.status === 'fulfilled') brands.value = brandList.value })
const addPurchase = () => form.purchases.push({ type: 'PRIMARY', price: 0, purchaseDate: new Date().toISOString().slice(0, 10) } as PurchaseRecord)
const uploadCover = async (options: UploadRequestOptions) => { uploading.value = true; try { form.coverImageUrl = (await uploadFile(options.file)).url; options.onSuccess({}) } catch (e) { ElMessage.error((e as Error).message) } finally { uploading.value = false } }
const reset = () => { formRef.value?.resetFields(); Object.assign(form, blank()) }
const submit = async () => { if (!await formRef.value?.validate().catch(() => false)) return; saving.value = true; try { if (workspace.editingAsset) await updateAsset(workspace.editingAsset.id, form); else await createAsset(form); ElMessage.success('物品已保存'); workspace.closeAssetEditor(true) } catch (e) { ElMessage.error((e as Error).message) } finally { saving.value = false } }
</script>
