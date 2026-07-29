<template>
  <div class="page assets-page">
    <PageHeader title="物品中心" subtitle="集中检索、筛选和维护全部物品。">
      <PrimaryButton label="新增物品" :icon="plusIcon" @click="workspace.openNewAsset" />
    </PageHeader>

    <div class="toolbar">
      <label class="search-box"><img :src="searchIcon" alt="" /><input v-model.trim="keyword" placeholder="搜索名称、品牌、标签或备注" @keyup.enter="load" /></label>
      <el-select v-model="status" class="dl-select" clearable placeholder="全部状态" @change="applyFilters"><el-option v-for="item in statuses" :key="item" :label="item" :value="item" /></el-select>
      <el-cascader v-model="categoryPath" class="dl-select dl-cascader" clearable :options="categories" :props="categoryProps" placeholder="全部分类" @change="applyFilters" />
      <el-select v-model="brandId" class="dl-select" clearable filterable placeholder="全部品牌" @change="applyFilters"><el-option v-for="item in brands" :key="item.id" :label="item.name" :value="item.id" /></el-select>
      <el-select v-model="platformId" class="dl-select" clearable filterable placeholder="全部平台" @change="applyFilters"><el-option v-for="item in platforms" :key="item.id" :label="item.name" :value="item.id" /></el-select>
      <el-select v-model="tagIds" class="dl-select tag-filter" clearable multiple collapse-tags :max-collapse-tags="1" placeholder="全部标签" @change="applyFilters"><el-option v-for="item in flatTags" :key="item.id" :label="item.name" :value="item.id" /></el-select>
      <div class="filter-actions"><button class="secondary-button search-button" @click="applyFilters">搜索</button><button class="secondary-button reset-button" @click="resetFilters">重置</button></div>
    </div>

    <AsyncState :loading="loading" :error="error" :empty="assets.length === 0" empty-title="还没有物品" empty-text="新增第一件物品，开始记录它的购买与使用周期。" @retry="load">
      <template #empty-action><PrimaryButton label="新增物品" :icon="plusIcon" @click="workspace.openNewAsset" /></template>
      <div class="list-meta"><strong>共 {{ total }} 件</strong><button class="view-switch" @click="viewMode = viewMode === 'card' ? 'list' : 'card'">{{ viewMode === 'card' ? '卡片视图' : '列表视图' }} <span>⇄</span></button></div>

      <section v-if="viewMode === 'card'" class="asset-grid">
        <article v-for="(asset, index) in assets" :key="asset.id" class="card asset-card" @click="router.push(`/assets/${asset.id}`)">
          <div class="asset-visual" :class="{ soft: index % 2 }"><img v-if="asset.coverImageUrl" :src="asset.coverImageUrl" :alt="asset.name" /><strong v-else>{{ initials(asset.name) }}</strong><span class="tag status-tag" :class="statusClass(asset.status)">{{ asset.status }}</span></div>
          <div class="asset-body"><h2>{{ asset.name }}</h2><p class="asset-category">{{ categoryLabel(asset) }}<template v-if="asset.brandName"> · {{ asset.brandName }}</template></p><p class="asset-facts">购买 {{ purchaseDate(asset) }} · 已用 {{ safeNumber(asset.useDays) }} 天 · 日均 {{ money(asset.avgCostPerDay) }}</p><div class="asset-tags"><span v-for="tag in asset.tags?.slice(0, 3)" :key="tag.id" :style="tagStyle(tag)" class="asset-tag">{{ tag.name }}</span></div><div class="asset-footer"><div><button class="text-button" @click.stop="edit(asset.id)">编辑</button><button class="text-button danger" @click.stop="confirmDelete(asset)">删除</button></div><strong>{{ money(asset.totalInvest || asset.primaryPrice) }}</strong></div></div>
        </article>
      </section>

      <section v-else class="card asset-list-card">
        <div class="table-scroll"><table><thead><tr><th class="sortable" @click="toggleSort('name')">物品 <b>{{ sortIndicator('name') }}</b></th><th class="sortable" @click="toggleSort('status')">状态 <b>{{ sortIndicator('status') }}</b></th><th>分类</th><th>标签</th><th class="sortable" @click="toggleSort('purchaseDate')">购买日期 <b>{{ sortIndicator('purchaseDate') }}</b></th><th class="sortable" @click="toggleSort('useDays')">使用天数 <b>{{ sortIndicator('useDays') }}</b></th><th class="sortable" @click="toggleSort('avgCostPerDay')">日均成本 <b>{{ sortIndicator('avgCostPerDay') }}</b></th><th class="sortable" @click="toggleSort('totalInvest')">总投入 <b>{{ sortIndicator('totalInvest') }}</b></th><th>操作</th></tr></thead><tbody><tr v-for="asset in assets" :key="asset.id" @click="router.push(`/assets/${asset.id}`)"><td><div class="list-item-name"><img v-if="asset.coverImageUrl" :src="asset.coverImageUrl" alt="" /><span v-else>{{ initials(asset.name) }}</span><strong>{{ asset.name }}</strong></div></td><td><span class="tag status-tag" :class="statusClass(asset.status)">{{ asset.status }}</span></td><td>{{ categoryLabel(asset) }}</td><td><div class="asset-tags"><span v-for="tag in asset.tags?.slice(0, 3)" :key="tag.id" :style="tagStyle(tag)" class="asset-tag">{{ tag.name }}</span><span v-if="!asset.tags?.length">—</span></div></td><td>{{ purchaseDate(asset) }}</td><td>{{ safeNumber(asset.useDays) }} 天</td><td>{{ money(asset.avgCostPerDay) }}</td><td><strong>{{ money(asset.totalInvest || asset.primaryPrice) }}</strong></td><td><button class="text-button" @click.stop="edit(asset.id)">编辑</button><button class="text-button danger" @click.stop="confirmDelete(asset)">删除</button></td></tr></tbody></table></div>
      </section>
      <el-pagination class="asset-pagination" v-model:current-page="currentPage" v-model:page-size="pageSize" :total="total" :page-sizes="[20, 50, 100]" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="changePageSize" />
    </AsyncState>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { deleteAsset, fetchAsset, fetchAssets, fetchAssetsPage } from '@/api/assets'
