<template>
  <div class="page detail-page">
    <PageHeader title="物品详情" subtitle="查看物品档案、使用成本与完整交易记录。">
      <button class="pill-button dark-button" :disabled="!asset" @click="edit">编辑物品</button>
      <button class="pill-button sale-wizard-button" :disabled="!canStartSale" :title="canStartSale ? '登记出售交易' : '物品已出售，不能再次创建出售记录'" @click="openCreateSale">出售向导</button>
      <PrimaryButton label="变更状态" :disabled="!asset" @click="statusDialog = true" />
    </PageHeader>

    <button class="back-button" @click="router.push('/assets')">← 返回物品中心</button>

    <AsyncState :loading="loading" :error="error" :empty="!asset" @retry="load">
      <template v-if="asset">
        <section class="detail-top">
          <div class="product-visual">
            <button v-if="asset.coverImageUrl" type="button" class="product-image-button" :aria-label="`放大查看 ${asset.name} 的完整图片`" @click="openCoverPreview">
              <img :src="asset.coverImageUrl" :alt="asset.name" />
              <span>点击查看原图</span>
            </button>
            <div v-else class="product-orbit"><strong>{{ initials(asset.name) }}</strong></div>
            <span class="tag lime">{{ asset.status }}</span>
          </div>

          <div class="detail-info">
            <div class="detail-title">
              <div>
                <h2>{{ asset.name }}</h2>
              </div>
              <strong>{{ money(asset.totalInvest) }}</strong>
            </div>

            <div class="category-line title-category-line">
              <span class="category-pill" :style="categoryStyle">{{ displayCategoryPath }}</span>
              <div class="tag-list" aria-label="物品标签">
                <span
                  v-for="tag in asset.tags || []"
                  :key="tag.id"
                  class="asset-tag"
                  :style="tagStyle(tag)"
                >
                  <span v-if="displayTagIcon(tag)" class="tag-icon">{{ displayTagIcon(tag) }}</span>{{ tag.name }}
                </span>
                <span v-if="!asset.tags?.length" class="asset-tag empty">未添加标签</span>
              </div>
            </div>

            <article class="card info-card">
              <h3>基础信息</h3>
              <dl>
                <div><dt>品牌</dt><dd>{{ brandLabel }}</dd></div>
                <div><dt>型号</dt><dd>{{ asset.model || '未填写' }}</dd></div>
                <div><dt>序列号</dt><dd>{{ asset.serialNo || '未填写' }}</dd></div>
                <div><dt>当前状态</dt><dd>{{ asset.status }}</dd></div>
                <div><dt>购买日期</dt><dd>{{ asset.purchaseDate || primaryPurchase?.purchaseDate || '未填写' }}</dd></div>
                <div><dt>购买平台</dt><dd>{{ primaryPurchase?.platformName || primaryPurchase?.seller || '未填写' }}</dd></div>
                <div><dt>保修到期</dt><dd>{{ primaryPurchase?.warrantyExpireDate || '未填写' }}</dd></div>
                <div><dt>购买数量</dt><dd>{{ primaryPurchase?.quantity || 1 }} 件</dd></div>
                <div><dt>手动使用时间</dt><dd>{{ manualUseDuration || '未填写' }}</dd></div>
                <div class="wide" v-if="displayRelatedLinks.length"><dt>相关链接</dt><dd class="detail-related-links"><a v-for="link in displayRelatedLinks" :key="`${link.url}-${link.description || ''}`" :href="link.url" target="_blank" rel="noreferrer">{{ link.description || link.url }}</a></dd></div>
                <div class="wide"><dt>备注</dt><dd>{{ asset.notes || '暂无备注。' }}</dd></div>
              </dl>
            </article>
          </div>
        </section>

        <section class="detail-metrics" aria-label="物品使用指标">
          <article class="card metric-block"><span>总投入</span><strong>{{ money(asset.totalInvest) }}</strong><small>包含主商品、配件与服务</small></article>
          <article class="card metric-block"><span>日均成本</span><strong>{{ money(asset.avgCostPerDay) }}</strong><small>按当前使用天数计算</small></article>
          <article class="card metric-block"><span>使用天数</span><strong>{{ safeNumber(asset.useDays) }} 天</strong><small>从首次购买日期起计算</small></article>
          <article class="card metric-block"><span>净收入</span><strong>{{ money(asset.lastNetIncome) }}</strong><small>最近一笔出售净收入</small></article>
        </section>

        <section class="card records-card purchase-records">
          <div class="card-heading">
            <div><h3>购买记录</h3><span>按购买时间正序排列</span></div>
            <button class="secondary-button" @click="openCreatePurchase">＋ 添加记录</button>
          </div>
          <div v-if="purchaseRecords.length" class="table-scroll">
            <table>
              <thead><tr><th>类型</th><th>名称</th><th>平台/卖家</th><th>价格</th><th>运费</th><th>数量</th><th>购买日期</th><th>质保到期</th><th>商品链接</th><th>附件</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="record in purchaseRecords" :key="record.id || `${record.type}-${record.purchaseDate}`">
                  <td><span class="record-tag" :class="{ primary: record.type === 'PRIMARY' }">{{ typeLabel(record.type) }}</span></td>
                  <td><strong>{{ record.type === 'PRIMARY' ? asset.name : record.name || '未命名记录' }}</strong></td>
                  <td>{{ record.platformName || record.seller || '—' }}</td>
                  <td><b>{{ money(record.price) }}</b></td>
                  <td>{{ money(record.shippingCost) }}</td>
                  <td>{{ record.quantity || 1 }}</td>
                  <td>{{ record.purchaseDate || '—' }}</td>
                  <td>{{ record.warrantyExpireDate || '—' }}</td>
                  <td><a v-if="record.productLink" :href="record.productLink" target="_blank" rel="noreferrer">查看商品</a><span v-else>—</span></td>
                  <td><button v-if="record.attachments?.length" class="text-button" @click="openAttachmentDialog(recordName(record), record.attachments)">附件 {{ record.attachments.length }}</button><span v-else>—</span></td>
                  <td><div class="row-actions"><button class="text-button" @click="openEditPurchase(record)">编辑</button><button class="text-button danger" @click="confirmDeletePurchase(record)">删除</button></div></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="inline-empty">暂无购买记录</div>
        </section>

        <section class="card records-card sale-records">
          <div class="card-heading">
            <div><h3>出售记录</h3><span>按出售时间正序排列</span></div>
            <button class="secondary-button" :disabled="!canStartSale" :title="canStartSale ? '登记出售交易' : '物品已出售，不能再次创建出售记录'" @click="openCreateSale">＋ 出售向导</button>
          </div>
          <div v-if="saleRecords.length" class="table-scroll">
            <table>
              <thead><tr><th>类别</th><th>名称</th><th>出售日期</th><th>平台</th><th>买家</th><th>售价</th><th>费用</th><th>净收入</th><th>使用天数</th><th>成本指标</th><th>附件</th><th>操作</th></tr></thead>
              <tbody>
                <tr v-for="record in saleRecords" :key="record.id">
                  <td><span class="record-tag sale" :class="record.saleScope === 'ASSET' ? 'sale-primary' : 'sale-accessory'">{{ record.saleScope === 'ASSET' ? '主商品' : '配件' }}</span></td>
                  <td><strong>{{ saleRecordName(record) }}</strong></td>
                  <td>{{ record.saleDate || '—' }}</td>
                  <td>{{ record.platformName || '—' }}</td>
                  <td>{{ record.buyer || '—' }}</td>
                  <td><b>{{ money(record.salePrice) }}</b></td>
                  <td>{{ money(safeNumber(record.fee) + safeNumber(record.shippingCost) + safeNumber(record.otherCost)) }}</td>
                  <td><strong class="income">{{ money(record.netIncome) }}</strong></td>
                  <td>{{ safeNumber(record.useDays) }} 天</td>
                  <td><div class="cost-metrics"><strong :class="lossClass(record.lossAmount)">{{ lossText(record.lossAmount) }}</strong><span>日均 {{ money(record.dailyUsageCost) }}</span><span>月均 {{ money(record.monthlyUsageCost) }}</span></div></td>
                  <td><button v-if="record.attachments?.length" class="text-button" @click="openAttachmentDialog(`出售记录 · ${asset.name}`, record.attachments)">附件 {{ record.attachments.length }}</button><span v-else>—</span></td>
                  <td><div class="row-actions"><button class="text-button" @click="openEditSale(record)">编辑</button><button class="text-button danger" @click="confirmDeleteSale(record)">删除</button></div></td>
                </tr>
              </tbody>
            </table>
          </div>
          <div v-else class="inline-empty">暂无出售记录</div>
        </section>
      </template>
    </AsyncState>

    <el-dialog v-model="coverPreviewOpen" :title="asset ? `${asset.name} · 物品图片` : '物品图片'" width="min(960px, calc(100vw - 32px))" class="cover-image-dialog" align-center destroy-on-close>
      <div class="cover-image-stage">
        <img v-if="asset?.coverImageUrl" :src="asset.coverImageUrl" :alt="`${asset.name} 完整图片`" />
      </div>
    </el-dialog>

    <el-dialog v-model="statusDialog" title="变更物品状态" width="430px">
      <el-form label-position="top" class="dialog-form"><el-form-item label="新状态"><el-select v-model="nextStatus"><el-option v-for="s in statuses" :key="s" :label="s" :value="s" /></el-select></el-form-item></el-form>
      <template #footer><div class="dialog-footer-actions"><button class="secondary-button" @click="statusDialog=false">取消</button><PrimaryButton label="确认变更" :loading="saving" @click="saveStatus" /></div></template>
    </el-dialog>

    <el-dialog v-model="purchaseDialog" :title="purchase.id ? '编辑购买记录' : '添加购买记录'" width="720px">
      <el-form label-position="top" class="dialog-form">
        <div class="dialog-grid">
          <el-form-item label="类型"><el-select v-model="purchase.type"><el-option label="主商品" value="PRIMARY" /><el-option label="配件" value="ACCESSORY" /><el-option label="服务" value="SERVICE" /></el-select></el-form-item>
          <el-form-item label="名称"><el-input v-model="purchase.name" :placeholder="purchase.type === 'PRIMARY' ? asset?.name : '配件或服务名称'" /></el-form-item>
          <el-form-item label="平台"><el-select v-model="purchase.platformId" clearable placeholder="请选择平台"><el-option v-for="p in platforms" :key="p.id" :label="p.name" :value="p.id" /></el-select></el-form-item>
          <el-form-item label="卖家"><el-input v-model="purchase.seller" /></el-form-item>
          <el-form-item label="金额"><el-input-number v-model="purchase.price" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="运费"><el-input-number v-model="purchase.shippingCost" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="数量"><el-input-number v-model="purchase.quantity" :min="1" :precision="0" /></el-form-item>
          <el-form-item label="购买日期"><el-date-picker v-model="purchase.purchaseDate" value-format="YYYY-MM-DD" placeholder="请选择购买日期" :shortcuts="dateShortcuts" /></el-form-item>
          <el-form-item label="质保月数"><el-input-number v-model="purchase.warrantyMonths" :min="0" :precision="0" /></el-form-item>
          <el-form-item label="质保到期"><el-date-picker v-model="purchase.warrantyExpireDate" value-format="YYYY-MM-DD" clearable placeholder="请选择质保到期日" :shortcuts="dateShortcuts" /></el-form-item>
        </div>
        <el-form-item label="商品链接"><el-input v-model="purchase.productLink" /></el-form-item>
        <el-form-item label="备注"><el-input v-model="purchase.notes" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="附件">
          <div class="attachment-editor">
            <AttachmentDropzone compact @files="uploadPurchaseAttachments" />
            <span v-for="(item, index) in purchase.attachments || []" :key="`${item}-${index}`" class="attachment-chip">
              附件 {{ index + 1 }}
              <button type="button" @click="removeAttachment(purchase, item)">×</button>
            </span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer-actions"><button class="secondary-button" @click="purchaseDialog=false">取消</button><PrimaryButton label="保存记录" :loading="saving" @click="savePurchase" /></div></template>
    </el-dialog>

    <el-dialog v-model="sellDialog" :title="sale.id ? '编辑出售记录' : '出售向导'" width="720px">
      <el-form label-position="top" class="dialog-form">
        <div class="dialog-grid">
          <el-form-item label="出售范围"><el-select v-model="sale.saleScope"><el-option label="主商品" value="ASSET" /><el-option label="配件" value="ACCESSORY" /></el-select></el-form-item>
          <el-form-item v-if="sale.saleScope === 'ACCESSORY'" label="关联配件"><el-select v-model="sale.purchaseId" clearable><el-option v-for="p in accessoryPurchases" :key="p.id" :label="recordName(p)" :value="p.id" /></el-select></el-form-item>
          <el-form-item label="平台"><el-select v-model="sale.platformId" clearable placeholder="请选择平台"><el-option v-for="p in platforms" :key="p.id" :label="p.name" :value="p.id" /></el-select></el-form-item>
          <el-form-item label="买家"><el-input v-model="sale.buyer" /></el-form-item>
          <el-form-item label="出售金额"><el-input-number v-model="sale.salePrice" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="出售日期"><el-date-picker v-model="sale.saleDate" value-format="YYYY-MM-DD" placeholder="请选择出售日期" :shortcuts="dateShortcuts" /></el-form-item>
          <el-form-item label="手续费"><el-input-number v-model="sale.fee" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="运费"><el-input-number v-model="sale.shippingCost" :min="0" :precision="2" /></el-form-item>
          <el-form-item label="其他费用"><el-input-number v-model="sale.otherCost" :min="0" :precision="2" /></el-form-item>
        </div>
        <el-form-item label="备注"><el-input v-model="sale.notes" type="textarea" :rows="2" /></el-form-item>
        <el-form-item label="附件">
          <div class="attachment-editor">
            <AttachmentDropzone compact @files="uploadSaleAttachments" />
            <span v-for="(item, index) in sale.attachments || []" :key="`${item}-${index}`" class="attachment-chip">
              附件 {{ index + 1 }}
              <button type="button" @click="removeAttachment(sale, item)">×</button>
            </span>
          </div>
        </el-form-item>
      </el-form>
      <template #footer><div class="dialog-footer-actions"><button class="secondary-button" @click="sellDialog=false">取消</button><PrimaryButton label="保存出售" :loading="saving" @click="saveSale" /></div></template>
    </el-dialog>

    <el-dialog v-model="attachmentDialog.visible" :title="attachmentDialog.title" width="760px" @closed="resetAttachmentDialog">
      <div v-if="attachmentDialog.items.length" class="attachment-viewer">
        <aside class="attachment-list">
          <button
            v-for="(item, index) in attachmentDialog.items"
            :key="`${item}-${index}`"
            class="attachment-list-item"
            :class="{ active: index === attachmentDialog.activeIndex }"
            @click="attachmentDialog.activeIndex = index"
          >
            <span>{{ isImageAttachment(item) ? '图片' : '文件' }}</span>
            <strong>附件 {{ index + 1 }}</strong>
          </button>
        </aside>
        <div class="attachment-preview">
          <el-image
            v-if="currentAttachment && isImageAttachment(currentAttachment)"
            :src="currentAttachmentUrl"
            :preview-src-list="attachmentPreviewList"
            fit="contain"
            class="preview-image"
          />
          <iframe v-else-if="currentAttachmentUrl" :src="currentAttachmentUrl" class="preview-frame" title="附件预览" />
          <div v-else class="inline-empty">暂无可预览附件</div>
        </div>
      </div>
      <div v-else class="inline-empty">暂无附件</div>
    </el-dialog>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  createPurchase,
  deletePurchase,
  deleteSale,
  fetchAsset,
  sellAsset,
  updateAssetStatus,
  updatePurchase,
  updateSale,
  type SellPayload
} from '@/api/assets'
import { uploadFile } from '@/api/files'
import { fetchCategories, fetchPlatforms } from '@/api/settings'
import type { AssetDetail, AssetStatus, CategoryNode, PlatformItem, PurchaseRecord, SaleRecord, TagItem } from '@/types'
import { useWorkspaceStore } from '@/stores/workspace'
import PageHeader from '@/components/PageHeader.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import AttachmentDropzone from '@/components/AttachmentDropzone.vue'

