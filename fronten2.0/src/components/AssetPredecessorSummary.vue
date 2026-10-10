<template>
  <section class="card predecessor-section" aria-labelledby="predecessor-heading">
    <header class="predecessor-heading">
      <div><h3 id="predecessor-heading">上代产品</h3><span v-if="predecessor" class="same-category">同类别</span></div>
      <button type="button" class="manage-link" @click="$emit('manage')">{{ predecessor ? '更换关联' : '关联上代产品' }}</button>
    </header>
    <p class="section-description">关联同类别的上代产品，方便对比购买价格与更换周期。</p>
    <div v-if="predecessor" class="predecessor-strip">
      <div class="product-summary">
        <button type="button" class="cover-button" :aria-label="`预览 ${predecessor.name}`" @click="$emit('preview', predecessor.id)">
          <img v-if="predecessor.coverImageUrl" :src="predecessor.coverImageUrl" :alt="predecessor.name" />
          <img v-else class="empty-cover-icon" :src="assetIcon" alt="暂无封面" />
        </button>
        <div class="product-copy">
          <div class="product-title"><button type="button" @click="$emit('preview', predecessor.id)">{{ predecessor.name }}</button><span class="status-tag">{{ predecessor.status }}</span></div>
          <p>主商品 <strong>{{ nullableMoney(predecessor.primaryPrice) }}</strong></p>
          <small>购买于 {{ predecessor.primaryPurchaseDate || '未填写' }}</small>
        </div>
      </div>
      <div class="comparison-metric"><span>主商品差价</span><strong>{{ signedMoney(comparison.delta) }}</strong><small>{{ priceHint }}</small></div>
      <div class="comparison-metric"><span>购买间隔</span><strong>{{ comparison.gap === null ? '暂无法计算' : `${Math.abs(comparison.gap)} 天` }}</strong><small :class="{ warning: comparison.gap !== null && comparison.gap < 0 }">{{ dateHint }}</small></div>
      <div class="summary-actions"><button type="button" class="preview-button" @click="$emit('preview', predecessor.id)">快速预览</button><RouterLink :to="`/assets/${predecessor.id}`">查看物品详情</RouterLink></div>
    </div>
    <div v-else class="predecessor-empty">尚未关联上代产品，可在编辑物品时搜索选择。</div>
    <p v-if="predecessor" class="comparison-note">差价 = 当前主商品购买价 − 上代主商品购买价，不含运费、配件与服务。</p>
  </section>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import type { AssetPredecessor, PurchaseRecord } from '@/types'
import { nullableMoney, signedMoney } from '@/utils/predecessor'
import assetIcon from '@/assets/icons/assets.svg'
const props = defineProps<{ predecessor?: AssetPredecessor | null; currentPurchase?: PurchaseRecord }>()
defineEmits<{ manage: []; preview: [id: number] }>()
const comparison = computed(() => ({ delta: props.predecessor?.primaryPriceDelta ?? null, gap: props.predecessor?.purchaseGapDays ?? null }))
const priceHint = computed(() => comparison.value.delta === null ? '缺少主商品购买价格' : comparison.value.delta > 0 ? '当前比上代多支出' : comparison.value.delta < 0 ? '当前比上代少支出' : '两代主商品价格相同')
const dateHint = computed(() => comparison.value.gap === null ? '缺少主商品购买日期' : comparison.value.gap < 0 ? '当前购买日期早于上代，请核对' : comparison.value.gap === 0 ? '两件主商品在同一天购买' : `${props.predecessor?.primaryPurchaseDate} 至 ${props.currentPurchase?.purchaseDate}`)
</script>