import { fetchBrands, fetchCategories, fetchPlatforms, fetchTags } from '@/api/settings'
import type { AssetStatus, AssetSummary, BrandItem, CategoryNode, PlatformItem, TagItem, TagNode } from '@/types'
import { useWorkspaceStore } from '@/stores/workspace'
import PageHeader from '@/components/PageHeader.vue'
import PrimaryButton from '@/components/PrimaryButton.vue'
import AsyncState from '@/components/AsyncState.vue'
import plusIcon from '@/assets/icons/plus.svg'
import searchIcon from '@/assets/icons/search.svg'

const router = useRouter()
const workspace = useWorkspaceStore()
const assets = ref<AssetSummary[]>([])
const categories = ref<CategoryNode[]>([])
const brands = ref<BrandItem[]>([])
const platforms = ref<PlatformItem[]>([])
const tags = ref<TagNode[]>([])
const loading = ref(true)
const error = ref('')
const keyword = ref('')
const status = ref('')
const categoryPath = ref<number[]>([])
const brandId = ref<number>()
const platformId = ref<number>()
const tagIds = ref<number[]>([])
const viewMode = ref<'card' | 'list'>('card')
const total = ref(0)
const currentPage = ref(1)
const pageSize = ref(20)
const sortBy = ref('purchaseDate')
const sortOrder = ref<'asc' | 'desc'>('desc')
const statuses: AssetStatus[] = ['使用中', '已闲置', '待出售', '已出售', '已丢弃']
const categoryProps = { value: 'id', label: 'name', children: 'children', checkStrictly: true, emitPath: true }
const categoryId = computed(() => categoryPath.value.at(-1))
const flatTags = computed(() => flattenTags(tags.value))

const safeNumber = (value: unknown) => { const number = Number(value); return Number.isFinite(number) ? number : 0 }
const money = (value?: number) => `¥${safeNumber(value).toLocaleString('zh-CN', { maximumFractionDigits: 2 })}`
const initials = (name: string) => name.slice(0, 2)
const purchaseDate = (asset: AssetSummary) => asset.primaryPurchaseDate || asset.purchaseDate || '未记录'
const categoryLabel = (asset: AssetSummary) => asset.categoryId ? categoryNamePath(categories.value, asset.categoryId) || asset.categoryPath || '未分类' : asset.categoryPath || '未分类'
const statusClass = (value: AssetStatus) => ({ '使用中': 'active', '已闲置': 'idle', '待出售': 'pending', '已出售': 'sold', '已丢弃': 'discarded' }[value])
const palette = ['#b7ff3c', '#8be9d1', '#ffd166', '#b8c7ff', '#ffb7d5', '#c8f7a1']
const tagStyle = (tag: TagItem) => ({ backgroundColor: `${tag.color || palette[tag.id % palette.length]}42`, borderColor: tag.color || palette[tag.id % palette.length], color: '#20231e' })
function flattenTags(nodes: TagNode[]): TagNode[] { return nodes.flatMap(node => [node, ...flattenTags(node.children || [])]) }
function categoryNamePath(nodes: CategoryNode[], id: number, parents: string[] = []): string | undefined { for (const node of nodes) { const path = [...parents, node.name]; if (node.id === id) return path.join(' / '); const found = categoryNamePath(node.children || [], id, path); if (found) return found } }

