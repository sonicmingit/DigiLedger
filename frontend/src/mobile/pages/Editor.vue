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
        <div class="mobile-field">
          <label>类别</label>
          <select v-model="assetForm.categoryId">
            <option value="">未分类</option>
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
          <label>图片/附件</label>
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
          <label>关联资产</label>
          <select v-model="wishlistForm.attachAssetId">
            <option value="">不关联</option>
            <option v-for="item in assetOptions" :key="item.id" :value="item.id">{{ item.name }}</option>
          </select>
        </div>
        <div class="mobile-field">
          <label>备注</label>
          <textarea v-model="wishlistForm.notes" maxlength="200" placeholder="输入备注信息"></textarea>
        </div>
        <div class="mobile-field">
          <label>图片</label>
          <MobileUploader v-model="wishlistForm.attachments" :multiple="false" />
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
import { fetchCategoryTree, fetchTagTree, type CategoryNode, type TagNode } from '@/api/dict'
import { createAsset, fetchAssets, fetchAssetDetail, updateAsset, type AssetPayload } from '@/api/asset'
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
const assetOptions = ref<Array<{ id: number; name: string }>>([])
const toast = ref('')

const assetForm = reactive({
  name: '',
  price: 0,
  purchaseDate: '',
  categoryId: '' as number | '' ,
  tagIds: [] as number[],
  targetCostStrategy: 'NONE' as AssetPayload['targetCostStrategy'],
  targetCostValue: undefined as number | undefined,
  attachAssetIds: [] as number[],
  notes: '',
  attachments: [] as MobileAttachment[]
})

const wishlistForm = reactive({
  name: '',
  price: 0,
  attachAssetId: '' as number | '' ,
  notes: '',
  attachments: [] as MobileAttachment[]
})

const resetForms = () => {
  assetForm.name = ''
  assetForm.price = 0
  assetForm.purchaseDate = ''
  assetForm.categoryId = ''
  assetForm.tagIds = []
  assetForm.targetCostStrategy = 'NONE'
  assetForm.targetCostValue = undefined
  assetForm.attachAssetIds = []
  assetForm.notes = ''
  assetForm.attachments = []

  wishlistForm.name = ''
  wishlistForm.price = 0
  wishlistForm.attachAssetId = ''
  wishlistForm.notes = ''
  wishlistForm.attachments = []
}

const goBack = () => router.back()
const switchTab = (value: 'asset' | 'wishlist') => {
  activeTab.value = value
}

const loadOptions = async () => {
  const [categoryRes, tagRes, assetRes] = await Promise.all([
    fetchCategoryTree(),
    fetchTagTree(),
    fetchAssets()
  ])
  categoryOptions.value = flattenCategories(categoryRes)
  tagOptions.value = flattenTags(tagRes)
  assetOptions.value = assetRes.map((item) => ({ id: item.id, name: item.name }))
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
    assetForm.name = data.name
    assetForm.price = data.totalInvest
    assetForm.purchaseDate = data.purchaseDate || data.enabledDate
    assetForm.categoryId = data.categoryId ?? ''
    assetForm.tagIds = data.tags?.map((tag) => tag.id) || []
    assetForm.notes = data.notes || ''
    assetForm.attachments =
      data.purchases?.[0]?.attachments?.map((url, index) => ({
        name: `${data.name}-附件${index + 1}`,
        url
      })) || []
  } else {
    const data = await fetchWishlistDetail(editingId.value)
    wishlistForm.name = data.name
    wishlistForm.price = data.expectedPrice || 0
    wishlistForm.notes = data.notes || ''
    wishlistForm.attachAssetId = data.convertedAssetId ?? ''
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
        status: '使用中',
        purchaseDate: assetForm.purchaseDate || undefined,
        enabledDate: assetForm.purchaseDate || new Date().toISOString().slice(0, 10),
        notes: assetForm.notes,
        tagIds: assetForm.tagIds,
        coverImageUrl: assetForm.attachments[0]?.url,
        targetCostStrategy: assetForm.targetCostStrategy,
        targetCostValue: assetForm.targetCostValue,
        attachAssetIds: assetForm.attachAssetIds,
        purchases: assetForm.price
          ? [
              {
                type: 'PRIMARY' as const,
                price: assetForm.price,
                quantity: 1,
                purchaseDate: assetForm.purchaseDate || new Date().toISOString().slice(0, 10),
                shippingCost: 0,
                attachments: assetForm.attachments.map((item) => item.objectKey || item.url)
              }
            ]
          : undefined
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
        notes: wishlistForm.notes,
        imageUrl: wishlistForm.attachments[0]?.url,
        relatedAssetId: wishlistForm.attachAssetId || undefined
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