type EditablePurchase = PurchaseRecord & { assetId?: number }
type EditableSale = SellPayload & { id?: number; attachments: string[] }

const route = useRoute()
const router = useRouter()
const workspace = useWorkspaceStore()
const asset = ref<AssetDetail>()
const categories = ref<CategoryNode[]>([])
const platforms = ref<PlatformItem[]>([])
const loading = ref(true)
const error = ref('')
const saving = ref(false)
const statusDialog = ref(false)
const coverPreviewOpen = ref(false)
const purchaseDialog = ref(false)
const sellDialog = ref(false)
const statuses: AssetStatus[] = ['使用中', '已闲置', '待出售', '已出售', '已丢弃']
const nextStatus = ref<AssetStatus>('使用中')
const today = () => new Date().toISOString().slice(0, 10)
const dateShortcuts = [{ text: '今天', value: () => new Date() }]
const blankPurchase = (): EditablePurchase => ({ type: 'ACCESSORY', name: '', price: 0, shippingCost: 0, quantity: 1, purchaseDate: today(), warrantyMonths: 12, attachments: [] })
const blankSale = (): EditableSale => ({ saleScope: 'ASSET', salePrice: 0, saleDate: today(), buyer: '', fee: 0, shippingCost: 0, otherCost: 0, attachments: [], notes: '' })
const purchase = reactive<EditablePurchase>(blankPurchase())
const sale = reactive<EditableSale>(blankSale())