async function load() { loading.value = true; error.value = ''; try { const result = await fetchAssetsPage({ keyword: keyword.value, status: status.value, categoryId: categoryId.value, brandId: brandId.value, platformId: platformId.value, tagIds: tagIds.value, page: currentPage.value, pageSize: pageSize.value, sortBy: sortBy.value, sortOrder: sortOrder.value }); assets.value = result.records; total.value = result.total; if (!assets.value.length && total.value && currentPage.value > 1) { currentPage.value--; await load() } } catch { const all = await fetchAssets({ keyword: keyword.value, status: status.value, categoryId: categoryId.value, brandId: brandId.value, platformId: platformId.value, tagIds: tagIds.value }); const sorted = sortFallback(all); total.value = sorted.length; const start = (currentPage.value - 1) * pageSize.value; assets.value = sorted.slice(start, start + pageSize.value) } finally { loading.value = false } }
function sortFallback(items: AssetSummary[]) { const getValue = (asset: AssetSummary) => ({ name: asset.name, status: asset.status, useDays: safeNumber(asset.useDays), avgCostPerDay: safeNumber(asset.avgCostPerDay), totalInvest: safeNumber(asset.totalInvest), purchaseDate: purchaseDate(asset) }[sortBy.value] || ''); return [...items].sort((left, right) => { const a = getValue(left), b = getValue(right); const result = typeof a === 'number' && typeof b === 'number' ? a - b : String(a).localeCompare(String(b), 'zh-CN'); return sortOrder.value === 'asc' ? result : -result }) }
function applyFilters() { currentPage.value = 1; load() }
function resetFilters() { keyword.value = ''; status.value = ''; categoryPath.value = []; brandId.value = undefined; platformId.value = undefined; tagIds.value = []; applyFilters() }
function changePageSize() { currentPage.value = 1; load() }
function toggleSort(key: string) { if (sortBy.value === key) sortOrder.value = sortOrder.value === 'asc' ? 'desc' : 'asc'; else { sortBy.value = key; sortOrder.value = key === 'purchaseDate' ? 'desc' : 'asc' } currentPage.value = 1; load() }
function sortIndicator(key: string) { return sortBy.value === key ? (sortOrder.value === 'asc' ? '↑' : '↓') : '↕' }
async function edit(id: number) { try { workspace.openAssetEditor(await fetchAsset(id)) } catch (e) { ElMessage.error((e as Error).message) } }
async function confirmDelete(asset: AssetSummary) { try { await ElMessageBox.confirm(`确认删除“${asset.name}”？此操作无法撤销。`, '删除物品', { confirmButtonText: '删除', cancelButtonText: '取消', type: 'warning' }); await deleteAsset(asset.id); ElMessage.success('物品已删除'); await load() } catch (e) { if (e !== 'cancel' && e !== 'close') ElMessage.error((e as Error).message) } }
watch(() => workspace.refreshKey, load)
onMounted(async () => { const [categoryResult, brandResult, platformResult, tagResult] = await Promise.all([fetchCategories().catch(() => [] as CategoryNode[]), fetchBrands().catch(() => [] as BrandItem[]), fetchPlatforms().catch(() => [] as PlatformItem[]), fetchTags().catch(() => [] as TagNode[])]); categories.value = categoryResult; brands.value = brandResult; platforms.value = platformResult; tags.value = tagResult; await load() })
</script>

