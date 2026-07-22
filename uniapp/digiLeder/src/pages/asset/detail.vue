<template>
	<view class="container" v-if="asset">
		<view class="hero-image" :style="{ backgroundImage: `url(${asset.coverImageUrl || ''})` }">
			<view class="overlay"></view>
			<view class="back-btn" @click="goBack"><text class="icon">←</text></view>
		</view>
		<view class="content-body">
			<view class="title-section">
				<text class="name">{{ asset.name }}</text>
				<text class="brand" v-if="asset.brandName || asset.model">{{ asset.brandName }} {{ asset.model }}</text>
				<view class="price-row">
					<view class="price-item"><text class="label">购入价格</text><text class="value">¥{{ formatPrice(asset.totalInvest) }}</text></view>
					<view class="price-item"><text class="label">日均成本</text><text class="value sm">¥{{ formatPrice(asset.avgCostPerDay) }}</text></view>
				</view>
			</view>

			<view class="card">
				<view class="card-title between"><text>购买记录</text><button size="mini" @click="openPurchase()">新增</button></view>
				<view class="purchase-row" v-for="p in purchases" :key="p.id || `${p.purchaseDate}-${p.price}`">
					<text>¥{{ formatPrice(p.price) }} · {{ p.purchaseDate }} · x{{ p.quantity || 1 }}</text>
					<view class="actions"><text class="action" @click="openPurchase(p)">编辑</text><text class="action danger" @click="deletePurchase(p)">删除</text></view>
				</view>
				<view v-if="!purchases.length" class="empty">暂无购买记录</view>
			</view>

			<attachment-panel :list="attachments" :base-path="API_PATHS.attachments" @upload="addAttachment" @delete="deleteAttachment" />

			<view class="card">
				<view class="card-title between"><text>封面推荐</text><button size="mini" @click="loadCoverSuggestions">获取推荐</button></view>
				<view v-if="coverSuggestions.length" class="hint">已获取 {{ coverSuggestions.length }} 张候选图</view>
			</view>

			<view class="card" v-if="asset.notes"><view class="card-title">备注</view><text class="notes-text">{{ asset.notes }}</text></view>
		</view>
		<view class="footer-action">
			<button class="btn-edit" @click="goEdit">编辑物品</button>
			<button class="btn-sell" @click="goSell" v-if="asset.status !== '已出售'">记录出售</button>
		</view>

		<purchase-editor-sheet :visible="purchaseVisible" :value="editingPurchase" @close="purchaseVisible = false" @save="savePurchase" />
		<cover-suggestion-sheet :visible="coverSheetVisible" :suggestions="coverSuggestions" @close="coverSheetVisible = false" @apply="applyCover" />
	</view>
	<view v-else class="loading"><text>加载中...</text></view>
</template>

<script>
import request, { API_PATHS } from '@/utils/request.js';
import PurchaseEditorSheet from '@/components/purchase-editor-sheet.vue';
import AttachmentPanel from '@/components/attachment-panel.vue';
import CoverSuggestionSheet from '@/components/cover-suggestion-sheet.vue';