<style scoped>
.predecessor-section{container-type:inline-size;margin-top:24px;padding:24px;border:1px solid #edf0e9}
.predecessor-heading,.predecessor-heading>div{display:flex;align-items:center;gap:14px}.predecessor-heading{justify-content:space-between}.predecessor-heading h3{margin:0;font-size:21px;letter-spacing:-.025em}.same-category{padding:5px 10px;border-radius:999px;background:var(--dl-accent-soft);color:#3f601e;font-size:11px;font-weight:700}.manage-link{padding:7px 0;border:0;background:none;color:#4f6b33;font-size:12px;font-weight:700;cursor:pointer}.manage-link:hover{text-decoration:underline}.section-description{margin:10px 0 20px;color:var(--dl-muted);font-size:12px;line-height:1.6}
.predecessor-strip{display:grid;grid-template-columns:minmax(0,1.6fr) minmax(0,.8fr) minmax(0,1fr) 128px;gap:20px;align-items:center;padding:18px;border-radius:16px;background:#f7f8f5}.product-summary{display:flex;align-items:center;gap:16px;min-width:0}.cover-button{flex:none;display:grid;place-items:center;width:88px;height:112px;padding:6px;border:0;border-radius:12px;background:#eef0eb;cursor:zoom-in}.cover-button img{width:100%;height:100%;object-fit:contain}.cover-button .empty-cover-icon{width:28px;height:28px;opacity:.4}.product-copy{min-width:0}.product-title{display:flex;flex-wrap:wrap;align-items:center;gap:8px}.product-title button{max-width:100%;padding:0;border:0;background:none;text-align:left;overflow-wrap:anywhere;font-size:15px;line-height:1.5;font-weight:800;cursor:pointer}.product-title button:hover{text-decoration:underline}.status-tag{flex:none;padding:3px 8px;border-radius:999px;background:#e6e9e2;color:#586051;font-size:10px}.product-copy p{margin:10px 0 6px;color:var(--dl-muted);font-size:12px}.product-copy p strong{margin-left:6px;color:var(--dl-text);font-size:16px;white-space:nowrap}.product-copy small{color:var(--dl-muted);font-size:11px}.comparison-metric{min-width:0;min-height:88px;padding-left:20px;border-left:1px solid #e0e5dc;display:flex;flex-direction:column;gap:8px}.comparison-metric>span{color:var(--dl-muted);font-size:12px}.comparison-metric strong{font-size:23px;line-height:1.35;white-space:nowrap;letter-spacing:-.035em}.comparison-metric small{color:var(--dl-muted);font-size:10px;line-height:1.6;overflow-wrap:anywhere}.comparison-metric small.warning{color:#a06c15}.summary-actions{display:flex;flex-direction:column;gap:14px;align-items:stretch;padding-left:16px;border-left:1px solid #e0e5dc}.preview-button{height:42px;padding:0 12px;border:1px solid #d8ddd2;border-radius:11px;background:#eef0e9;font-size:12px;font-weight:700;cursor:pointer;transition:background .18s}.preview-button:hover{background:#e3edda}.summary-actions a{color:#466825;text-align:center;font-size:11px;font-weight:700;text-decoration:none}.summary-actions a:hover{text-decoration:underline}.comparison-note{margin:12px 0 0;color:var(--dl-muted);font-size:10px;line-height:1.6}.predecessor-empty{padding:22px;border-radius:14px;background:#f7f8f5;color:var(--dl-muted);font-size:12px}button:focus-visible,a:focus-visible{outline:2px solid #7da642;outline-offset:4px}
@container(max-width:900px){.predecessor-strip{grid-template-columns:minmax(0,1fr) minmax(0,1fr);gap:18px}.product-summary{grid-column:1/-1}.comparison-metric{padding:0 0 0 16px}.summary-actions{grid-column:1/-1;flex-direction:row;justify-content:flex-end;align-items:center;border-left:0;padding:0}.summary-actions a{padding:10px 0}}
@container(max-width:430px){.predecessor-strip{grid-template-columns:minmax(0,1fr)}.comparison-metric{min-height:0}.product-summary{align-items:flex-start}.cover-button{width:64px;height:80px}.predecessor-heading{gap:8px}.predecessor-heading h3{font-size:18px}.same-category{display:none}}
.predecessor-heading h3{font-size:26px}.same-category{font-size:12px}.manage-link{font-size:14px}.section-description{font-size:14px}.cover-button{width:112px;height:140px}.product-title button{font-size:19px}.status-tag{font-size:11px}.product-copy p{font-size:14px}.product-copy p strong{font-size:18px}.product-copy small{font-size:12px}.comparison-metric>span{font-size:14px}.comparison-metric strong{font-size:28px}.comparison-metric small{font-size:12px}.summary-actions{padding-left:12px}.preview-button{height:46px;font-size:14px}.summary-actions a{font-size:12px}.comparison-note{font-size:11px}
@container(max-width:900px){.product-title button{font-size:18px}.comparison-metric strong{font-size:26px}}
@container(max-width:430px){.cover-button{width:64px;height:80px}.predecessor-heading h3{font-size:18px}}
</style>
