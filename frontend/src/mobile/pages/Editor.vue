<template>
  <div class="mobile-scroll">
    <header class="mobile-topbar">
      <div class="mobile-topbar-header">
        <button type="button" class="icon-btn" @click="goBack">←</button>
        <div class="tab-toggle">
          <button
            v-for="tab in tabs"
            :key="tab.value"
            type="button"
            :class="{ active: activeTab === tab.value }"
            @click="switchTab(tab.value)"
          >
            {{ tab.label }}
          </button>
        </div>
        <span></span>
      </div>
    </header>

    <form class="mobile-form" @submit.prevent="submit">
      <section v-if="activeTab === 'asset'">
        <div class="mobile-field">
          <label>物品名称 *</label>
          <input v-model="assetForm.name" type="text" placeholder="请输入物品名称" required />
        </div>
        <div class="mobile-field">
          <label>价格 (￥)</label>
          <input v-model.number="assetForm.price" type="number" min="0" step="0.01" placeholder="0.00" />
        </div>
        <div class="mobile-field">
          <label>购买日期</label>
          <input v-model="assetForm.purchaseDate" type="date" />
        </div>
        <div class="mobile-field"><label>型号</label><input v-model="assetForm.model" type="text" /></div>
        <div class="mobile-field"><label>序列号</label><input v-model="assetForm.serialNo" type="text" /></div>
        <div class="mobile-field"><label>配置规格</label><textarea v-model="assetForm.specifications" placeholder="CPU、内存、存储等"></textarea></div>
        <div class="mobile-field"><label>状态</label><select v-model="assetForm.status"><option v-for="status in editingId ? ['使用中', '已闲置', '待出售', '已出售', '已丢弃'] : ['使用中', '已闲置', '待出售']" :key="status" :value="status">{{ status }}</option></select></div>
        <div class="mobile-field"><label>品牌</label><select v-model="assetForm.brandId"><option value="">未选择</option><option v-for="brand in brandOptions" :key="brand.id" :value="brand.id">{{ brand.name }}</option></select></div>
        <div class="mobile-field"><label>购买平台</label><select v-model="assetForm.platformId"><option value="">未选择</option><option v-for="platform in platformOptions" :key="platform.id" :value="platform.id">{{ platform.name }}</option></select></div>
        <div class="mobile-field"><label>卖家 / 店铺</label><input v-model="assetForm.seller" type="text" /></div>
        <div class="mobile-field"><label>运费 (￥)</label><input v-model.number="assetForm.shippingCost" type="number" min="0" step="0.01" /></div>
        <div class="mobile-field"><label>数量</label><input v-model.number="assetForm.quantity" type="number" min="1" step="1" /></div>
        <div class="mobile-field"><label>质保月数</label><input v-model.number="assetForm.warrantyMonths" type="number" min="0" step="1" /></div>
        <div class="mobile-field"><label>质保到期</label><input v-model="assetForm.warrantyExpireDate" type="date" /></div>
        <div class="mobile-field"><label>购买链接</label><input v-model="assetForm.productLink" type="url" /></div>
        <div class="mobile-field"><label>购买备注</label><textarea v-model="assetForm.purchaseNotes"></textarea></div>
        <div class="mobile-field"><label>已使用月数</label><input v-model.number="assetForm.manualUseMonths" type="number" min="0" step="1" /></div>
        <div class="mobile-field"><label>停用日期</label><input v-model="assetForm.retiredDate" type="date" /></div>
        <div class="mobile-field">
          <label>类别 *</label>
          <select v-model="assetForm.categoryId">
            <option value="">请选择类别</option>
            <option v-for="item in categoryOptions" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </div>
        <div class="mobile-field">
          <label>标签</label>
          <select v-model="assetForm.tagIds" multiple>
            <option v-for="tag in tagOptions" :key="tag.id" :value="tag.id">{{ tag.name }}</option>
          </select>
        </div>
        <div class="mobile-field">
          <label>目标成本</label>
          <select v-model="assetForm.targetCostStrategy">
            <option value="NONE">不设定</option>
            <option value="PRICE">按照价格</option>
            <option value="DATE">按照日期</option>
            <option value="CUSTOM">自定义</option>
          </select>
          <input
            v-if="assetForm.targetCostStrategy === 'CUSTOM'"
            v-model.number="assetForm.targetCostValue"
            type="number"
            placeholder="请输入目标金额"
          />
        </div>
        <div class="mobile-field">
          <label>附加物品</label>
          <select v-model="assetForm.attachAssetIds" multiple>
            <option v-for="item in assetOptions" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </div>
        <div class="mobile-field">
          <label>备注</label>
          <textarea v-model="assetForm.notes" maxlength="200" placeholder="输入备注，最多 200 字"></textarea>
        </div>
        <div class="mobile-field">
          <label>物品封面</label>
          <MobileUploader v-model="assetForm.cover" :multiple="false" accept="image/*" />
        </div>
        <div class="mobile-field">
          <label>其他图片与附件</label>
          <MobileUploader v-model="assetForm.attachments" />
        </div>
      </section>

      <section v-else>
        <div class="mobile-field">
          <label>心愿名称 *</label>
          <input v-model="wishlistForm.name" type="text" placeholder="请输入心愿名称" required />
        </div>
        <div class="mobile-field">
          <label>心愿价格 (￥)</label>
          <input v-model.number="wishlistForm.price" type="number" min="0" step="0.01" placeholder="0.00" />
        </div>
        <div class="mobile-field">
          <label>当前关注价 (￥)</label>
          <input v-model.number="wishlistForm.currentPrice" type="number" min="0" step="0.01" placeholder="0.00" />
        </div>
        <div class="mobile-field"><label>分类</label><select v-model="wishlistForm.categoryId"><option value="">未分类</option><option v-for="item in categoryOptions" :key="item.id" :value="item.id">{{ item.name }}</option></select></div>
        <div class="mobile-field"><label>品牌</label><select v-model="wishlistForm.brandId"><option value="">未选择</option><option v-for="brand in brandOptions" :key="brand.id" :value="brand.id">{{ brand.name }}</option></select></div>
        <div class="mobile-field"><label>型号</label><input v-model="wishlistForm.model" type="text" /></div>
        <div class="mobile-field"><label>优先级</label><select v-model.number="wishlistForm.priority"><option v-for="level in [1, 2, 3, 4, 5]" :key="level" :value="level">{{ level }}</option></select></div>
        <div class="mobile-field"><label>来源</label><input v-model="wishlistForm.source" type="text" /></div>
        <div class="mobile-field"><label>商品链接</label><input v-model="wishlistForm.link" type="url" /></div>
        <div class="mobile-field"><label>标签</label><select v-model="wishlistForm.tagIds" multiple><option v-for="tag in tagOptions" :key="tag.id" :value="tag.id">{{ tag.name }}</option></select></div>
        <div class="mobile-field">
          <label>备注</label>
          <textarea v-model="wishlistForm.notes" maxlength="200" placeholder="输入备注信息"></textarea>
        </div>
        <div class="mobile-field">
          <label>图片</label>
          <MobileUploader v-model="wishlistForm.attachments" :multiple="false" accept="image/*" />
        </div>
      </section>
    </form>

    <div class="mobile-save-bar">
      <button type="button" class="mobile-save-button" @click="submit">保存</button>
    </div>

    <div v-if="toast" class="mobile-toast">{{ toast }}</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import MobileUploader, { type MobileAttachment } from '@/mobile/components/MobileUploader.vue'