const primaryPurchase = computed(() => asset.value?.purchases?.find(record => record.type === 'PRIMARY'))
const displayRelatedLinks = computed(() => [
  ...(primaryPurchase.value?.productLink ? [{ url: primaryPurchase.value.productLink, description: '购买链接' }] : []),
  ...(asset.value?.relatedLinks || [])
])
const manualUseDuration = computed(() => {
  const total = asset.value?.manualUseMonths || 0
  if (!total) return ''
  const years = Math.floor(total / 12)
  const months = total % 12
  return `${years ? `${years} 年` : ''}${months ? `${months} 个月` : ''}`
})
const canStartSale = computed(() => Boolean(asset.value && asset.value.status !== '已出售'))
const purchaseRecords = computed(() => [...(asset.value?.purchases || [])].sort((a, b) => dateValue(a.purchaseDate) - dateValue(b.purchaseDate)))
const saleRecords = computed(() => [...(asset.value?.sales || [])].sort((a, b) => dateValue(a.saleDate) - dateValue(b.saleDate)))
const accessoryPurchases = computed(() => purchaseRecords.value.filter(record => record.type === 'ACCESSORY' && record.id))
const brandLabel = computed(() => asset.value?.brand?.alias || asset.value?.brand?.name || asset.value?.brandName || '未填写')

