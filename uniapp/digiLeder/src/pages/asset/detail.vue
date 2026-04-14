<template>
	<view class="container" v-if="asset">
		<!-- Hero Image -->
		<view class="hero-image" :style="{ backgroundImage: `url(${asset.coverImageUrl})` }">
			<div class="overlay"></div>
			<div class="back-btn" @click="goBack">
				<text class="icon">←</text>
			</div>
			
			<div class="hero-content">
				<view class="badges">
					<text class="badge status" :class="statusClass">{{ asset.status }}</text>
					<text class="badge category" v-if="asset.categoryPath">{{ asset.categoryPath }}</text>
				</view>
			</div>
		</view>
		
		<view class="content-body">
			<!-- Title Section -->
			<view class="title-section">
				<text class="name">{{ asset.name }}</text>
				<text class="brand" v-if="asset.brandName || asset.model">
					{{ asset.brandName }} {{ asset.model }}
				</text>
				
				<view class="price-row">
					<view class="price-item">
						<text class="label">购入价格</text>
						<text class="value">¥{{ formatPrice(asset.totalInvest) }}</text>
					</view>
					<view class="price-item">
						<text class="label">日均成本</text>
						<text class="value sm">¥{{ formatPrice(asset.avgCostPerDay) }}</text>
					</view>
				</view>
			</view>
			
			<!-- Lifecycle -->
			<view class="card">
				<view class="card-title">生命周期</view>
				<view class="timeline">
					<view class="timeline-item">
						<view class="dot active"></view>
						<view class="info">
							<text class="label">购入日期</text>
							<text class="val">{{ asset.purchaseDate }}</text>
						</view>
					</view>
					<view class="line"></view>
					<view class="timeline-item">
						<view class="dot current"></view>
						<view class="info">
							<text class="label">已使用</text>
							<text class="val highlight">{{ asset.useDays }} 天</text>
						</view>
					</view>
				</view>
			</view>
			
			<!-- Notes -->
			<view class="card" v-if="asset.notes">
				<view class="card-title">备注</view>
				<text class="notes-text">{{ asset.notes }}</text>
			</view>
		</view>
		
		<!-- Footer -->
		<view class="footer-action">
			<button class="btn-edit" @click="goEdit">编辑物品</button>
			<button class="btn-sell" @click="goSell" v-if="asset && asset.status !== '已出售'">记录出售</button>
		</view>
	</view>
	<view v-else class="loading">
		<text>加载中...</text>
	</view>
</template>

<script>
	import request from '@/utils/request.js';
	
	export default {
		data() {
			return {
				id: null,
				asset: null
			}
		},
		onLoad(options) {
			const parsedId = this.parseId(options && options.id);
			if (!parsedId) {
				uni.showToast({ title: '无效的物品ID', icon: 'none' });
				setTimeout(() => uni.navigateBack(), 400);
				return;
			}
			this.id = parsedId;
			this.loadDetail();
		},
		onShow() {
			// Reload when returning from edit
			if (this.id) this.loadDetail();
		},
		computed: {
			statusClass() {
				if (!this.asset) return '';
				const map = {
					'使用中': 'active',
					'已闲置': 'idle',
					'已出售': 'sold'
				};
				return map[this.asset.status] || '';
			}
		},
		methods: {
			parseId(rawId) {
				const normalized = String(rawId || '').trim();
				if (!normalized) return null;
				const idNum = Number(normalized);
				if (!Number.isInteger(idNum) || idNum <= 0) return null;
				return idNum;
			},
			async loadDetail() {
				if (!this.id) return;
				try {
					const res = await request({ url: `/assets/${this.id}` });
					this.asset = res;
				} catch (e) {
					console.error(e);
					uni.showToast({ title: '加载失败', icon: 'none' });
				}
			},
			formatPrice(val) {
				return (val || 0).toFixed(2);
			},
			goBack() {
				uni.navigateBack();
			},
			goEdit() {
				uni.navigateTo({
					url: `/pages/asset/edit?id=${this.id}`
				});
			},
			goSell() {
				uni.navigateTo({
					url: `/pages/asset/sell?id=${this.id}`
				});
			}
		}
	}
</script>