import { fetchBrands, fetchCategoryTree, fetchPlatforms, fetchTagTree, type CategoryNode, type TagNode } from '@/api/dict'
import { createAsset, fetchAssets, fetchAssetDetail, updateAsset, type AssetPayload } from '@/api/asset'
import type { PurchaseRecord } from '@/types'
import { createWishlist, fetchWishlistDetail, updateWishlist } from '@/api/wishlist'

const route = useRoute()
const router = useRouter()

const tabs = [
  { label: '资产', value: 'asset' as const },
  { label: '心愿', value: 'wishlist' as const }
]

const activeTab = ref<'asset' | 'wishlist'>(route.query.type === 'wishlist' ? 'wishlist' : 'asset')
const editingId = ref<number | null>(route.query.id ? Number(route.query.id) : null)

const categoryOptions = ref<Array<{ id: number; name: string }>>([])
const tagOptions = ref<Array<{ id: number; name: string }>>([])
const brandOptions = ref<Array<{ id: number; name: string }>>([])
const platformOptions = ref<Array<{ id: number; name: string }>>([])
const assetOptions = ref<Array<{ id: number; name: string }>>([])
const toast = ref('')
const existingPurchases = ref<PurchaseRecord[]>([])

const assetForm = reactive({
  name: '',
  price: 0,
  purchaseDate: '',
  model: '',
  serialNo: '',
  specifications: '',
  status: '使用中',
  brandId: '' as number | '',
  platformId: '' as number | '',
  seller: '',
  shippingCost: 0,
  quantity: 1,
  warrantyMonths: undefined as number | undefined,
  warrantyExpireDate: '',
  productLink: '',
  purchaseNotes: '',
  manualUseMonths: undefined as number | undefined,
  retiredDate: '',
  categoryId: '' as number | '' ,
  tagIds: [] as number[],
  targetCostStrategy: 'NONE' as AssetPayload['targetCostStrategy'],
  targetCostValue: undefined as number | undefined,
  attachAssetIds: [] as number[],
  notes: '',
  cover: [] as MobileAttachment[],
  attachments: [] as MobileAttachment[]
})

