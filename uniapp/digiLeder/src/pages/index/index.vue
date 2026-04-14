<template>
	<view class="container">
		<!-- KPI Header / Dark Neon Dashboard -->
		<view class="header-section">
			<view class="title-bar">
				<text class="app-name">工作台</text>
				<view class="user-avatar">
					<!-- 使用默认或者用户头像 -->
					<text class="icon">⚡</text>
				</view>
			</view>
			
			<scroll-view class="stats-scroll" scroll-x="true" show-scrollbar="false">
				<!-- 总投入卡片 (Primary Highlighting) -->
				<view class="stat-card primary" hover-class="card-hover-active">
					<view class="stat-inner">
						<text class="label">总投入</text>
						<text class="value">¥{{ totalValue }}</text>
						<text class="sub">共有 {{ assetList.length }} 件在持物品</text>
					</view>
				</view>
				
				<!-- 近期日均卡片 -->
				<view class="stat-card" hover-class="card-hover-active">
					<view class="stat-inner">
						<text class="label">资产使用状态</text>
						<view class="ratio-bar">
							<text class="value highlight">{{ activeCount }}</text>
							<text class="value divider">/</text>
							<text class="value idle">{{ idleCount }}</text>
						</view>
						<text class="sub">在用与闲置数</text>
					</view>
				</view>
			</scroll-view>
		</view>
		
		<!-- 近期动态横向卷轴 -->
		<view class="section-title">
			<text>近期新增</text>
			<text class="view-all" @click="goAll">查看全部</text>
		</view>
		<scroll-view class="recent-scroll" scroll-x="true" show-scrollbar="false">
			<view class="recent-item" hover-class="card-hover-active" v-for="item in recentAssets" :key="item.id" @click="goDetail(item)">
				<image class="recent-thumb" v-if="item.coverImageUrl" :src="item.coverImageUrl" mode="aspectFill"></image>
				<view class="recent-placeholder" v-else>📱</view>
				<text class="recent-name">{{ item.name }}</text>
				<text class="recent-cost">¥{{ item.totalInvest || 0 }}</text>
			</view>
			<view class="empty-state" v-if="recentAssets.length === 0">暂无数据</view>
		</scroll-view>
		
		<view class="fab-btn" hover-class="card-hover-active" @click="goAdd">
			<text class="icon">+</text>
		</view>
	</view>
</template>

<script>
	import request from '@/utils/request.js';
	
	export default {
		data() {
			return {
				assetList: [],
				loading: false
			}
		},
		computed: {
			totalValue() {
				return this.assetList.reduce((sum, item) => sum + (item.totalInvest || 0), 0).toFixed(0);
			},
			activeCount() {
				return this.assetList.filter(i => i.status === '使用中').length;
			},
			idleCount() {
				return this.assetList.filter(i => i.status === '已闲置').length;
			},
			recentAssets() {
				// 获取最近添加或修改的5个设备
				return [...this.assetList].reverse().slice(0, 5);
			}
		},
		onShow() {
			this.loadData();
		},
		methods: {
			async loadData() {
				this.loading = true;
				try {
					// 根据后台接口规范返回资产列表
					const res = await request({ url: '/assets' });
					this.assetList = res || [];
				} catch (e) {
					console.error(e);
				} finally {
					this.loading = false;
				}
			},
			goDetail(asset) {
				uni.navigateTo({
					url: `/pages/asset/detail?id=${asset.id}`
				});
			},
			goAll() {
				// 跳转到物品柜
				uni.switchTab({
					url: '/pages/asset/list'
				});
			},
			goAdd() {
				uni.navigateTo({
					url: '/pages/asset/add'
				});
			}
		}
	}
</script>

