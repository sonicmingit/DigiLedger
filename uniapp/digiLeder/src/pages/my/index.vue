<template>
	<view class="container">
		<!-- 自定义顶部 -->
		<view class="header">
			<view class="avatar-wrap">
				<view class="avatar">
					<text class="avatar-icon">⚡</text>
				</view>
				<view class="user-info">
					<text class="username">DigiLedger</text>
					<text class="version">v0.4.0 · 数码账本</text>
				</view>
			</view>
		</view>

		<!-- KPI 统计区 -->
		<view class="stats-row">
			<view class="stat-block">
				<text class="stat-val">{{ stats.total }}</text>
				<text class="stat-label">物品总量</text>
			</view>
			<view class="divider-v"></view>
			<view class="stat-block">
				<text class="stat-val green">{{ stats.active }}</text>
				<text class="stat-label">在用中</text>
			</view>
			<view class="divider-v"></view>
			<view class="stat-block">
				<text class="stat-val blue">¥{{ stats.totalInvest }}</text>
				<text class="stat-label">总投入</text>
			</view>
		</view>

		<!-- 功能入口列表 -->
		<view class="section-group">
			<view class="group-title">功能</view>

			<view class="menu-card">
				<view class="menu-item" hover-class="item-hover" @click="goWishlist">
					<view class="item-left">
						<view class="item-icon wish">🌟</view>
						<text class="item-label">心愿单</text>
					</view>
					<view class="item-right">
						<text class="item-badge" v-if="stats.wishCount > 0">{{ stats.wishCount }}</text>
						<text class="arrow">›</text>
					</view>
				</view>

				<view class="separator"></view>

				<view class="menu-item" hover-class="item-hover" @click="goAssetList">
					<view class="item-left">
						<view class="item-icon assets">💻</view>
						<text class="item-label">物品柜</text>
					</view>
					<view class="item-right">
						<text class="arrow">›</text>
					</view>
				</view>

				<view class="separator"></view>

				<view class="menu-item" hover-class="item-hover" @click="goSettings">
					<view class="item-left">
						<view class="item-icon settings">⚙️</view>
						<text class="item-label">服务器设置</text>
					</view>
					<view class="item-right">
						<text class="arrow">›</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 数据统计区 -->
		<view class="section-group">
			<view class="group-title">数据</view>

			<view class="menu-card">
				<view class="menu-item" hover-class="item-hover">
					<view class="item-left">
						<view class="item-icon sold">💰</view>
						<text class="item-label">已出售物品</text>
					</view>
					<view class="item-right">
						<text class="item-sub">{{ stats.sold }} 件</text>
						<text class="arrow">›</text>
					</view>
				</view>

				<view class="separator"></view>

				<view class="menu-item" hover-class="item-hover">
					<view class="item-left">
						<view class="item-icon idle">🗜️</view>
						<text class="item-label">闲置物品</text>
					</view>
					<view class="item-right">
						<text class="item-sub">{{ stats.idle }} 件</text>
						<text class="arrow">›</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 关于区 -->
		<view class="section-group">
			<view class="group-title">关于</view>

			<view class="menu-card">
				<view class="menu-item">
					<view class="item-left">
						<view class="item-icon info">ℹ️</view>
						<text class="item-label">关于 DigiLedger</text>
					</view>
					<view class="item-right">
						<text class="item-sub">v0.4.0</text>
					</view>
				</view>
			</view>
		</view>

		<!-- 底部品牌语 -->
		<view class="footer-brand">
			<text class="brand-text">每一件物品，都值得被好好记录。</text>
		</view>
	</view>
</template>

