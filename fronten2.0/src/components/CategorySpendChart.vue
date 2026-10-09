<template>
  <div class="category-chart">
    <div class="chart-toolbar">
      <div>
        <span class="eyebrow">CATEGORY MIX</span>
        <h3>分类支出</h3>
      </div>
      <div class="chart-actions">
        <button v-if="selectedCategoryId" type="button" class="clear-category" @click="emit('clear')">全部分类</button>
        <div class="chart-switch" aria-label="分类图表类型">
          <button type="button" :aria-pressed="viewMode === 'donut'" :class="{ active: viewMode === 'donut' }" @click="viewMode = 'donut'">占比</button>
          <button type="button" :aria-pressed="viewMode === 'bar'" :class="{ active: viewMode === 'bar' }" @click="viewMode = 'bar'">排行</button>
        </div>
      </div>
    </div>

    <div v-if="!items.length || totalAmount <= 0" class="category-empty">当前筛选范围没有分类支出</div>
    <div v-else-if="viewMode === 'donut'" class="donut-layout">
      <div class="donut-stage">
        <svg viewBox="0 0 180 180" role="img" :aria-label="`分类支出占比，共 ${money(totalAmount)}`">
          <circle cx="90" cy="90" r="67" class="donut-base" />
          <circle v-for="segment in segments" :key="segment.key" cx="90" cy="90" r="67"
            class="donut-segment" :stroke="segment.color"
            :stroke-dasharray="`${segment.length} ${circumference}`"
            :stroke-dashoffset="-segment.offset" />
        </svg>
        <div class="donut-center"><span>分类总支出</span><strong>{{ compactMoney(totalAmount) }}</strong></div>
      </div>
      <div class="donut-legend">
        <button v-for="segment in segments" :key="segment.key" type="button" class="legend-row"
          :disabled="!segment.categoryId || segment.categoryId === selectedCategoryId"
          @click="select(segment.categoryId)">
          <span class="legend-name"><i :style="{ background: segment.color }" />{{ segment.name }}</span>
          <span class="legend-values"><strong>{{ percent(segment.amount) }}</strong><small>{{ money(segment.amount) }}</small></span>
        </button>
      </div>
    </div>
    <div v-else class="rank-list">
      <button v-for="(item, index) in items" :key="item.categoryId ?? 'none'" type="button" class="rank-row"
        :disabled="!item.categoryId || item.categoryId === selectedCategoryId" @click="select(item.categoryId)">
        <span class="rank-head"><span><em>{{ String(index + 1).padStart(2, '0') }}</em>{{ item.categoryName }}</span><strong>{{ money(item.amount) }}</strong></span>
        <span class="rank-track"><span :style="{ width: percent(item.amount), background: colors[index % colors.length] }" /></span>
        <span class="rank-foot"><span>{{ item.purchaseCount }} 笔购买</span><span>{{ percent(item.amount) }}</span></span>
      </button>
    </div>
    <p v-if="items.length && totalAmount > 0" class="chart-note">{{ selectedCategoryId ? '显示所选分类及直属子分类' : '点击分类查看更细的支出' }}</p>
  </div>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import type { DashboardSpending } from '@/types'