const wishlistForm = reactive({
  name: '',
  price: 0,
  currentPrice: undefined as number | undefined,
  categoryId: '' as number | '',
  brandId: '' as number | '',
  model: '',
  priority: 3,
  source: '',
  link: '',
  tagIds: [] as number[],
  notes: '',
  attachments: [] as MobileAttachment[]
})

const resetForms = () => {
  assetForm.name = ''
  assetForm.price = 0
  assetForm.purchaseDate = ''
  assetForm.model = ''
  assetForm.serialNo = ''
  assetForm.specifications = ''
  assetForm.status = '使用中'
  assetForm.brandId = ''
  assetForm.platformId = ''
  assetForm.seller = ''
  assetForm.shippingCost = 0
  assetForm.quantity = 1
  assetForm.warrantyMonths = undefined
  assetForm.warrantyExpireDate = ''
  assetForm.productLink = ''
  assetForm.purchaseNotes = ''
  assetForm.manualUseMonths = undefined
  assetForm.retiredDate = ''
  assetForm.categoryId = ''
  assetForm.tagIds = []
  assetForm.targetCostStrategy = 'NONE'
  assetForm.targetCostValue = undefined
  assetForm.attachAssetIds = []
  assetForm.notes = ''
  assetForm.cover = []
  assetForm.attachments = []
  existingPurchases.value = []

  wishlistForm.name = ''
  wishlistForm.price = 0
  wishlistForm.currentPrice = undefined
  wishlistForm.categoryId = ''
  wishlistForm.brandId = ''
  wishlistForm.model = ''
  wishlistForm.priority = 3
  wishlistForm.source = ''
  wishlistForm.link = ''
  wishlistForm.tagIds = []
  wishlistForm.notes = ''
  wishlistForm.attachments = []
}

const goBack = () => router.back()
const switchTab = (value: 'asset' | 'wishlist') => {
  activeTab.value = value
}

const loadOptions = async () => {
  const [categoryRes, tagRes, assetRes, brandRes, platformRes] = await Promise.all([
    fetchCategoryTree(),
    fetchTagTree(),
    fetchAssets(),
    fetchBrands(),
    fetchPlatforms()
  ])
  categoryOptions.value = flattenCategories(categoryRes)
  tagOptions.value = flattenTags(tagRes)
  assetOptions.value = assetRes.map((item) => ({ id: item.id, name: item.name }))
  brandOptions.value = brandRes
  platformOptions.value = platformRes
}

