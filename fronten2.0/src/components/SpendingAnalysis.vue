<template>
  <section class="spending-analysis">
    <header class="spending-heading"><div><h2>支出分析</h2><p>按购买日期统计，每笔金额包含商品价格与运费。</p></div><span>{{ periodLabel }}</span></header>
    <div class="card spending-filters">
      <div class="filter-intro"><div><strong>筛选范围</strong><span>组合条件，查看对应的购买支出</span></div><button type="button" @click="resetFilters">重置筛选</button></div>
      <div class="filter-field period-field"><label>快捷时间</label><div class="period-buttons"><button v-for="option in periodOptions" :key="option.value" type="button" :class="{ active: period === option.value }" @click="choosePeriod(option.value)">{{ option.label }}</button></div></div>
      <div class="filter-field date-field"><label>自定义日期</label><el-date-picker v-model="customRange" type="daterange" value-format="YYYY-MM-DD" start-placeholder="开始日期" end-placeholder="结束日期" range-separator="至" unlink-panels clearable @change="applyCustomRange" /></div>
      <div class="filter-field"><label>物品分类</label><el-select v-model="categoryId" clearable filterable placeholder="全部分类" @change="applyFilters"><el-option v-for="item in flatCategories" :key="item.id" :label="item.label" :value="item.id" /></el-select></div>
      <div class="filter-field"><label>购买类型</label><el-select v-model="type" clearable placeholder="全部类型" @change="applyFilters"><el-option label="主商品" value="PRIMARY" /><el-option label="配件" value="ACCESSORY" /><el-option label="服务" value="SERVICE" /></el-select></div>
      <div class="filter-field"><label>购买平台</label><el-select v-model="platformId" clearable filterable placeholder="全部平台" @change="applyFilters"><el-option v-for="item in platforms" :key="item.id" :label="item.name" :value="item.id" /></el-select></div>
      <div class="filter-field search-field"><label>快速查找</label><div class="spending-search"><el-input v-model="keyword" clearable placeholder="搜索物品或购买记录" @keyup.enter="applyFilters" /><button type="button" @click="applyFilters">查询支出</button></div></div>
    </div>
    <div v-if="error" class="card spending-message" role="alert"><strong>支出分析暂时无法加载</strong><span>{{ error }}</span><button type="button" @click="load">重试</button></div>
    <div v-else-if="loading && !data" class="card spending-message">正在加载支出数据…</div>
    <template v-else-if="data">
      <div class="spending-stats" :class="{ 'is-loading': loading }">
        <article class="card spending-stat main"><span>筛选后总支出</span><strong>{{ money(data.totalSpend) }}</strong><small>{{ data.purchaseCount }} 笔购买 · {{ data.assetCount }} 件物品</small></article>
        <article class="card spending-stat"><span>主商品</span><strong>{{ money(data.primarySpend) }}</strong></article>
        <article class="card spending-stat"><span>配件</span><strong>{{ money(data.accessorySpend) }}</strong></article>
        <article class="card spending-stat"><span>服务</span><strong>{{ money(data.serviceSpend) }}</strong></article>
      </div>
      <div class="spending-panels" :class="{ 'is-loading': loading }">
        <article class="card spending-panel"><div class="spending-panel-title"><h3>每月支出</h3><span>购买日期所在月份</span></div>
          <div v-if="data.monthlyTrend.length" class="spending-chart-scroll" role="region" aria-label="每月支出，横向滚动查看更多月份" tabindex="0"><div class="spending-bars" :style="{ minWidth: `${Math.max(480, data.monthlyTrend.length * 76)}px` }">
            <div v-for="point in data.monthlyTrend" :key="point.month" class="spending-bar-column" :title="`${point.month} · ${money(point.amount)}`"><span>{{ compactMoney(point.amount) }}</span><div class="bar-track"><div class="bar-fill" :style="{ height: `${Math.max(3, Number(point.amount) / maxMonth * 100)}%` }" /></div><span>{{ point.month }}</span></div>
          </div></div><div v-else class="spending-empty">该范围内暂无购买记录</div>
        </article>
        <article class="card spending-panel category-panel"><CategorySpendChart :items="data.categoryBreakdown" :total="data.totalSpend" :selected-category-id="categoryId" @select="selectCategory" @clear="clearCategory" /></article>
      </div>
      <article class="card spending-records" :class="{ 'is-loading': loading }"><div class="spending-panel-title"><div><h3>购买流水</h3><p>共 {{ data.records.total }} 笔，点击物品查看详情</p></div></div>
        <div v-if="data.records.items.length" class="spending-table-scroll"><table><thead><tr><th>购买日期</th><th>物品 / 记录</th><th>类型</th><th>平台</th><th>价格</th><th>运费</th><th>支出</th></tr></thead><tbody><tr v-for="item in data.records.items" :key="item.id"><td>{{ item.purchaseDate }}</td><td><RouterLink :to="`/assets/${item.assetId}`">{{ item.assetName }}</RouterLink><small v-if="item.name">{{ item.name }}</small></td><td>{{ typeLabel(item.type) }}</td><td>{{ item.platformName || '—' }}</td><td>{{ money(item.price) }}</td><td>{{ money(item.shippingCost) }}</td><td class="amount-cell">{{ money(item.amount) }}</td></tr></tbody></table></div>
        <div v-else class="spending-empty">当前筛选条件下没有购买流水</div>
        <el-pagination v-if="data.records.total > pageSize" v-model:current-page="page" :page-size="pageSize" :total="data.records.total" layout="prev, pager, next" @current-change="load" />
      </article>
    </template>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { fetchDashboardSpending } from '@/api/dashboard'