<style lang="scss">
	.container {
		min-height: 100vh;
		background: $uni-bg-base;
		padding-bottom: 120rpx;
	}
	
	.hero-image {
		height: 500rpx;
		background-size: cover;
		background-position: center;
		position: relative;
		
		.overlay {
			position: absolute;
			inset: 0;
			background: linear-gradient(to bottom, rgba(0,0,0,0.3), $uni-bg-base);
		}
		
		.back-btn {
			position: absolute;
			top: calc(var(--status-bar-height) + 20rpx);
			left: 30rpx;
			width: 80rpx;
			height: 80rpx;
			background: rgba(0, 0, 0, 0.5);
			border-radius: 50%;
			@include flex-center;
			z-index: 10;
			
			.icon { color: #fff; font-size: 40rpx; }
		}
		
		.hero-content {
			position: absolute;
			bottom: 40rpx;
			left: 30rpx;
			right: 30rpx;
		}
		
		.badges {
			display: flex;
			gap: 16rpx;
			
			.badge {
				padding: 6rpx 16rpx;
				border-radius: 8rpx;
				font-size: 24rpx;
				backdrop-filter: blur(4px);
				
				&.status {
					background: rgba(255, 255, 255, 0.2);
					color: #fff;
					
					&.active { background: rgba(0, 255, 157, 0.2); color: $uni-color-success; }
					&.idle { background: rgba(255, 215, 0, 0.2); color: $uni-color-warning; }
					&.sold { background: rgba(255, 71, 87, 0.2); color: $uni-color-danger; }
				}
				
				&.category {
					background: rgba(0, 0, 0, 0.6);
					color: $uni-text-secondary;
					border: 1px solid rgba(255, 255, 255, 0.1);
				}
			}
		}
	}
	
	.content-body {
		padding: 30rpx;
		margin-top: -30rpx;
		position: relative;
		z-index: 1;
	}
	
	.title-section {
		margin-bottom: 40rpx;
		
		.name {
			font-size: 48rpx;
			font-weight: bold;
			color: $uni-text-primary;
			display: block;
			margin-bottom: 12rpx;
		}
		
		.brand {
			font-size: 28rpx;
			color: $uni-text-secondary;
			display: block;
			margin-bottom: 30rpx;
		}
		
		.price-row {
			display: flex;
			gap: 60rpx;
			
			.price-item {
				display: flex;
				flex-direction: column;
				
				.label { font-size: 24rpx; color: $uni-text-muted; margin-bottom: 8rpx; }
				.value {
					font-size: 40rpx;
					font-weight: bold;
					color: $uni-text-primary;
					font-family: monospace;
					
					&.sm { font-size: 32rpx; color: $uni-color-primary; }
				}
			}
		}
	}
	
	.card {
		@include card-base;
		margin-bottom: 30rpx;
		
		.card-title {
			font-size: 28rpx;
			color: $uni-text-muted;
			margin-bottom: 24rpx;
			text-transform: uppercase;
			letter-spacing: 2rpx;
		}
		
		.notes-text {
			font-size: 28rpx;
			color: $uni-text-secondary;
			line-height: 1.6;
		}
	}
	
	.timeline {
		display: flex;
		align-items: center;
		
		.timeline-item {
			display: flex;
			align-items: center;
			gap: 16rpx;
			
			.dot {
				width: 20rpx;
				height: 20rpx;
				border-radius: 50%;
				background: $uni-text-muted;
				
				&.active { background: $uni-color-success; box-shadow: 0 0 10rpx $uni-color-success; }
				&.current { background: $uni-color-primary; box-shadow: 0 0 10rpx $uni-color-primary; }
			}
			
			.info {
				display: flex;
				flex-direction: column;
				
				.label { font-size: 22rpx; color: $uni-text-muted; }
				.val { font-size: 28rpx; color: $uni-text-secondary; }
				.highlight { color: $uni-color-primary; font-weight: bold; }
			}
		}
		
		.line {
			flex: 1;
			height: 2rpx;
			background: rgba(255, 255, 255, 0.1);
			margin: 0 30rpx;
		}
	}
	
	.footer-action {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		background: rgba(18, 18, 18, 0.9);
		backdrop-filter: blur(10px);
		padding: 20rpx 30rpx;
		padding-bottom: calc(20rpx + env(safe-area-inset-bottom));
		border-top: 1px solid rgba(255, 255, 255, 0.1);
		display: flex;
		gap: 20rpx;
		
		.btn-edit {
			flex: 1;
			background: $uni-bg-surface-light;
			color: $uni-text-primary;
			border: 1px solid rgba(255, 255, 255, 0.2);
			border-radius: 44rpx;
			height: 88rpx;
			display: flex;
			align-items: center;
			justify-content: center;
			font-size: 32rpx;
			
			&::after { display: none; }
			&:active { background: rgba(255, 255, 255, 0.1); }
		}
		
		.btn-sell {
			flex: 1.6;
			background: linear-gradient(135deg, $uni-color-danger, rgba(251, 113, 133, 1));
			color: #fff;
			font-weight: 600;
			border-radius: 44rpx;
			height: 88rpx;
			display: flex;
			align-items: center;
			justify-content: center;
			font-size: 32rpx;
			box-shadow: 0 6rpx 20rpx rgba(239, 68, 68, 0.3);
			border: none;
			
			&::after { display: none; }
		}
	}
	
	.loading {
		height: 100vh;
		@include flex-center;
		color: $uni-text-muted;
	}
</style>