<style scoped>
.toolbar{flex-wrap:wrap}.toolbar :deep(.dl-select),.toolbar :deep(.dl-cascader){width:142px}.toolbar :deep(.tag-filter){width:160px}.toolbar :deep(.dl-cascader .el-input__wrapper),.toolbar :deep(.dl-cascader .el-input__wrapper.is-focus){min-height:46px;border:0!important;border-radius:999px;background:#fff;box-shadow:none!important;padding:0 16px}.toolbar :deep(.dl-cascader .el-input__inner){font-size:13px;color:var(--dl-text)}.toolbar :deep(.dl-cascader .el-input__inner::placeholder){color:var(--dl-muted)}.filter-actions{display:flex;flex:none;gap:10px}.reset-button,.search-button{min-width:70px}.list-meta{height:36px;display:flex;align-items:center;justify-content:space-between;color:var(--dl-text-secondary);font-size:12px}.list-meta strong{color:var(--dl-text);font-size:14px}.view-switch{padding:6px 0;border:0;background:none;color:var(--dl-text-secondary);font-size:12px;cursor:pointer}.view-switch:hover{color:var(--dl-text)}.view-switch span{margin-left:5px;color:var(--dl-lime);font-size:15px}.asset-grid{display:grid;grid-template-columns:repeat(4,minmax(0,1fr));gap:20px}.asset-card{min-height:292px;padding:14px;cursor:pointer;transition:transform .18s,box-shadow .18s}.asset-card:hover{transform:translateY(-3px);box-shadow:0 14px 32px rgba(0,0,0,.09)}.asset-visual{position:relative;height:118px;display:grid;place-items:center;overflow:hidden;border-radius:var(--dl-radius-md);background:var(--dl-bg-alt)}.asset-visual.soft{background:var(--dl-accent-soft)}.asset-visual img{width:100%;height:100%;object-fit:cover}.asset-visual strong{font-size:28px}.asset-visual .tag{position:absolute;top:10px;right:10px}.tag{display:inline-flex;align-items:center;min-height:24px;border:1px solid transparent;border-radius:999px;padding:0 9px;font-size:10px;font-weight:700;white-space:nowrap}.status-tag{background:#f3f4f0;color:var(--dl-text)}.status-tag.active{background:#e5ffb9;border-color:#b7ff3c;color:#466510}.status-tag.idle{background:#eef0eb;border-color:#cbd0c7;color:#667066}.status-tag.pending{background:#fff0d5;border-color:#ffd166;color:#965d00}.status-tag.sold{background:#def8ef;border-color:#8be9d1;color:#187259}.status-tag.discarded{background:#f0eaf0;border-color:#d6c8d8;color:#735a77}.asset-body{padding:11px 4px 0}.asset-body h2{margin:0;overflow:hidden;white-space:nowrap;text-overflow:ellipsis;font-size:15px}.asset-body p{overflow:hidden;margin:6px 0 0;color:var(--dl-text-secondary);font-size:10px;text-overflow:ellipsis;white-space:nowrap}.asset-body .asset-facts{color:var(--dl-muted);font-size:9px}.asset-tags{display:flex;min-height:24px;align-items:center;gap:5px;margin-top:8px;overflow:hidden}.asset-tag{display:inline-flex;align-items:center;min-height:20px;border:1px solid transparent;border-radius:999px;padding:0 7px;font-size:9px;font-weight:700;white-space:nowrap}.asset-footer{display:flex;align-items:center;justify-content:space-between;margin-top:5px}.asset-footer>div{display:flex;gap:8px}.asset-footer strong{font-size:14px}.asset-list-card{padding:6px 22px}.table-scroll{overflow-x:auto}.asset-list-card table{width:100%;min-width:920px;border-collapse:collapse;font-size:11px}.asset-list-card th{padding:12px 8px;border-bottom:1px solid #dfe2db;color:var(--dl-text-secondary);font-weight:600;text-align:left;white-space:nowrap}.asset-list-card th.sortable{cursor:pointer;user-select:none}.asset-list-card th.sortable:hover{color:var(--dl-text)}.asset-list-card th b{margin-left:3px;color:var(--dl-lime)}.asset-list-card td{padding:12px 8px;border-bottom:1px solid #eceee9;color:var(--dl-text-secondary);white-space:nowrap}.asset-list-card tbody tr{cursor:pointer}.asset-list-card tbody tr:hover{background:#fafbf8}.asset-list-card tbody tr:last-child td{border-bottom:0}.asset-list-card td strong{color:var(--dl-text)}.list-item-name{display:flex;align-items:center;gap:10px;min-width:170px}.list-item-name img,.list-item-name>span{width:36px;height:36px;display:grid;place-items:center;overflow:hidden;border-radius:11px;background:var(--dl-accent-soft);object-fit:cover}.asset-list-card .asset-tags{margin:0;min-width:60px}.asset-pagination{display:flex;justify-content:flex-end;margin-top:18px}@media(max-width:1200px){.asset-grid{grid-template-columns:repeat(3,minmax(0,1fr))}.toolbar .search-box{flex-basis:100%;max-width:none}}@media(max-width:860px){.asset-grid{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:560px){.asset-grid{grid-template-columns:1fr}.toolbar :deep(.dl-select),.toolbar :deep(.dl-cascader){flex:1;min-width:130px}}
</style>