export default {
	components: { PurchaseEditorSheet, AttachmentPanel, CoverSuggestionSheet },
	data() {
		return { id: null, asset: null, purchases: [], attachments: [], purchaseVisible: false, editingPurchase: null, coverSuggestions: [], coverSheetVisible: false, API_PATHS };
	},
	onLoad(options) { const idNum = Number(options?.id || 0); if (idNum <= 0) { uni.showToast({ title:'无效ID', icon:'none' }); return; } this.id = idNum; this.refreshAll(); },
	onShow() { if (this.id) this.refreshAll(); },
	methods: {
		async refreshAll() { await Promise.allSettled([this.loadDetail(), this.loadPurchases(), this.loadAttachments()]); },
		async loadDetail() { this.asset = await request({ url: `${API_PATHS.assets}/${this.id}` }); },
		async loadPurchases() { this.purchases = await request({ url: `${API_PATHS.purchases}?assetId=${this.id}` }) || []; },
		async loadAttachments() { this.attachments = await request({ url: `${API_PATHS.attachments}?bizType=asset&bizId=${this.id}` }) || []; },
		formatPrice(val) { return Number(val || 0).toFixed(2); },
		goBack() { uni.navigateBack(); },
		goEdit() { uni.navigateTo({ url: `/pages/asset/edit?id=${this.id}` }); },
		goSell() { uni.navigateTo({ url: `/pages/asset/sell?id=${this.id}` }); },
		openPurchase(item = null) { this.editingPurchase = item ? { ...item } : { assetId: this.id, quantity: 1, purchaseDate: new Date().toISOString().slice(0,10) }; this.purchaseVisible = true; },
		async savePurchase(payload) {
			const isEdit = !!payload.id;
			await request({ url: isEdit ? `${API_PATHS.purchases}/${payload.id}` : API_PATHS.purchases, method: isEdit ? 'PUT' : 'POST', data: { ...payload, assetId: this.id } });
			this.purchaseVisible = false;
			await this.refreshAll();
		},
		async deletePurchase(item) { await request({ url: `${API_PATHS.purchases}/${item.id}`, method: 'DELETE' }); await this.refreshAll(); },
		async addAttachment(url) { await request({ url: API_PATHS.attachments, method: 'POST', data: { bizType: 'asset', bizId: this.id, url } }); await this.loadAttachments(); },
		async deleteAttachment(file) { await request({ url: `${API_PATHS.attachments}/${file.id}`, method: 'DELETE' }); await this.loadAttachments(); },
		async loadCoverSuggestions() {
			this.coverSuggestions = await request({ url: `${API_PATHS.assets}/${this.id}/cover/suggestions` }) || [];
			this.coverSheetVisible = true;
		},
		async applyCover(url) {
			await request({ url: `${API_PATHS.assets}/${this.id}/cover/from-url`, method: 'POST', data: { url } });
			this.coverSheetVisible = false;
			await this.loadDetail();
		}
	}
}
</script>

<style lang="scss">
.container { min-height:100vh; background:$uni-bg-base; padding-bottom:120rpx; }
.hero-image { height:420rpx; background-size:cover; background-position:center; position:relative; }
.overlay { position:absolute; inset:0; background:linear-gradient(to bottom, rgba(0,0,0,.3), $uni-bg-base); }
.back-btn { position:absolute; top:calc(var(--status-bar-height) + 20rpx); left:30rpx; width:80rpx; height:80rpx; background:rgba(0,0,0,.5); border-radius:50%; @include flex-center; z-index:10; }
.icon { color:#fff; font-size:40rpx; }
.content-body { padding: 20rpx 30rpx; margin-top:-30rpx; position:relative; z-index:1; }
.title-section { margin-bottom: 20rpx; }
.name{ font-size:42rpx; font-weight:700; color:$uni-text-primary; display:block; }
.brand{ font-size:26rpx; color:$uni-text-secondary; display:block; margin:8rpx 0 18rpx; }
.price-row { display:flex; gap:50rpx; }
.label{ font-size:24rpx; color:$uni-text-muted; display:block; }
.value{ font-size:34rpx; color:$uni-text-primary; font-weight:700; }
.value.sm{ color:$uni-color-primary; font-size:28rpx; }
.card { @include card-base; margin-bottom: 20rpx; }
.card-title { font-size:28rpx; color:$uni-text-primary; margin-bottom: 14rpx; }
.between { display:flex; justify-content:space-between; align-items:center; }
.purchase-row{display:flex;justify-content:space-between;align-items:center;padding:10rpx 0;color:$uni-text-secondary}.actions{display:flex;gap:14rpx}.action{color:$uni-color-primary}.danger{color:$uni-color-danger}
.empty{color:$uni-text-muted;padding:10rpx 0}
.footer-action { position:fixed; bottom:0; left:0; right:0; background:rgba(18,18,18,.9); padding:18rpx 30rpx calc(18rpx + env(safe-area-inset-bottom)); border-top:1px solid rgba(255,255,255,.1); display:flex; gap:20rpx; }
.btn-edit,.btn-sell{ flex:1; height:84rpx; border-radius:44rpx; font-size:30rpx; display:flex; align-items:center; justify-content:center; }
.btn-edit{ background:$uni-bg-surface-light; color:$uni-text-primary; }
.btn-sell{ background:linear-gradient(135deg, $uni-color-danger, rgba(251,113,133,1)); color:#fff; }
.loading { padding: 140rpx 0; text-align:center; color:$uni-text-muted; }
</style>
