<template>
	<view class="container">
		<!-- Filters Sticky Header -->
		<view class="filter-sticky">
			<scroll-view class="filter-bar" scroll-x="true" show-scrollbar="false">
				<view 
					class="filter-item" 
					:class="{ active: currentFilter === item.value }"
					v-for="item in filters" 
					:key="item.value"
					@click="setFilter(item.value)"
				>
					{{ item.label }}
				</view>
			</scroll-view>
		</view>
		
		<!-- Asset List -->
		<view class="list-section">
			<asset-card 
				v-for="item in filteredList" 
				:key="item.id" 
				:asset="item"
				@select="goDetail"
			></asset-card>
			
			<view v-if="loading" class="loading">
				<text class="loading-dot">...</text>
			</view>
			
			<view v-if="!loading && filteredList.length === 0" class="empty">
				<text class="icon">🔍</text>
				<text class="empty-text">当前状态暂无物品</text>
			</view>
		</view>
	</view>
</template>

<script>
	import request from '@/utils/request.js';
	import AssetCard from '@/components/asset-card.vue';
	
	export default {
		components: {
			AssetCard
		},
		data() {
			return {
				assetList: [],
				loading: false,
				navigating: false,
				currentFilter: 'all',
				filters: [
					{ label: '全部', value: 'all' },
					{ label: '使用中', value: '使用中' },
					{ label: '待出售/闲置', value: 'pending' },
					{ label: '已出售', value: '已出售' }
				]
			}
		},
		computed: {
			filteredList() {
				if (this.currentFilter === 'all') return this.assetList;
				if (this.currentFilter === 'pending') {
					return this.assetList.filter(item => item.status === '待出售' || item.status === '已闲置');
				}
				return this.assetList.filter(item => item.status === this.currentFilter);
			}
		},
		onShow() {
			this.loadData();
		},
		methods: {
			async loadData() {
				this.loading = true;
				try {
					const res = await request({ url: '/assets' });
					this.assetList = res || [];
				} catch (e) {
					console.error("Fetch asset failed", e);
				} finally {
					this.loading = false;
				}
			},
			setFilter(val) {
				this.currentFilter = val;
			},
			goDetail(asset) {
				const id = asset && asset.id;
				if (id === undefined || id === null || id === '') {
					uni.showToast({ title: '物品ID无效', icon: 'none' });
					return;
				}
				if (this.navigating) return;
				this.navigating = true;
				uni.navigateTo({
					url: `/pages/asset/detail?id=${id}`,
					complete: () => {
						this.navigating = false;
					}
				});
			}
		}
	}
</script>

<style lang="scss">
	.container {
		padding-bottom: env(safe-area-inset-bottom);
	}
	
	.filter-sticky {
		position: sticky;
		top: 0;
		z-index: 10;
		background: rgba(15, 23, 42, 0.85);
		backdrop-filter: blur(12px);
		padding: 32rpx 0;
	}
	
	.filter-bar {
		white-space: nowrap;
		padding: 0 40rpx;
		/* Hack to allow scrolling margin without cutting off shadow on right side */
		box-sizing: border-box;
		
		.filter-item {
			display: inline-block;
			padding: 16rpx 36rpx;
			background: $uni-bg-surface;
			border-radius: 40rpx;
			margin-right: 20rpx;
			font-size: 28rpx;
			font-weight: 500;
			color: $uni-text-secondary;
			border: 1px solid rgba(255, 255, 255, 0.05);
			transition: all 0.25s ease;
			
			&:last-child {
				margin-right: 80rpx;
			}
			
			&.active {
				background: $uni-color-primary-dim;
				color: $uni-color-primary;
				border-color: rgba(59, 130, 246, 0.4);
				box-shadow: 0 4rpx 15rpx rgba(59, 130, 246, 0.15);
			}
		}
	}
	
	.list-section {
		padding: 20rpx 40rpx 100rpx 40rpx;
		min-height: 60vh;
	}
	
	.loading, .empty {
		padding: 120rpx 0;
		@include flex-center;
		flex-direction: column;
		
		.loading-dot {
			font-size: 40rpx;
			color: $uni-color-primary;
			letter-spacing: 4rpx;
			animation: breathe 1.5s infinite;
		}
		
		.icon {
			font-size: 80rpx;
			margin-bottom: 24rpx;
			opacity: 0.4;
		}
		
		.empty-text {
			font-size: 30rpx;
			color: $uni-text-muted;
			font-weight: 500;
		}
	}
	
	@keyframes breathe {
		0%, 100% { opacity: 0.4; }
		50% { opacity: 1; }
	}
</style>