<script>
	import request, { API_PATHS } from '@/utils/request.js';

	export default {
		data() {
			return {
				stats: {
					total: 0,
					active: 0,
					idle: 0,
					sold: 0,
					totalInvest: '0',
					wishCount: 0
				}
			}
		},
		onShow() {
			this.loadStats();
		},
		methods: {
			async loadStats() {
				try {
					// 从物品列表聚合统计，与 /dashboard/stats 接口对接皆可
					const [assets, wishes] = await Promise.allSettled([
						request({ url: '/assets' }),
						request({ url: API_PATHS.wishlist })
					]);

					const list = assets.status === 'fulfilled' ? (assets.value || []) : [];
					const wList = wishes.status === 'fulfilled' ? (wishes.value || []) : [];

					this.stats.total = list.length;
					this.stats.active = list.filter(i => i.status === '使用中').length;
					this.stats.idle = list.filter(i => i.status === '已闲置').length;
					this.stats.sold = list.filter(i => i.status === '已出售').length;
					this.stats.wishCount = wList.length;

					const invest = list.reduce((sum, i) => sum + (i.totalInvest || 0), 0);
					this.stats.totalInvest = invest >= 10000
						? (invest / 10000).toFixed(1) + 'w'
						: invest.toFixed(0);
				} catch (e) {
					console.error('加载统计失败', e);
				}
			},
			goWishlist() {
				uni.switchTab({ url: '/pages/wishlist/list' });
			},
			goAssetList() {
				uni.switchTab({ url: '/pages/asset/list' });
			},
			goSettings() {
				uni.navigateTo({ url: '/pages/settings/index' });
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

	/* ===== 头部 ===== */
	.header {
		padding: calc(var(--status-bar-height) + 20rpx) 40rpx 40rpx;
		background: linear-gradient(180deg, rgba(15, 23, 42, 1) 0%, transparent 100%);
	}

	.avatar-wrap {
		display: flex;
		align-items: center;
		gap: 28rpx;

		.avatar {
			width: 100rpx;
			height: 100rpx;
			border-radius: 28rpx;
			background: linear-gradient(135deg, rgba(59, 130, 246, 0.3), rgba(139, 92, 246, 0.3));
			border: 2rpx solid rgba(139, 92, 246, 0.4);
			@include flex-center;
			box-shadow: 0 8rpx 24rpx rgba(139, 92, 246, 0.2);

			.avatar-icon { font-size: 48rpx; }
		}

		.user-info {
			.username {
				display: block;
				font-size: 38rpx;
				font-weight: 700;
				color: $uni-text-primary;
			}

			.version {
				display: block;
				font-size: 24rpx;
				color: $uni-text-muted;
				margin-top: 6rpx;
			}
		}
	}

	/* ===== 数据统计 ===== */
	.stats-row {
		margin: 0 40rpx 48rpx;
		@include card-base;
		display: flex;
		padding: 40rpx 20rpx;
		background: linear-gradient(135deg, rgba(59, 130, 246, 0.08), rgba(139, 92, 246, 0.06));
		border-color: rgba(139, 92, 246, 0.15);

		.stat-block {
			flex: 1;
			@include flex-center;
			flex-direction: column;
			gap: 10rpx;

			.stat-val {
				font-size: 44rpx;
				font-weight: 700;
				color: $uni-text-primary;
				font-family: $uni-font-tabular;
				letter-spacing: -1rpx;

				&.green { color: $uni-color-success; }
				&.blue  { color: $uni-color-primary; }
			}

			.stat-label { font-size: 24rpx; color: $uni-text-muted; }
		}

		.divider-v {
			width: 1rpx;
			height: 60rpx;
			background: rgba(255, 255, 255, 0.07);
			align-self: center;
		}
	}

	/* ===== 菜单组 ===== */
	.section-group {
		margin: 0 40rpx 32rpx;

		.group-title {
			font-size: 24rpx;
			color: $uni-text-muted;
			text-transform: uppercase;
			letter-spacing: 2rpx;
			margin-bottom: 16rpx;
			padding-left: 8rpx;
		}
	}

	.menu-card {
		@include card-base;
		overflow: hidden;
		padding: 0;
	}

	.menu-item {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 32rpx 28rpx;
		transition: background 0.2s;

		&.item-hover { background: rgba(255, 255, 255, 0.04); }

		.item-left {
			display: flex;
			align-items: center;
			gap: 20rpx;

			.item-icon {
				width: 68rpx;
				height: 68rpx;
				border-radius: 16rpx;
				@include flex-center;
				font-size: 34rpx;

				&.wish   { background: rgba(139, 92, 246, 0.15); }
				&.assets { background: rgba(59, 130, 246, 0.12); }
				&.sold   { background: rgba(0, 255, 157, 0.1); }
				&.idle   { background: rgba(251, 191, 36, 0.1); }
				&.info   { background: rgba(100, 116, 139, 0.15); }
				&.settings { background: rgba(100, 116, 139, 0.12); }
			}

			.item-label {
				font-size: 30rpx;
				color: $uni-text-primary;
				font-weight: 500;
			}
		}

		.item-right {
			display: flex;
			align-items: center;
			gap: 12rpx;

			.item-badge {
				min-width: 44rpx;
				height: 44rpx;
				background: $uni-color-secondary;
				border-radius: 22rpx;
				@include flex-center;
				font-size: 22rpx;
				color: #fff;
				font-weight: 600;
				padding: 0 12rpx;
			}

			.item-sub {
				font-size: 26rpx;
				color: $uni-text-muted;
			}

			.arrow {
				font-size: 36rpx;
				color: $uni-text-muted;
				line-height: 1;
			}
		}
	}

	.separator {
		height: 1rpx;
		background: rgba(255, 255, 255, 0.05);
		margin: 0 28rpx;
	}

	/* ===== 底部品牌 ===== */
	.footer-brand {
		padding: 48rpx 40rpx;
		text-align: center;

		.brand-text {
			font-size: 26rpx;
			color: rgba(100, 116, 139, 0.5);
			font-style: italic;
		}
	}
</style>