<style lang="scss">
	.container {
		padding: 40rpx 0 160rpx 0; /* 为悬浮按钮留出空间 */
	}
	
	.title-bar {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 0 40rpx;
		margin-bottom: 48rpx;
		
		.app-name {
			font-size: 48rpx;
			font-weight: 700;
			color: $uni-text-primary;
			letter-spacing: -1rpx;
		}
		
		.user-avatar {
			width: 72rpx;
			height: 72rpx;
			border-radius: 20rpx;
			background: $uni-bg-surface-light;
			@include flex-center;
			border: 1px solid rgba(255, 255, 255, 0.1);
			box-shadow: 0 4rpx 15rpx rgba(0,0,0,0.3);
			
			.icon { font-size: 32rpx; }
		}
	}
	
	.stats-scroll {
		white-space: nowrap;
		padding: 0 40rpx;
		margin-bottom: 64rpx;
		
		/* 解决最后一张卡片右侧遮挡问题 */
		box-sizing: border-box;
		
		.stat-card {
			display: inline-flex;
			width: 480rpx;
			height: 240rpx;
			margin-right: 32rpx;
			vertical-align: top;
			@include card-base;
			padding: 40rpx;
			
			/* Hack for scroll spacing on last item */
			&:last-child {
				margin-right: 80rpx;
			}
			
			&.primary {
				background: linear-gradient(135deg, rgba(59, 130, 246, 0.15) 0%, rgba(139, 92, 246, 0.1) 100%);
				border: 1px solid rgba(59, 130, 246, 0.3);
				box-shadow: 0 10rpx 30rpx -10rpx rgba(59, 130, 246, 0.4);
			}
			
			.stat-inner {
				display: flex;
				flex-direction: column;
				height: 100%;
			}
			
			.label {
				font-size: 28rpx;
				color: $uni-text-secondary;
				font-weight: 500;
				margin-bottom: 16rpx;
			}
			
			.value {
				font-size: 64rpx;
				font-weight: 700;
				color: $uni-text-primary;
				font-family: $uni-font-tabular;
				letter-spacing: -2rpx;
				
				&.highlight { color: $uni-color-success; }
				&.idle { color: $uni-text-muted; }
				&.divider { font-size: 40rpx; margin: 0 16rpx; color: $uni-border-color; }
			}
			
			.ratio-bar {
				display: flex;
				align-items: baseline;
			}
			
			.sub {
				margin-top: auto;
				font-size: 24rpx;
				color: $uni-text-muted;
			}
		}
	}
	
	.section-title {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 0 40rpx;
		margin-bottom: 32rpx;
		font-size: 32rpx;
		font-weight: 600;
		color: $uni-text-primary;
		
		.view-all {
			font-size: 26rpx;
			font-weight: 400;
			color: $uni-color-primary;
		}
	}
	
	.recent-scroll {
		white-space: nowrap;
		padding: 0 40rpx;
		
		.recent-item {
			display: inline-flex;
			flex-direction: column;
			width: 220rpx;
			margin-right: 32rpx;
			vertical-align: top;
			
			&:last-child {
				margin-right: 80rpx;
			}
			
			.recent-thumb, .recent-placeholder {
				width: 220rpx;
				height: 220rpx;
				border-radius: $uni-radius-lg;
				margin-bottom: 24rpx;
				background: $uni-bg-surface;
				border: 1px solid rgba(255, 255, 255, 0.05);
			}
			
			.recent-placeholder {
				@include flex-center;
				font-size: 80rpx;
			}
			
			.recent-name {
				font-size: 28rpx;
				color: $uni-text-primary;
				font-weight: 500;
				margin-bottom: 8rpx;
				overflow: hidden;
				text-overflow: ellipsis;
				white-space: nowrap;
			}
			
			.recent-cost {
				font-size: 24rpx;
				color: $uni-color-primary;
				font-family: $uni-font-tabular;
				font-weight: 600;
			}
		}
	}
	
	.empty-state {
		padding: 40rpx 0;
		color: $uni-text-muted;
		font-size: 28rpx;
		text-align: left;
	}
	
	.fab-btn {
		position: fixed;
		bottom: 180rpx; /* H5与兼容tabbar高度 */
		right: 40rpx;
		width: 112rpx;
		height: 112rpx;
		border-radius: 32rpx; /* Squircle */
		background: linear-gradient(135deg, $uni-color-primary 0%, $uni-color-secondary 100%);
		@include flex-center;
		box-shadow: 0 10rpx 30rpx rgba(139, 92, 246, 0.4);
		z-index: 99;
		transition: transform 0.2s ease, filter 0.2s ease;
		
		.icon {
			font-size: 64rpx;
			font-weight: 300;
			color: #fff;
			margin-top: -6rpx;
		}
	}
</style>