import { fetchCategories, fetchPlatforms } from '@/api/settings'
import CategorySpendChart from './CategorySpendChart.vue'
import type { CategoryNode, DashboardSpending, PlatformItem, PurchaseType } from '@/types'
type Period = 'all' | 'month' | 'year' | 'custom'
const periodOptions: Array<{ value: Period; label: string }> = [{ value: 'all', label: '全部时间' }, { value: 'month', label: '本月' }, { value: 'year', label: '本年' }]
const period = ref<Period>('all'), customRange = ref<[string, string] | null>(null)
const categoryId = ref<number>(), type = ref<PurchaseType>(), platformId = ref<number>(), keyword = ref('')
const categories = ref<CategoryNode[]>([]), platforms = ref<PlatformItem[]>([]), data = ref<DashboardSpending>()
const page = ref(1), pageSize = 20, loading = ref(false), error = ref('')
let requestId = 0
const flatCategories = computed(() => { const result: Array<{ id: number; label: string }> = []; const visit = (nodes: CategoryNode[], prefix = '') => nodes.forEach(node => { const label = prefix ? `${prefix} / ${node.name}` : node.name; result.push({ id: node.id, label }); visit(node.children || [], label) }); visit(categories.value); return result })
const maxMonth = computed(() => Math.max(1, ...(data.value?.monthlyTrend.map(point => Number(point.amount)) || [])))
const periodLabel = computed(() => period.value === 'all' ? '全部时间' : period.value === 'month' ? '本月' : period.value === 'year' ? '本年' : customRange.value?.join(' 至 ') || '自定义日期')
const localDate = (date: Date) => `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
const money = (value: unknown) => `¥ ${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const compactMoney = (value: unknown) => Number(value || 0) >= 10000 ? `${(Number(value) / 10000).toFixed(1)}万` : Number(value || 0).toLocaleString('zh-CN', { maximumFractionDigits: 0 })
const typeLabel = (value: PurchaseType) => ({ PRIMARY: '主商品', ACCESSORY: '配件', SERVICE: '服务' })[value]
function dates() { const now = new Date(); if (period.value === 'month') return { dateFrom: localDate(new Date(now.getFullYear(), now.getMonth(), 1)), dateTo: localDate(now) }; if (period.value === 'year') return { dateFrom: localDate(new Date(now.getFullYear(), 0, 1)), dateTo: localDate(now) }; if (period.value === 'custom' && customRange.value) return { dateFrom: customRange.value[0], dateTo: customRange.value[1] }; return {} }
async function load() { const current = ++requestId; loading.value = true; error.value = ''; try { const result = await fetchDashboardSpending({ ...dates(), categoryId: categoryId.value, type: type.value, platformId: platformId.value, q: keyword.value.trim() || undefined, page: page.value, pageSize }); if (current === requestId) data.value = result } catch { if (current === requestId) error.value = '请稍后重试，或检查服务是否正常运行。' } finally { if (current === requestId) loading.value = false } }
function applyFilters() { page.value = 1; load() }
function choosePeriod(value: Period) { period.value = value; applyFilters() }
function applyCustomRange() { period.value = customRange.value ? 'custom' : 'all'; applyFilters() }
function selectCategory(value: number | null) { if (value) { categoryId.value = value; applyFilters() } }
function clearCategory() { categoryId.value = undefined; applyFilters() }
function resetFilters() { period.value = 'all'; customRange.value = null; categoryId.value = undefined; type.value = undefined; platformId.value = undefined; keyword.value = ''; applyFilters() }
onMounted(async () => { load(); const [categoryResult, platformResult] = await Promise.allSettled([fetchCategories(), fetchPlatforms()]); if (categoryResult.status === 'fulfilled') categories.value = categoryResult.value; if (platformResult.status === 'fulfilled') platforms.value = platformResult.value })
</script>

<style scoped>
.spending-analysis{margin-top:36px}.spending-heading{display:flex;justify-content:space-between;align-items:end;margin-bottom:16px}.spending-heading h2{margin:0;font-size:23px}.spending-heading p,.spending-panel-title p{margin:5px 0 0;color:var(--dl-muted);font-size:12px}.spending-heading>span{color:var(--dl-text-secondary);font-size:12px}.spending-filters{display:grid;grid-template-columns:1.35fr 1.55fr repeat(3,minmax(140px,1fr));gap:12px;padding:18px}.spending-filters :deep(.el-date-editor),.spending-filters :deep(.el-select){width:100%}.period-buttons{display:flex;align-items:center;gap:3px;padding:3px;border-radius:12px;background:#eef1ea}.period-buttons button{flex:1;min-width:0;height:32px;padding:0 6px;border:0;border-radius:9px;background:transparent;color:#596454;font-size:11px;cursor:pointer;white-space:nowrap}.period-buttons button.active{background:#202b1d;color:#fff}.spending-search{grid-column:1/-1;display:flex;gap:10px}.spending-search button,.spending-message button,.spending-panel-title button{border:0;border-radius:10px;background:#e5f9c4;color:#3d621b;font-size:12px;font-weight:700;cursor:pointer}.spending-search button{min-width:76px}.spending-stats{display:grid;grid-template-columns:1.55fr repeat(3,1fr);gap:16px;margin-top:18px}.spending-stat{min-height:118px;padding:20px}.spending-stat span{display:block;color:var(--dl-text-secondary);font-size:12px}.spending-stat strong{display:block;margin-top:12px;font-size:25px;white-space:nowrap}.spending-stat small{display:block;margin-top:5px;color:var(--dl-text-secondary);font-size:11px}.spending-stat.main{background:#eaffc6}.spending-panels{display:grid;grid-template-columns:1.2fr 1fr;gap:18px;margin-top:18px}.spending-panel{min-height:310px;padding:22px}.spending-panel-title{display:flex;justify-content:space-between;align-items:center}.spending-panel-title h3{margin:0;font-size:17px}.spending-panel-title span{color:var(--dl-muted);font-size:11px}.spending-panel-title button{padding:8px 10px}.spending-chart-scroll{overflow-x:auto;margin-top:22px}.spending-bars{display:flex;gap:10px;align-items:end;min-height:230px}.spending-bar-column{flex:1;min-width:44px;display:flex;flex-direction:column;align-items:center;gap:6px;font-size:10px;color:var(--dl-muted);white-space:nowrap}.bar-track{position:relative;width:100%;height:174px}.bar-fill{position:absolute;bottom:0;left:20%;width:60%;border-radius:7px 7px 2px 2px;background:#b7ff3c}.category-list{display:grid;gap:18px;max-height:246px;overflow:auto;margin-top:22px}.category-row{display:block;width:100%;padding:0;border:0;background:none;text-align:left;cursor:pointer}.category-row:disabled{cursor:default}.category-row-head{display:flex;justify-content:space-between;gap:12px;color:var(--dl-text);font-size:12px}.category-row-head small{margin-left:5px;color:var(--dl-muted)}.category-track{display:block;height:10px;margin-top:9px;border-radius:10px;background:#e9ece6;overflow:hidden}.category-track span{display:block;height:100%;border-radius:10px;background:#b7ff3c}.spending-records{margin-top:18px;padding:22px}.spending-table-scroll{overflow:auto;margin-top:18px}.spending-records table{width:100%;border-collapse:collapse;min-width:760px;text-align:left}.spending-records th,.spending-records td{padding:12px;border-bottom:1px solid #edf0e9;font-size:12px;white-space:nowrap}.spending-records th{color:var(--dl-muted);font-size:11px}.spending-records td a{color:#26381d;font-weight:700;text-decoration:none}.spending-records td a:hover{text-decoration:underline}.spending-records td small{display:block;margin-top:3px;color:var(--dl-muted)}.amount-cell{font-weight:700}.spending-records :deep(.el-pagination){justify-content:flex-end;margin-top:18px}.spending-empty,.spending-message{display:flex;align-items:center;justify-content:center;gap:12px;min-height:170px;color:var(--dl-muted);font-size:12px}.spending-message{margin-top:18px;min-height:100px}.spending-message button{padding:8px 12px}.is-loading{opacity:.55}
@media(max-width:1450px){.spending-filters{grid-template-columns:repeat(3,minmax(0,1fr))}.spending-stats{grid-template-columns:repeat(2,minmax(0,1fr))}}@media(max-width:1100px){.spending-panels{grid-template-columns:1fr}.spending-filters{grid-template-columns:repeat(2,minmax(0,1fr))}}
</style>
<style scoped>
.spending-analysis{margin-top:42px}
.spending-heading{margin-bottom:19px}
.spending-heading h2{font-size:25px;letter-spacing:-.03em}
.spending-heading p{margin-top:7px}
.spending-heading>span{padding:7px 12px;border:1px solid #dfe6d8;border-radius:999px;background:#f7faf3;color:#56674c;font-weight:700}
.spending-filters{grid-template-columns:repeat(5,minmax(0,1fr));gap:17px 13px;padding:22px 24px 24px;border:1px solid #ebefe7;box-shadow:0 12px 30px #25332108}
.filter-intro{grid-column:1/-1;display:flex;justify-content:space-between;align-items:center;gap:12px;padding-bottom:13px;border-bottom:1px solid #edf0e9}
.filter-intro>div{display:flex;align-items:baseline;gap:12px}
.filter-intro strong{font-size:13px}
.filter-intro span{color:#929b8d;font-size:11px}
.filter-intro button{border:0;background:none;color:#5d783f;font-size:11px;font-weight:700;cursor:pointer}
.filter-field{display:flex;flex-direction:column;gap:8px;min-width:0}
.filter-field label{color:#74806e;font-size:10px;font-weight:800;letter-spacing:.04em}
.filter-field :deep(.el-select__wrapper),.filter-field :deep(.el-input__wrapper),.filter-field :deep(.el-date-editor){min-height:42px;border-radius:11px;box-shadow:0 0 0 1px #e2e7dd inset;background:#fbfcfa}
.period-buttons{height:42px;padding:4px;background:#f0f3ed}
.period-buttons button{height:34px;font-weight:700}
.spending-search{grid-column:auto}
.search-field{grid-column:1/-1}
.spending-search button{min-width:108px;background:#b7ff3c;color:#25351b}
.spending-stats{grid-template-columns:1.35fr repeat(3,minmax(0,1fr));gap:14px;margin-top:18px}
.spending-stat{min-height:130px;border:1px solid #eef1e9;box-shadow:0 10px 25px #26351f08}
.spending-stat.main{background:linear-gradient(135deg,#dfff9b,#efffcf);border-color:#d9f7a2}
.spending-stat strong{font-size:24px;letter-spacing:-.035em}
.spending-panels{gap:16px;margin-top:16px}
.spending-panel{min-height:355px;border:1px solid #eef1e9;box-shadow:0 10px 25px #26351f08}
.category-panel{padding:22px 24px}
.spending-records{margin-top:16px;border:1px solid #eef1e9;box-shadow:0 10px 25px #26351f08}
.spending-message{flex-wrap:wrap;padding:24px;text-align:center}
.spending-message strong{color:#26351f;font-size:14px}
.spending-message span{width:100%;font-size:12px}
@media(max-width:1600px){.spending-filters{grid-template-columns:repeat(3,minmax(0,1fr))}}
@media(max-width:1300px){.spending-panels{grid-template-columns:1fr}.spending-filters{grid-template-columns:repeat(2,minmax(0,1fr))}.spending-stats{grid-template-columns:repeat(2,minmax(0,1fr))}}
.spending-panels{grid-template-columns:minmax(0,1.2fr) minmax(0,1fr)}
.spending-panel{min-width:0}
.spending-chart-scroll{width:100%;min-width:0;overflow-x:auto;overscroll-behavior-x:contain;padding-bottom:8px;scrollbar-color:#aab79e #edf1e9;scrollbar-width:thin}
.spending-chart-scroll::-webkit-scrollbar{height:9px}
.spending-chart-scroll::-webkit-scrollbar-track{border-radius:999px;background:#edf1e9}
.spending-chart-scroll::-webkit-scrollbar-thumb{border:2px solid #edf1e9;border-radius:999px;background:#aab79e}
.spending-chart-scroll::-webkit-scrollbar-thumb:hover{background:#82946f}
.spending-chart-scroll:focus-visible{outline:2px solid #8bbd3f;outline-offset:3px;border-radius:8px}
.spending-bar-column{flex:1 0 64px;min-width:64px}
@media(max-width:1300px){.spending-panels{grid-template-columns:minmax(0,1fr)}}
</style>