type Category = DashboardSpending['categoryBreakdown'][number]
const props = defineProps<{ items: Category[]; total: number; selectedCategoryId?: number }>()
const emit = defineEmits<{ select: [categoryId: number | null]; clear: [] }>()
const viewMode = ref<'donut' | 'bar'>('donut')
const colors = ['#aaf53c', '#26351f', '#6cab78', '#f0ab50', '#87a6d3', '#c6d6b8']
const circumference = 2 * Math.PI * 67
const totalAmount = computed(() => Number(props.total || 0))
const money = (value: number) => `¥ ${Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
const compactMoney = (value: number) => value >= 10000 ? `¥ ${(value / 10000).toFixed(1)}万` : money(value)
const percent = (amount: number) => `${(Number(amount || 0) / (totalAmount.value || 1) * 100).toFixed(1)}%`
const donutItems = computed(() => {
  const positive = props.items.filter(item => Number(item.amount) > 0)
  const visible = positive.slice(0, 5).map(item => ({ categoryId: item.categoryId, name: item.categoryName, amount: Number(item.amount) }))
  if (positive.length > 5) visible.push({ categoryId: null, name: '其他分类', amount: positive.slice(5).reduce((sum, item) => sum + Number(item.amount), 0) })
  return visible
})
const segments = computed(() => {
  let offset = 0
  return donutItems.value.map((item, index) => {
    const share = item.amount / (totalAmount.value || 1)
    const length = Math.max(0, share * circumference - 1.5)
    const segment = { ...item, key: `${item.categoryId ?? 'other'}-${index}`, color: colors[index % colors.length], length, offset }
    offset += share * circumference
    return segment
  })
})
function select(categoryId: number | null) { if (categoryId) emit('select', categoryId) }
</script>

<style scoped>
.category-chart{height:100%;display:flex;flex-direction:column}.chart-toolbar,.chart-actions{display:flex;align-items:center;justify-content:space-between;gap:12px}.eyebrow{color:#8b9685;font-size:10px;font-weight:800;letter-spacing:.13em}.chart-toolbar h3{margin:4px 0 0;font-size:18px;line-height:1.35}.chart-actions{justify-content:flex-end}.clear-category{border:0;background:none;color:#506a3b;font-size:11px;font-weight:700;cursor:pointer;white-space:nowrap}.chart-switch{display:flex;padding:3px;border-radius:999px;background:#edf1e9}.chart-switch button{min-width:48px;padding:6px 10px;border:0;border-radius:999px;background:none;color:#6a7565;font-size:11px;font-weight:700;cursor:pointer}.chart-switch button.active{background:#26351f;color:#fff;box-shadow:0 2px 6px #26351f26}.donut-layout{display:grid;grid-template-columns:minmax(165px,.85fr) minmax(0,1.15fr);align-items:center;gap:18px;flex:1;min-height:240px}.donut-stage{position:relative;width:min(100%,210px);aspect-ratio:1;margin:auto}.donut-stage svg{display:block;width:100%;height:100%;transform:rotate(-90deg)}.donut-base,.donut-segment{fill:none;stroke-width:23}.donut-base{stroke:#eef1e9}.donut-segment{transition:stroke-dasharray .25s,stroke-dashoffset .25s}.donut-center{position:absolute;inset:0;display:flex;flex-direction:column;align-items:center;justify-content:center;pointer-events:none}.donut-center span{color:#879183;font-size:10px}.donut-center strong{margin-top:5px;font-size:19px;letter-spacing:-.04em}.donut-legend{display:grid;gap:4px;max-height:256px;overflow:auto}.legend-row{display:flex;align-items:center;justify-content:space-between;gap:8px;width:100%;min-height:38px;padding:5px 3px;border:0;border-bottom:1px solid #f0f2ec;background:none;text-align:left;cursor:pointer}.legend-row:disabled{cursor:default}.legend-name{display:flex;align-items:center;gap:7px;min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap;font-size:11px}.legend-name i{flex:none;width:9px;height:9px;border-radius:3px}.legend-values{display:flex;flex-direction:column;align-items:end;gap:1px;white-space:nowrap}.legend-values strong{font-size:11px}.legend-values small{color:#8a9486;font-size:9px}.rank-list{display:grid;gap:13px;max-height:280px;overflow:auto;margin-top:19px}.rank-row{display:block;width:100%;padding:0 2px;border:0;background:none;text-align:left;cursor:pointer}.rank-row:disabled{cursor:default}.rank-head,.rank-foot{display:flex;justify-content:space-between;align-items:center;gap:10px}.rank-head{font-size:11px}.rank-head>span{overflow:hidden;text-overflow:ellipsis;white-space:nowrap}.rank-head em{margin-right:9px;color:#a1aaa0;font-style:normal;font-weight:800}.rank-head strong{white-space:nowrap}.rank-track{display:block;height:8px;overflow:hidden;margin-top:8px;border-radius:999px;background:#edf0e9}.rank-track>span{display:block;height:100%;border-radius:999px;transition:width .2s}.rank-foot{margin-top:4px;color:#919b8e;font-size:9px}.chart-note{margin:auto 0 0;padding-top:10px;color:#98a194;font-size:10px}.category-empty{display:grid;flex:1;min-height:230px;place-items:center;color:#8d968a;font-size:12px}@media(max-width:1370px){.donut-layout{grid-template-columns:1fr;gap:4px}.donut-stage{width:160px}.donut-legend{max-height:130px}.category-chart{min-height:330px}}
</style>