const flattenCategories = (nodes: CategoryNode[]) => {
  const result: Array<{ id: number; name: string }> = []
  const traverse = (list: CategoryNode[], prefix = '') => {
    list.forEach((node) => {
      result.push({ id: node.id, name: prefix ? `${prefix} / ${node.name}` : node.name })
      if (node.children?.length) {
        traverse(node.children, prefix ? `${prefix} / ${node.name}` : node.name)
      }
    })
  }
  traverse(nodes)
  return result
}

const flattenTags = (nodes: TagNode[]) => {
  const result: Array<{ id: number; name: string }> = []
  const traverse = (list: TagNode[], prefix = '') => {
    list.forEach((node) => {
      result.push({ id: node.id, name: prefix ? `${prefix} / ${node.name}` : node.name })
      if (node.children?.length) {
        traverse(node.children, prefix ? `${prefix} / ${node.name}` : node.name)
      }
    })
  }
  traverse(nodes)
  return result
}

const loadEditingData = async () => {
  if (!editingId.value) return
  if (activeTab.value === 'asset') {
    const data = await fetchAssetDetail(editingId.value)
    existingPurchases.value = data.purchases || []
    const primary = data.purchases?.find((purchase) => purchase.type === 'PRIMARY')
    assetForm.name = data.name
    assetForm.price = primary?.price ?? 0
    assetForm.purchaseDate = primary?.purchaseDate || data.purchaseDate || ''
    assetForm.model = data.model || ''
    assetForm.serialNo = data.serialNo || ''
    assetForm.specifications = data.specifications || ''
    assetForm.status = data.status
    assetForm.brandId = data.brand?.id || ''
    assetForm.platformId = primary?.platformId || ''
    assetForm.seller = primary?.seller || ''
    assetForm.shippingCost = primary?.shippingCost || 0
    assetForm.quantity = primary?.quantity || 1
    assetForm.warrantyMonths = primary?.warrantyMonths
    assetForm.warrantyExpireDate = primary?.warrantyExpireDate || ''
    assetForm.productLink = primary?.productLink || ''
    assetForm.purchaseNotes = primary?.notes || ''
    assetForm.manualUseMonths = data.manualUseMonths
    assetForm.retiredDate = data.retiredDate || ''
    assetForm.categoryId = data.categoryId ?? ''
    assetForm.tagIds = data.tags?.map((tag) => tag.id) || []
    assetForm.notes = data.notes || ''
    assetForm.cover = data.coverImageUrl ? [{ name: data.name, url: data.coverImageUrl }] : []
    assetForm.attachments =
      primary?.attachments?.map((url, index) => ({
        name: `${data.name}-附件${index + 1}`,
        url
      })) || []
  } else {
    const data = await fetchWishlistDetail(editingId.value)
    wishlistForm.name = data.name
    wishlistForm.price = data.expectedPrice || 0
    wishlistForm.currentPrice = data.currentPrice
    wishlistForm.categoryId = data.categoryId || ''
    wishlistForm.brandId = data.brandId || ''
    wishlistForm.model = data.model || ''
    wishlistForm.priority = data.priority || 3
    wishlistForm.source = data.source || ''
    wishlistForm.link = data.link || ''
    wishlistForm.tagIds = data.tags?.map((tag) => tag.id) || []
    wishlistForm.notes = data.notes || ''
    wishlistForm.attachments = data.imageUrl
      ? [{ name: data.name, url: data.imageUrl }]
      : []
  }
}

watch(
  () => route.query,
  () => {
    activeTab.value = route.query.type === 'wishlist' ? 'wishlist' : 'asset'
    editingId.value = route.query.id ? Number(route.query.id) : null
    resetForms()
    loadEditingData()
  }
)

const validateAsset = () => {
  if (!assetForm.name.trim()) {
    toast.value = '请输入物品名称'
    setTimeout(() => (toast.value = ''), 1800)
    return false
  }
  if (!assetForm.categoryId) {
    toast.value = '请选择类别'
    setTimeout(() => (toast.value = ''), 1800)
    return false
  }
  if (assetForm.price < 0) {
    toast.value = '价格不能为负数'
    setTimeout(() => (toast.value = ''), 1800)
    return false
  }
  return true
}

