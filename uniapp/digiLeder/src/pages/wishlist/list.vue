<template>
	<view class="container">
		<!-- 自定义顶部 -->
		<view class="header-section">
			<view class="title-bar">
				<view>
					<text class="app-name">心愿单</text>
					<text class="subtitle">{{ list.length }} 件待入手</text>
				</view>
				<view class="header-btn" hover-class="card-hover-active" @click="goAdd">
					<text class="icon">+</text>
				</view>
			</view>
		</view>

		<!-- 空状态 -->
		<view v-if="!loading && list.length === 0" class="empty-state">
			<text class="empty-icon">🌟</text>
			<text class="empty-title">心愿单还是空的</text>
			<text class="empty-sub">记录你想入手的好物，随时追踪</text>
			<view class="btn-add-first" hover-class="card-hover-active" @click="goAdd">
				<text>添加第一个心愿</text>
			</view>
		</view>

		<!-- 加载中 -->
		<view v-if="loading" class="loading">
			<text class="loading-dot">···</text>
		</view>

		<!-- 列表 -->
		<view class="list-body" v-if="!loading && list.length > 0">
			<view
				class="wish-card"
				hover-class="card-hover-active"
				v-for="item in list"
				:key="item.id"
				@click="goDetail(item)"
			>
				<!-- 优先级指示条 -->
				<view class="priority-bar" :class="priorityClass(item.priority)"></view>

				<view class="card-inner">
					<view class="card-left">
						<view class="wish-icon">
							<text>{{ categoryIcon(item.category) }}</text>
						</view>
					</view>

					<view class="card-content">
						<view class="name-row">
							<text class="name">{{ item.name }}</text>
							<view class="priority-badge" :class="priorityClass(item.priority)">
								<text>{{ priorityLabel(item.priority) }}</text>
							</view>
						</view>

						<view class="meta-row">
							<text class="platform" v-if="item.targetPlatform">{{ item.targetPlatform }}</text>
							<text class="notes-preview" v-if="item.notes">{{ item.notes }}</text>
						</view>

						<view class="price-row">
							<view class="budget-chip">
								<text class="budget-label">预算</text>
								<text class="budget-val">¥{{ item.budget || '不限' }}</text>
							</view>
							<view class="actions">
								<view class="action-btn convert" hover-class="card-hover-active" @click.stop="convertItem(item)">
									<text>转为物品</text>
								</view>
								<view class="action-btn delete" hover-class="card-hover-active" @click.stop="deleteItem(item)">
									<text>删除</text>
								</view>
							</view>
						</view>
					</view>
				</view>
			</view>
		</view>

		<!-- FAB -->
		<view class="fab-btn" hover-class="card-hover-active" @click="goAdd" v-if="list.length > 0">
			<text class="icon">+</text>
		</view>
	</view>
</template>

<script>
	import request from '@/utils/request.js';

	export default {
		data() {
			return {
				list: [],
				loading: false
			}
		},
		onShow() {
			this.loadData();
		},
		methods: {
			async loadData() {
				this.loading = true;
				try {
					const res = await request({ url: '/wishlists' });
					this.list = res || [];
				} catch (e) {
					console.error('获取心愿单失败', e);
				} finally {
					this.loading = false;
				}
			},
			goAdd() {
				uni.navigateTo({ url: '/pages/wishlist/add' });
			},
			goDetail(item) {
				// 当前版本心愿单无独立详情，点击直连编辑（可扩展）
			},
			async convertItem(item) {
				uni.showModal({
					title: '一键转为物品',
					content: `将「${item.name}」加入物品柜？`,
					success: async (res) => {
						if (res.confirm) {
							try {
								await request({
									url: `/wishlists/${item.id}/convert`,
									method: 'POST'
								});
								uni.showToast({ title: '已加入物品柜', icon: 'success' });
								this.loadData();
							} catch (e) {
								console.error(e);
							}
						}
					}
				});
			},
			async deleteItem(item) {
				uni.showModal({
					title: '确认删除',
					content: `删除「${item.name}」？`,
					confirmColor: '#ef4444',
					success: async (res) => {
						if (res.confirm) {
							try {
								await request({
									url: `/wishlists/${item.id}`,
									method: 'DELETE'
								});
								uni.showToast({ title: '已删除', icon: 'success' });
								this.loadData();
							} catch (e) {
								console.error(e);
							}
						}
					}
				});
			},
			priorityClass(p) {
				return { 'low': 'p-low', 'medium': 'p-medium', 'high': 'p-high' }[p] || 'p-low';
			},
			priorityLabel(p) {
				return { 'low': '普通', 'medium': '想要', 'high': '必入' }[p] || '普通';
			},
			categoryIcon(cat) {
				const map = {
					'手机': '📱', '电脑': '💻', '相机': '📷', '耳机': '🎧',
					'平板': '📟', '手表': '⌚', '配件': '🔧'
				};
				return map[cat] || '🌟';
			}
		}
	}
</script>