const findCategoryPath = (nodes: CategoryNode[], id: number, parents: string[] = []): string[] | undefined => {
  for (const node of nodes) {
    const current = [...parents, node.name]
    if (node.id === id) return current
    const childPath = findCategoryPath(node.children || [], id, current)
    if (childPath) return childPath
  }
}
const displayCategoryPath = computed(() => {
  const resolved = asset.value?.categoryId ? findCategoryPath(categories.value, asset.value.categoryId) : undefined
  return resolved?.join(' / ') || asset.value?.categoryPath || '未分类'
})
const categoryStyle = computed(() => colorStyle(seedColor(asset.value?.categoryId || displayCategoryPath.value.length)))

const safeNumber = (value: unknown) => { const parsed = Number(value); return Number.isFinite(parsed) ? parsed : 0 }
const money = (value: unknown = 0) => `¥ ${safeNumber(value).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const initials = (name: string) => name.slice(0, 2)
const typeLabel = (type: string) => ({ PRIMARY: '主商品', ACCESSORY: '配件', SERVICE: '服务' }[type] || type)
const dateValue = (date?: string) => date ? new Date(date).getTime() : 0
const recordName = (record: PurchaseRecord) => record.type === 'PRIMARY' ? asset.value?.name || '主商品' : record.name || '未命名记录'
const saleRecordName = (record: SaleRecord) => {
  if (record.saleScope === 'ASSET') return asset.value?.name || '主商品'
  return asset.value?.purchases?.find(purchase => purchase.id === record.purchaseId)?.name || '未命名配件'
}
const lossText = (value: unknown) => { const amount = safeNumber(value); return amount > 0 ? `亏损 ${money(amount)}` : amount < 0 ? `收益 ${money(Math.abs(amount))}` : '持平' }
const lossClass = (value: unknown) => safeNumber(value) > 0 ? 'loss' : safeNumber(value) < 0 ? 'gain' : 'neutral'

const palette = ['#b7ff3c', '#8be9d1', '#ffd166', '#b8c7ff', '#ffb7d5', '#c8f7a1']
function seedColor(seed: number) { return palette[Math.abs(seed) % palette.length] }
function colorStyle(color: string) {
  return { backgroundColor: `${color}55`, borderColor: color, color: '#10140f' }
}
function tagStyle(tag: TagItem) {
  return colorStyle(tag.color || seedColor(tag.id))
}
function displayTagIcon(tag: TagItem) {
  const icon = tag.icon?.trim()
  if (!icon || /^(mdi|el|lucide)-/i.test(icon)) return ''
  return icon
}

async function load() {
  loading.value = true
  error.value = ''
  try {
    const [detail, dictionary, platformList] = await Promise.all([
      fetchAsset(Number(route.params.id)),
      fetchCategories().catch(() => [] as CategoryNode[]),
      fetchPlatforms().catch(() => [] as PlatformItem[])
    ])
    asset.value = detail
    categories.value = dictionary
    platforms.value = platformList
    nextStatus.value = asset.value.status
  } catch (e) {
    error.value = (e as Error).message
  } finally {
    loading.value = false
  }
}

function edit() { if (asset.value) workspace.openAssetEditor(asset.value) }
function openCoverPreview() { if (asset.value?.coverImageUrl) coverPreviewOpen.value = true }
async function execute(task: () => Promise<unknown>, done: () => void, message = '操作已保存') {
  saving.value = true
  try {
    await task()
    ElMessage.success(message)
    done()
    await load()
  } catch (e) {
    ElMessage.error((e as Error).message)
  } finally {
    saving.value = false
  }
}
const saveStatus = () => asset.value && execute(() => updateAssetStatus(asset.value!.id, nextStatus.value), () => { statusDialog.value = false })

function openCreatePurchase() {
  Object.assign(purchase, blankPurchase())
  purchaseDialog.value = true
}
function openEditPurchase(record: PurchaseRecord) {
  Object.assign(purchase, blankPurchase(), { ...record, attachments: [...(record.attachments || [])] })
  purchaseDialog.value = true
}
function purchasePayload() {
  return {
    ...purchase,
    assetId: asset.value!.id,
    name: purchase.type === 'PRIMARY' ? undefined : purchase.name || undefined,
    attachments: [...(purchase.attachments || [])]
  }
}
const savePurchase = () => {
  if (!asset.value) return
  const payload = purchasePayload()
  if (!payload.price && payload.price !== 0) {
    ElMessage.warning('请填写购买金额')
    return
  }
  const task = purchase.id ? () => updatePurchase(purchase.id!, payload) : () => createPurchase(payload)
  return execute(task, () => { purchaseDialog.value = false }, '购买记录已保存')
}
async function confirmDeletePurchase(record: PurchaseRecord) {
  if (!record.id) return
  await ElMessageBox.confirm('确认删除该购买记录？删除后会重新计算物品投入与成本。', '删除购买记录', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
  await execute(() => deletePurchase(record.id!), () => undefined, '购买记录已删除')
}

function openCreateSale() {
  if (!canStartSale.value) {
    ElMessage.warning('该物品已出售，不能再次创建出售记录')
    return
  }
  Object.assign(sale, blankSale())
  sellDialog.value = true
}
function openEditSale(record: SaleRecord) {
  Object.assign(sale, blankSale(), { ...record, attachments: [...(record.attachments || [])] })
  sellDialog.value = true
}
function salePayload(): SellPayload {
  return {
    platformId: sale.platformId,
    saleScope: sale.saleScope,
    purchaseId: sale.saleScope === 'ACCESSORY' ? sale.purchaseId : undefined,
    buyer: sale.buyer || undefined,
    salePrice: sale.salePrice,
    fee: sale.fee || 0,
    shippingCost: sale.shippingCost || 0,
    otherCost: sale.otherCost || 0,
    saleDate: sale.saleDate,
    attachments: [...(sale.attachments || [])],
    notes: sale.notes || undefined
  }
}
const saveSale = () => {
  if (!asset.value) return
  if (sale.saleScope === 'ACCESSORY' && !sale.purchaseId) {
    ElMessage.warning('请选择要出售的配件')
    return
  }
  const payload = salePayload()
  const task = sale.id ? () => updateSale(asset.value!.id, sale.id!, payload) : () => sellAsset(asset.value!.id, payload)
  return execute(task, () => { sellDialog.value = false }, '出售记录已保存')
}
async function confirmDeleteSale(record: SaleRecord) {
  if (!asset.value) return
  await ElMessageBox.confirm('确认删除该出售记录？删除后会恢复相关状态并重新计算成本。', '删除出售记录', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' })
  await execute(() => deleteSale(asset.value!.id, record.id), () => undefined, '出售记录已删除')
}

async function uploadAttachments(files: File[], target: { attachments?: string[] }) {
  try {
    if (!target.attachments) target.attachments = []
    const results = await Promise.all(files.map(uploadFile))
    target.attachments.push(...results.map(result => result.url))
    ElMessage.success(`已上传 ${results.length} 个附件`)
  } catch (e) {
    ElMessage.error((e as Error).message)
  }
}
const uploadPurchaseAttachments = (files: File[]) => uploadAttachments(files, purchase)
const uploadSaleAttachments = (files: File[]) => uploadAttachments(files, sale)
function removeAttachment(target: { attachments?: string[] }, item: string) {
  target.attachments = (target.attachments || []).filter(value => value !== item)
}

const attachmentDialog = reactive({ visible: false, title: '', items: [] as string[], activeIndex: 0 })
const currentAttachment = computed(() => attachmentDialog.items[attachmentDialog.activeIndex])
const currentAttachmentUrl = computed(() => currentAttachment.value || '')
const attachmentPreviewList = computed(() => attachmentDialog.items.filter(isImageAttachment))
function isImageAttachment(value: string) {
  const path = (value || '').split('?')[0].toLowerCase()
  return /\.(png|jpe?g|gif|bmp|webp|svg)$/.test(path)
}
function openAttachmentDialog(title: string, attachments?: string[]) {
  if (!attachments?.length) {
    ElMessage.info('暂无附件可查看')
    return
  }
  attachmentDialog.title = title
  attachmentDialog.items = [...attachments]
  attachmentDialog.activeIndex = 0
  attachmentDialog.visible = true
}
function resetAttachmentDialog() {
  attachmentDialog.items = []
  attachmentDialog.activeIndex = 0
}

watch(() => workspace.refreshKey, load)
watch(() => sale.saleScope, scope => {
  if (scope === 'ASSET') sale.purchaseId = undefined
  if (scope === 'ACCESSORY' && !sale.purchaseId) sale.purchaseId = accessoryPurchases.value[0]?.id
})
onMounted(load)
</script>

<style scoped>
.back-button{margin:-12px 0 20px;padding:0;border:0;background:none;color:var(--dl-text-secondary);font-size:12px;font-weight:600;cursor:pointer}.detail-top{display:grid;grid-template-columns:470px 1fr;gap:40px}.product-visual{position:relative;height:410px;display:grid;place-items:center;overflow:hidden;border-radius:var(--dl-radius-lg);background:#f5f6f2;box-shadow:var(--dl-shadow)}.product-image-button{position:relative;width:100%;height:100%;padding:12px;border:0;background:transparent;cursor:zoom-in}.product-image-button img{display:block;width:100%;height:100%;object-fit:contain;transition:transform .2s ease}.product-image-button:hover img{transform:scale(1.018)}.product-image-button span{position:absolute;right:24px;bottom:20px;padding:6px 10px;border-radius:999px;background:rgba(15,20,15,.76);color:#fff;font-size:11px;font-weight:700;opacity:0;transition:opacity .18s}.product-image-button:hover span,.product-image-button:focus-visible span{opacity:1}.product-image-button:focus-visible{outline:2px solid var(--dl-accent);outline-offset:-4px}.product-visual>.tag{z-index:1;position:absolute;top:24px;right:30px;pointer-events:none}.product-orbit{width:270px;height:270px;display:grid;place-items:center;border-radius:50%;background:rgba(255,255,255,.72);box-shadow:inset 0 0 0 30px rgba(183,255,60,.4)}.product-orbit strong{font-size:70px}.cover-image-stage{display:grid;place-items:center;min-height:300px;max-height:74vh;overflow:auto;border-radius:18px;background:#f5f6f2}.cover-image-stage img{display:block;max-width:100%;max-height:74vh;object-fit:contain}.detail-info{min-width:0}.detail-title{height:88px;display:flex;align-items:flex-start;justify-content:space-between;padding-top:4px}.detail-title h2{margin:0;font-size:30px}.detail-title p{margin:8px 0;color:var(--dl-text-secondary);font-size:13px}.detail-title>strong{font-size:22px}.info-card{min-height:322px;padding:19px 22px}.info-card h3,.records-card h3{margin:0;font-size:17px}.category-line{display:flex;align-items:center;justify-content:space-between;gap:18px;margin-top:18px;padding:13px 14px;border-radius:22px;background:#f5f6f2}.category-pill,.asset-tag{display:inline-flex;min-height:28px;align-items:center;border:1px solid transparent;border-radius:999px;padding:0 12px;font-size:11px;font-weight:700;white-space:nowrap}.tag-list{min-width:0;display:flex;justify-content:flex-end;gap:8px;overflow:hidden}.asset-tag.empty{background:#eceee9;color:var(--dl-muted)}.tag-icon{margin-right:5px}.info-card dl{display:grid;grid-template-columns:1fr 1fr;gap:13px 42px;margin-top:18px}.info-card dl div{min-width:0;display:flex;flex-direction:column;gap:5px}.info-card dl .wide{grid-column:1/-1}.info-card dt{color:var(--dl-text-secondary);font-size:10px}.info-card dd{margin:0;overflow:hidden;color:var(--dl-text);font-size:12px;font-weight:600;text-overflow:ellipsis;white-space:nowrap}.info-card .wide dd{white-space:normal;line-height:1.7}.detail-related-links{display:flex;flex-wrap:wrap;gap:7px}.detail-related-links a{display:inline-flex;align-items:center;min-height:26px;padding:0 9px;border-radius:999px;background:#edf5e3;color:#45660c;font-size:10px;line-height:1.2;text-decoration:none}.detail-related-links a:hover{background:#e2f5ca;text-decoration:none}.detail-metrics{display:grid;grid-template-columns:repeat(4,1fr);gap:18px;margin-top:28px}.metric-block{min-height:116px;padding:17px 19px}.metric-block span{color:var(--dl-text-secondary);font-size:12px}.metric-block strong{display:block;margin-top:8px;font-size:23px}.metric-block small{display:block;margin-top:7px;color:var(--dl-muted);font-size:10px}.records-card{margin-top:24px;padding:20px 22px}.card-heading{display:flex;justify-content:space-between;align-items:center}.card-heading>div{display:flex;flex-direction:column;gap:5px}.card-heading span{color:var(--dl-muted);font-size:10px}.table-scroll{margin-top:18px;overflow-x:auto}.records-card table{width:100%;min-width:920px;border-collapse:collapse;font-size:11px}.sale-records table{min-width:960px}.records-card th{padding:10px 8px;border-bottom:1px solid #dfe2db;color:var(--dl-text-secondary);font-size:10px;font-weight:600;text-align:left;white-space:nowrap}.records-card td{padding:13px 8px;border-bottom:1px solid #eceee9;color:var(--dl-text-secondary);vertical-align:middle;white-space:nowrap}.records-card tbody tr:last-child td{border-bottom:0}.records-card td strong,.records-card td b{color:var(--dl-text)}.records-card a{color:var(--dl-text);font-weight:600;text-decoration:none}.records-card a:hover{text-decoration:underline}.record-tag{display:inline-flex;min-height:25px;align-items:center;padding:0 9px;border-radius:999px;background:var(--dl-bg-alt);color:var(--dl-text-secondary);font-size:10px;font-weight:600}.record-tag.primary{background:var(--dl-accent-soft);color:var(--dl-text)}.record-tag.sale{background:#fff1df;color:#a96a00}.income{color:var(--dl-success)!important}.cost-metrics{display:flex;flex-direction:column;gap:3px}.cost-metrics span{color:var(--dl-muted);font-size:9px}.cost-metrics .loss{color:var(--dl-danger)}.cost-metrics .gain{color:var(--dl-success)}.cost-metrics .neutral{color:var(--dl-text-secondary)}.row-actions{display:flex;align-items:center;gap:8px}.inline-empty{height:88px;display:grid;place-items:center;color:var(--dl-muted);font-size:12px}.dialog-grid{display:grid;grid-template-columns:1fr 1fr;gap:0 16px}.dialog-form :deep(.el-input-number),.dialog-form :deep(.el-date-editor),.dialog-form :deep(.el-select){width:100%}.attachment-editor{display:flex;flex-wrap:wrap;align-items:center;gap:10px}.attachment-chip{display:inline-flex;align-items:center;gap:8px;min-height:32px;padding:0 10px;border-radius:999px;background:#f1f3ee;color:var(--dl-text-secondary);font-size:11px;font-weight:700}.attachment-chip button{width:18px;height:18px;border:0;border-radius:50%;background:#fff;color:var(--dl-danger);cursor:pointer}.attachment-viewer{display:grid;grid-template-columns:170px 1fr;gap:18px;min-height:430px}.attachment-list{display:flex;flex-direction:column;gap:8px;max-height:430px;overflow:auto}.attachment-list-item{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:12px;border:1px solid #e1e4dd;border-radius:18px;background:#f7f8f5;color:var(--dl-text-secondary);cursor:pointer;text-align:left}.attachment-list-item.active{border-color:var(--dl-accent);background:var(--dl-accent-soft);color:var(--dl-text)}.attachment-list-item span{font-size:10px}.attachment-list-item strong{font-size:12px}.attachment-preview{min-width:0;height:430px;display:grid;place-items:center;overflow:hidden;border-radius:24px;background:#f5f6f2}.preview-image{width:100%;height:100%;display:grid;place-items:center}.preview-image :deep(img){max-width:100%;max-height:430px;object-fit:contain}.preview-frame{width:100%;height:100%;border:0;background:#fff}
.detail-title{height:54px}.title-category-line{margin:0 0 16px}.info-card{min-height:0}.sale-records table{min-width:900px;font-size:10px}.sale-records th,.sale-records td{padding-left:5px;padding-right:5px}.record-tag.sale-primary{background:var(--dl-accent-soft);color:#4b6d09}.record-tag.sale-accessory{background:#fff1df;color:#a96a00}.sale-wizard-button{background:#0f1410;color:#fff}.sale-wizard-button:hover:not(:disabled){background:#2b3528}.dialog-footer-actions{display:flex;justify-content:flex-end;gap:10px}.dialog-footer-actions :deep(button){display:inline-flex;justify-content:center;align-items:center;box-sizing:border-box;min-width:94px;height:36px;padding:0 14px;font-size:12px}.attachment-editor{align-items:stretch}.attachment-editor :deep(.attachment-dropzone){flex:1 1 100%}
@media (max-width:1100px){.detail-top{grid-template-columns:1fr}.product-visual{height:360px}.detail-metrics{grid-template-columns:repeat(2,1fr)}}@media (max-width:760px){.detail-metrics,.info-card dl,.dialog-grid,.attachment-viewer{grid-template-columns:1fr}.category-line{align-items:flex-start;flex-direction:column}.tag-list{justify-content:flex-start;flex-wrap:wrap}.attachment-preview{height:320px}.preview-image :deep(img){max-height:320px}}
</style>