const validateWishlist = () => {
  if (!wishlistForm.name.trim()) {
    toast.value = '请输入心愿名称'
    setTimeout(() => (toast.value = ''), 1800)
    return false
  }
  return true
}

const submit = async () => {
  toast.value = ''
  try {
    if (activeTab.value === 'asset') {
      if (!validateAsset()) return
      const payload: AssetPayload = {
        name: assetForm.name.trim(),
        categoryId: typeof assetForm.categoryId === 'number' ? assetForm.categoryId : 0,
        model: assetForm.model.trim() || undefined,
        serialNo: assetForm.serialNo.trim() || undefined,
        specifications: assetForm.specifications.trim(),
        brandId: typeof assetForm.brandId === 'number' ? assetForm.brandId : undefined,
        status: assetForm.status,
        purchaseDate: assetForm.purchaseDate || undefined,
        retiredDate: assetForm.retiredDate || undefined,
        manualUseMonths: assetForm.manualUseMonths,
        notes: assetForm.notes,
        tagIds: assetForm.tagIds,
        coverImageUrl: assetForm.cover[0]?.url,
        targetCostStrategy: assetForm.targetCostStrategy,
        targetCostValue: assetForm.targetCostValue,
        attachAssetIds: assetForm.attachAssetIds,
        purchases: [
          {
            ...existingPurchases.value.find((purchase) => purchase.type === 'PRIMARY'),
            type: 'PRIMARY' as const,
            platformId: typeof assetForm.platformId === 'number' ? assetForm.platformId : undefined,
            seller: assetForm.seller.trim() || undefined,
            price: assetForm.price,
            quantity: assetForm.quantity || 1,
            purchaseDate: assetForm.purchaseDate || new Date().toISOString().slice(0, 10),
            shippingCost: assetForm.shippingCost || 0,
            warrantyMonths: assetForm.warrantyMonths,
            warrantyExpireDate: assetForm.warrantyExpireDate || undefined,
            productLink: assetForm.productLink.trim() || undefined,
            notes: assetForm.purchaseNotes.trim() || undefined,
            attachments: assetForm.attachments.map((item) => item.url)
          },
          ...existingPurchases.value.filter((purchase) => purchase.type !== 'PRIMARY')
        ]
      }
      if (editingId.value) {
        await updateAsset(editingId.value, payload)
      } else {
        await createAsset(payload)
      }
      toast.value = '资产已保存'
    } else {
      if (!validateWishlist()) return
      const payload = {
        name: wishlistForm.name.trim(),
        expectedPrice: wishlistForm.price || undefined,
        currentPrice: wishlistForm.currentPrice,
        categoryId: typeof wishlistForm.categoryId === 'number' ? wishlistForm.categoryId : undefined,
        brandId: typeof wishlistForm.brandId === 'number' ? wishlistForm.brandId : undefined,
        model: wishlistForm.model.trim() || undefined,
        priority: wishlistForm.priority,
        source: wishlistForm.source.trim() || undefined,
        link: wishlistForm.link.trim() || undefined,
        tagIds: wishlistForm.tagIds,
        notes: wishlistForm.notes,
        imageUrl: wishlistForm.attachments[0]?.url
      }
      if (editingId.value) {
        await updateWishlist(editingId.value, payload)
      } else {
        await createWishlist(payload)
      }
      toast.value = '心愿已保存'
    }
    setTimeout(() => router.back(), 600)
  } catch (error) {
    toast.value = '保存失败，请检查表单后重试'
  } finally {
    setTimeout(() => (toast.value = ''), 2000)
  }
}

onMounted(async () => {
  await loadOptions()
  await loadEditingData()
})
</script>

<style scoped>
.tab-toggle {
  display: flex;
  background: rgba(255, 255, 255, 0.18);
  border-radius: 999px;
  padding: 4px;
  gap: 4px;
}

.tab-toggle button {
  flex: 1;
  border-radius: 999px;
  border: none;
  padding: 8px 16px;
  background: transparent;
  color: rgba(255, 255, 255, 0.7);
}

.tab-toggle button.active {
  background: #fff;
  color: var(--mobile-green-end);
}

select[multiple] {
  min-height: 100px;
}
</style>