<style lang="scss">
	.container {
		min-height: 100vh;
		background: $uni-bg-base;
		padding-bottom: 180rpx;
	}

	.header-section {
		padding: calc(var(--status-bar-height) + 20rpx) 40rpx 32rpx;
		background: linear-gradient(180deg, rgba(15,23,42,1) 0%, rgba(15,23,42,0) 100%);
	}

	.title-bar {
		display: flex;
		justify-content: space-between;
		align-items: center;

		.app-name {
			font-size: 48rpx;
			font-weight: 700;
			color: $uni-text-primary;
			display: block;
		}

		.subtitle {
			font-size: 26rpx;
			color: $uni-text-muted;
			display: block;
			margin-top: 4rpx;
		}

		.header-btn {
			width: 72rpx;
			height: 72rpx;
			border-radius: 20rpx;
			background: linear-gradient(135deg, $uni-color-secondary, $uni-color-primary);
			@include flex-center;
			box-shadow: 0 4rpx 20rpx rgba(139, 92, 246, 0.4);

			.icon {
				font-size: 40rpx;
				color: #fff;
				margin-top: -4rpx;
			}
		}
	}

	/* 空状态 */
	.empty-state {
		padding: 120rpx 60rpx;
		@include flex-center;
		flex-direction: column;
		gap: 20rpx;

		.empty-icon { font-size: 100rpx; }

		.empty-title {
			font-size: 36rpx;
			font-weight: 600;
			color: $uni-text-primary;
		}

		.empty-sub {
			font-size: 28rpx;
			color: $uni-text-muted;
		}

		.btn-add-first {
			margin-top: 20rpx;
			padding: 24rpx 60rpx;
			background: linear-gradient(135deg, $uni-color-primary, $uni-color-secondary);
			border-radius: 48rpx;
			font-size: 28rpx;
			color: #fff;
			font-weight: 600;
			box-shadow: 0 8rpx 30rpx rgba(59, 130, 246, 0.35);
		}
	}

	/* Loading */
	.loading {
		padding: 120rpx 0;
		@include flex-center;

		.loading-dot {
			font-size: 48rpx;
			color: $uni-color-primary;
			animation: breathe 1.5s infinite;
			letter-spacing: 4rpx;
		}
	}

	@keyframes breathe {
		0%, 100% { opacity: 0.3; }
		50% { opacity: 1; }
	}

	/* 列表 */
	.list-body {
		padding: 0 40rpx;
	}

	.wish-card {
		@include card-base;
		margin-bottom: 28rpx;
		display: flex;
		overflow: hidden;
		padding: 0;
		box-shadow: 0 8rpx 30rpx -8rpx rgba(0, 0, 0, 0.4);
	}

	/* 优先级左侧指示条 */
	.priority-bar {
		width: 8rpx;
		flex-shrink: 0;

		&.p-low    { background: rgba(100,116,139,0.6); }
		&.p-medium { background: linear-gradient(180deg, $uni-color-primary, rgba(59,130,246,0.4)); }
		&.p-high   { background: linear-gradient(180deg, $uni-color-secondary, rgba(139,92,246,0.4)); }
	}

	.card-inner {
		flex: 1;
		display: flex;
		padding: 28rpx 28rpx 28rpx 24rpx;
		gap: 24rpx;
	}

	.card-left {
		.wish-icon {
			width: 100rpx;
			height: 100rpx;
			border-radius: $uni-radius-md;
			background: rgba(255, 255, 255, 0.04);
			border: 1px solid rgba(255, 255, 255, 0.08);
			@include flex-center;
			font-size: 52rpx;
		}
	}

	.card-content {
		flex: 1;
		min-width: 0;
		display: flex;
		flex-direction: column;
		gap: 14rpx;
	}

	.name-row {
		display: flex;
		align-items: center;
		gap: 16rpx;

		.name {
			flex: 1;
			font-size: 32rpx;
			font-weight: 600;
			color: $uni-text-primary;
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
		}

		.priority-badge {
			padding: 4rpx 18rpx;
			border-radius: 24rpx;
			font-size: 22rpx;
			font-weight: 600;
			flex-shrink: 0;

			&.p-low    { background: rgba(100,116,139,0.2); color: $uni-text-muted; }
			&.p-medium { background: rgba(59,130,246,0.15); color: $uni-color-primary; }
			&.p-high   {
				background: linear-gradient(135deg, rgba(139,92,246,0.2), rgba(236,72,153,0.2));
				color: $uni-color-secondary;
			}
		}
	}

	.meta-row {
		display: flex;
		gap: 16rpx;
		flex-wrap: wrap;

		.platform {
			font-size: 24rpx;
			color: $uni-text-secondary;
			background: rgba(255,255,255,0.05);
			padding: 4rpx 16rpx;
			border-radius: 8rpx;
		}

		.notes-preview {
			font-size: 24rpx;
			color: $uni-text-muted;
			overflow: hidden;
			text-overflow: ellipsis;
			white-space: nowrap;
			flex: 1;
		}
	}

	.price-row {
		display: flex;
		justify-content: space-between;
		align-items: center;

		.budget-chip {
			display: flex;
			align-items: baseline;
			gap: 8rpx;

			.budget-label {
				font-size: 22rpx;
				color: $uni-text-muted;
			}

			.budget-val {
				font-size: 36rpx;
				font-weight: 700;
				color: $uni-color-success;
				font-family: $uni-font-tabular;
			}
		}

		.actions {
			display: flex;
			gap: 16rpx;

			.action-btn {
				padding: 10rpx 24rpx;
				border-radius: 24rpx;
				font-size: 24rpx;
				font-weight: 600;
				transition: all 0.2s;

				&.convert {
					background: rgba(59,130,246,0.12);
					color: $uni-color-primary;
					border: 1px solid rgba(59,130,246,0.3);
				}

				&.delete {
					background: rgba(239,68,68,0.1);
					color: $uni-color-danger;
					border: 1px solid rgba(239,68,68,0.2);
				}
			}
		}
	}

	/* FAB */
	.fab-btn {
		position: fixed;
		bottom: 180rpx;
		right: 40rpx;
		width: 112rpx;
		height: 112rpx;
		border-radius: 32rpx;
		background: linear-gradient(135deg, $uni-color-secondary 0%, $uni-color-primary 100%);
		@include flex-center;
		box-shadow: 0 10rpx 30rpx rgba(139, 92, 246, 0.4);
		z-index: 99;

		.icon {
			font-size: 64rpx;
			font-weight: 300;
			color: #fff;
			margin-top: -6rpx;
		}
	}
</style>
