<template>
	<view class="asset-card" hover-class="card-hover-active" @click="onClick">
		<view class="thumb">
			<image v-if="asset.coverImageUrl" :src="asset.coverImageUrl" mode="aspectFill"></image>
			<view v-else class="placeholder">
				<!-- SVG placeholder can go here, using text for now -->
				<text class="icon">💻</text>
			</view>
			<view class="status-badge" :class="statusClass">
				<text>{{ asset.status }}</text>
			</view>
		</view>
		
		<view class="content">
			<view class="header">
				<text class="name">{{ asset.name }}</text>
				<text class="brand" v-if="asset.brandName || asset.brand">{{ asset.brandName || asset.brand }}</text>
			</view>
			
			<view class="info">
				<text class="price">¥{{ formatPrice(asset.totalInvest) }}</text>
				<text class="days">已用: {{ asset.useDays }} 天</text>
			</view>
		</view>
	</view>
</template>

<script>
	export default {
		props: {
			asset: {
				type: Object,
				required: true
			}
		},
		computed: {
			statusClass() {
				const map = {
					'使用中': 'active',
					'已闲置': 'idle',
					'待出售': 'pending',
					'已出售': 'sold',
					'已丢弃': 'discarded'
				};
				return map[this.asset.status] || '';
			}
		},
		methods: {
			formatPrice(val) {
				const cost = typeof val === 'number' ? val : parseFloat(val);
				if (isNaN(cost)) return '0.00';
				return cost.toFixed(2);
			},
			onClick() {
				// 使用自定义事件名，避免与原生 click 冲突导致参数丢失
				this.$emit('select', this.asset);
			}
		}
	}
</script>

<style lang="scss">
	@mixin text-ellipsis {
		overflow: hidden;
		white-space: nowrap;
		text-overflow: ellipsis;
	}

	.asset-card {
		/* 使用全局定义的高级质感大卡片 */
		@include card-base;
		display: flex;
		margin-bottom: 24rpx;
		/* 确保内部容器响应flex内容并防止溢出 */
		box-sizing: border-box;
		box-shadow: 0 10rpx 30rpx -10rpx rgba(0, 0, 0, 0.5);
	}
	
	.thumb {
		width: 160rpx;
		height: 160rpx;
		border-radius: $uni-radius-md;
		overflow: hidden;
		position: relative;
		margin-right: 32rpx;
		background: #000;
		flex-shrink: 0;
		box-shadow: 0 4rpx 10rpx rgba(0,0,0,0.3);
		
		image {
			width: 100%;
			height: 100%;
		}
		
		.placeholder {
			width: 100%;
			height: 100%;
			@include flex-center;
			background: rgba(255, 255, 255, 0.05);
			
			.icon { font-size: 64rpx; }
		}
		
		.status-badge {
			position: absolute;
			top: 0;
			right: 0;
			background: rgba(0, 0, 0, 0.75);
			font-size: 20rpx;
			padding: 4rpx 12rpx;
			border-bottom-left-radius: 12rpx;
			backdrop-filter: blur(8px);
			font-weight: bold;
			
			&.active { color: $uni-color-success; }
			&.idle { color: $uni-color-warning; }
			&.pending { color: $uni-color-primary; }
			&.sold { color: $uni-color-danger; }
		}
	}
	
	.content {
		flex: 1;
		display: flex;
		flex-direction: column;
		justify-content: space-between;
		min-width: 0; /* 允许内部文本使用省略号 */
	}
	
	.header {
		.name {
			font-size: 34rpx;
			font-weight: 600;
			color: $uni-text-primary;
			display: block;
			margin-bottom: 12rpx;
			@include text-ellipsis;
		}
		
		.brand {
			display: inline-block;
			font-size: 22rpx;
			color: $uni-text-primary;
			background: $uni-color-primary-dim;
			padding: 4rpx 16rpx;
			border-radius: 8rpx;
			border: 1px solid rgba(59, 130, 246, 0.2);
			font-weight: 500;
		}
	}
	
	.info {
		display: flex;
		justify-content: space-between;
		align-items: flex-end;
		
		.price {
			font-size: 40rpx;
			font-weight: 700;
			color: $uni-color-primary;
			font-family: $uni-font-tabular;
			letter-spacing: -1rpx;
		}
		
		.days {
			font-size: 24rpx;
			color: $uni-text-muted;
			font-family: $uni-font-tabular;
		}
	}
</style>
