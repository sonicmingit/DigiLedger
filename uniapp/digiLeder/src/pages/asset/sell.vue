<template>
	<view class="container">
		<!-- Step 指示器 -->
		<view class="step-header">
			<view class="step-dots">
				<view class="dot" :class="{ active: step >= 1, done: step > 1 }">
					<text>{{ step > 1 ? '✓' : '1' }}</text>
				</view>
				<view class="step-line" :class="{ active: step > 1 }"></view>
				<view class="dot" :class="{ active: step >= 2, done: step > 2 }">
					<text>{{ step > 2 ? '✓' : '2' }}</text>
				</view>
				<view class="step-line" :class="{ active: step > 2 }"></view>
				<view class="dot" :class="{ active: step >= 3 }">
					<text>3</text>
				</view>
			</view>
			<view class="step-labels">
				<text :class="{ 'label-active': step === 1 }">确认物品</text>
				<text :class="{ 'label-active': step === 2 }">出售信息</text>
				<text :class="{ 'label-active': step === 3 }">确认提交</text>
			</view>
		</view>

		<!-- Step 1: 物品确认 -->
		<view class="step-content" v-if="step === 1">
			<view class="step-title">确认出售的物品</view>

			<view v-if="asset" class="asset-confirm-card">
				<view class="asset-thumb">
					<image v-if="asset.coverImageUrl" :src="asset.coverImageUrl" mode="aspectFill"></image>
					<view v-else class="thumb-placeholder"><text>💻</text></view>
				</view>
				<view class="asset-meta">
					<text class="asset-name">{{ asset.name }}</text>
					<text class="asset-brand" v-if="asset.brandName">{{ asset.brandName }}</text>
					<view class="asset-stats">
						<view class="stat">
							<text class="stat-label">购入价格</text>
							<text class="stat-val">¥{{ formatPrice(asset.totalInvest) }}</text>
						</view>
						<view class="stat">
							<text class="stat-label">已使用</text>
							<text class="stat-val blue">{{ asset.useDays }} 天</text>
						</view>
					</view>
				</view>
			</view>
			<view v-else class="loading"><text>加载中...</text></view>

			<view class="info-tip">
				<text class="tip-icon">💡</text>
				<text class="tip-text">接下来填写出售价格和各项费用，系统将自动计算净利润。</text>
			</view>
		</view>

		<!-- Step 2: 出售信息 -->
		<view class="step-content" v-if="step === 2">
			<view class="step-title">填写出售信息</view>

			<view class="form-card primary-card">
				<view class="form-item row-mode">
					<text class="label">售价</text>
					<view class="price-input">
						<text class="currency">¥</text>
						<input
							class="input seamless money"
							type="digit"
							v-model="sellForm.salePrice"
							placeholder="0.00"
							placeholder-style="color: rgba(255,255,255,0.2)"
						/>
					</view>
				</view>
			</view>

			<view class="form-card">
				<view class="section-title">扣除费用（可选）</view>

				<view class="form-item row-mode thin">
					<view class="fee-label">
						<text class="fee-icon">📦</text>
						<text class="label">快递费</text>
					</view>
					<view class="price-input">
						<text class="currency sm">¥</text>
						<input
							class="input seamless fee-input"
							type="digit"
							v-model="sellForm.shippingFee"
							placeholder="0"
							placeholder-style="color: rgba(255,255,255,0.2)"
						/>
					</view>
				</view>

				<view class="form-item row-mode thin divider">
					<view class="fee-label">
						<text class="fee-icon">💳</text>
						<text class="label">平台手续费</text>
					</view>
					<view class="price-input">
						<text class="currency sm">¥</text>
						<input
							class="input seamless fee-input"
							type="digit"
							v-model="sellForm.platformFee"
							placeholder="0"
							placeholder-style="color: rgba(255,255,255,0.2)"
						/>
					</view>
				</view>

				<view class="form-item row-mode thin divider">
					<view class="fee-label">
						<text class="fee-icon">📝</text>
						<text class="label">其他费用</text>
					</view>
					<view class="price-input">
						<text class="currency sm">¥</text>
						<input
							class="input seamless fee-input"
							type="digit"
							v-model="sellForm.otherFee"
							placeholder="0"
							placeholder-style="color: rgba(255,255,255,0.2)"
						/>
					</view>
				</view>
			</view>

			<view class="form-card">
				<view class="section-title">出售来源</view>
				<view class="pill-group">
					<view
						class="pill"
						:class="{ active: sellForm.salePlatform === p }"
						v-for="p in platforms"
						:key="p"
						@click="sellForm.salePlatform = p"
					>{{ p }}</view>
				</view>
			</view>
		</view>

		<!-- Step 3: 汇总预览 -->
		<view class="step-content" v-if="step === 3">
			<view class="step-title">出售汇总确认</view>

			<view class="summary-card">
				<view class="summary-row">
					<text class="s-label">物品名称</text>
					<text class="s-val">{{ asset ? asset.name : '' }}</text>
				</view>
				<view class="summary-row">
					<text class="s-label">原始投入</text>
					<text class="s-val">¥{{ asset ? formatPrice(asset.totalInvest) : '0.00' }}</text>
				</view>
				<view class="summary-row">
					<text class="s-label">实际售价</text>
					<text class="s-val green">¥{{ formatPrice(sellForm.salePrice) }}</text>
				</view>

				<view class="divider-line"></view>

				<view class="summary-row" v-if="Number(sellForm.shippingFee) > 0">
					<text class="s-label">快递费</text>
					<text class="s-val red">-¥{{ formatPrice(sellForm.shippingFee) }}</text>
				</view>
				<view class="summary-row" v-if="Number(sellForm.platformFee) > 0">
					<text class="s-label">平台手续费</text>
					<text class="s-val red">-¥{{ formatPrice(sellForm.platformFee) }}</text>
				</view>
				<view class="summary-row" v-if="Number(sellForm.otherFee) > 0">
					<text class="s-label">其他费用</text>
					<text class="s-val red">-¥{{ formatPrice(sellForm.otherFee) }}</text>
				</view>

				<view class="divider-line"></view>

				<!-- 净利润核心展示 -->
				<view class="net-profit-block" :class="netProfit >= 0 ? 'profit' : 'loss'">
					<text class="net-label">净{{ netProfit >= 0 ? '收益' : '亏损' }}</text>
					<text class="net-val">{{ netProfit >= 0 ? '+' : '' }}¥{{ formatPrice(Math.abs(netProfit)) }}</text>
				</view>

				<view class="summary-row platform-row" v-if="sellForm.salePlatform">
					<text class="s-label">出售渠道</text>
					<text class="s-val">{{ sellForm.salePlatform }}</text>
				</view>
			</view>

			<view class="warn-tip" v-if="netProfit < 0">
				<text>⚠️ 出售后将亏损 ¥{{ formatPrice(Math.abs(netProfit)) }}，继续？</text>
			</view>
		</view>

		<!-- 底部操作区 -->
		<view class="footer-action">
			<view class="btn-group">
				<button class="btn-back" hover-class="btn-back-active" @click="prevStep" v-if="step > 1">
					上一步
				</button>
				<button class="btn-back" hover-class="btn-back-active" @click="goBack" v-else>
					取消
				</button>
				<button
					class="btn-next"
					hover-class="btn-next-active"
					@click="nextStep"
					:loading="submitting"
					v-if="step < 3"
				>
					下一步
				</button>
				<button
					class="btn-next confirm"
					hover-class="btn-next-active"
					@click="submit"
					:loading="submitting"
					v-else
				>
					确认出售
				</button>
			</view>
		</view>
	</view>
</template>

<script>
	import request from '@/utils/request.js';

	export default {
		data() {
			return {
				id: null,
				asset: null,
				step: 1,
				submitting: false,
				sellForm: {
					salePrice: '',
					shippingFee: '',
					platformFee: '',
					otherFee: '',
					salePlatform: ''
				},
				platforms: ['闲鱼', '京东', '淘宝', '微信', '转让', '其他']
			}
		},
		computed: {
			netProfit() {
				const sale = Number(this.sellForm.salePrice) || 0;
				const shipping = Number(this.sellForm.shippingFee) || 0;
				const platform = Number(this.sellForm.platformFee) || 0;
				const other = Number(this.sellForm.otherFee) || 0;
				const cost = this.asset ? (this.asset.totalInvest || 0) : 0;
				return sale - shipping - platform - other - cost;
			}
		},
		onLoad(options) {
			if (options.id) {
				this.id = options.id;
				this.loadAsset();
			}
		},
		methods: {
			async loadAsset() {
				try {
					const res = await request({ url: `/assets/${this.id}` });
					this.asset = res;
				} catch (e) {
					console.error(e);
					uni.showToast({ title: '加载失败', icon: 'none' });
				}
			},
			nextStep() {
				if (this.step === 2) {
					if (!this.sellForm.salePrice || Number(this.sellForm.salePrice) <= 0) {
						return uni.showToast({ title: '请填写出售价格', icon: 'none' });
					}
				}
				this.step++;
			},
			prevStep() {
				this.step--;
			},
			goBack() {
				uni.navigateBack();
			},
			async submit() {
				this.submitting = true;
				try {
					await request({
						url: `/assets/${this.id}/sell`,
						method: 'POST',
						data: {
							salePrice: Number(this.sellForm.salePrice),
							shippingFee: Number(this.sellForm.shippingFee) || 0,
							platformFee: Number(this.sellForm.platformFee) || 0,
							otherFee: Number(this.sellForm.otherFee) || 0,
							salePlatform: this.sellForm.salePlatform,
							netProfit: this.netProfit
						}
					});
					uni.showToast({ title: '出售记录完成！', icon: 'success' });
					setTimeout(() => {
						// 回到物品柜
						uni.switchTab({ url: '/pages/asset/list' });
					}, 1200);
				} catch (e) {
					console.error(e);
				} finally {
					this.submitting = false;
				}
			},
			formatPrice(val) {
				const v = typeof val === 'number' ? val : parseFloat(val) || 0;
				return v.toFixed(2);
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

	/* ===== Step 指示器 ===== */
	.step-header {
		padding: 40rpx 60rpx 32rpx;
		background: $uni-bg-surface;
		border-bottom: 1rpx solid rgba(255, 255, 255, 0.05);
	}

	.step-dots {
		display: flex;
		align-items: center;
		margin-bottom: 20rpx;

		.dot {
			width: 48rpx;
			height: 48rpx;
			border-radius: 50%;
			background: rgba(255, 255, 255, 0.08);
			border: 2rpx solid rgba(255, 255, 255, 0.12);
			@include flex-center;
			font-size: 22rpx;
			color: $uni-text-muted;
			transition: all 0.3s;

			&.active {
				background: rgba(59, 130, 246, 0.2);
				border-color: $uni-color-primary;
				color: $uni-color-primary;
			}

			&.done {
				background: $uni-color-success;
				border-color: $uni-color-success;
				color: #000;
				font-weight: bold;
			}
		}

		.step-line {
			flex: 1;
			height: 2rpx;
			background: rgba(255, 255, 255, 0.08);
			margin: 0 12rpx;
			transition: background 0.3s;

			&.active { background: $uni-color-primary; }
		}
	}

	.step-labels {
		display: flex;
		justify-content: space-between;
		padding: 0 8rpx;

		text {
			font-size: 22rpx;
			color: $uni-text-muted;
			transition: color 0.3s;

			&.label-active {
				color: $uni-color-primary;
				font-weight: 600;
			}
		}
	}

	/* ===== 通用步骤内容 ===== */
	.step-content {
		padding: 40rpx 40rpx 0;
	}

	.step-title {
		font-size: 36rpx;
		font-weight: 700;
		color: $uni-text-primary;
		margin-bottom: 32rpx;
	}

	/* ===== Step 1: 物品确认 ===== */
	.asset-confirm-card {
		@include card-base;
		display: flex;
		gap: 28rpx;
		margin-bottom: 32rpx;

		.asset-thumb {
			width: 160rpx;
			height: 160rpx;
			border-radius: $uni-radius-md;
			overflow: hidden;
			flex-shrink: 0;

			image { width: 100%; height: 100%; }

			.thumb-placeholder {
				width: 100%;
				height: 100%;
				background: rgba(255, 255, 255, 0.04);
				@include flex-center;
				font-size: 64rpx;
			}
		}

		.asset-meta {
			flex: 1;
			display: flex;
			flex-direction: column;
			justify-content: space-between;

			.asset-name {
				font-size: 34rpx;
				font-weight: 600;
				color: $uni-text-primary;
				display: block;
			}

			.asset-brand {
				font-size: 24rpx;
				color: $uni-text-muted;
				display: block;
			}

			.asset-stats {
				display: flex;
				gap: 32rpx;

				.stat {
					.stat-label { font-size: 22rpx; color: $uni-text-muted; display: block; }
					.stat-val {
						font-size: 32rpx;
						font-weight: 700;
						color: $uni-text-primary;
						font-family: $uni-font-tabular;
						display: block;

						&.blue { color: $uni-color-primary; }
					}
				}
			}
		}
	}

	.info-tip {
		background: rgba(59, 130, 246, 0.06);
		border: 1rpx solid rgba(59, 130, 246, 0.2);
		border-radius: $uni-radius-md;
		padding: 24rpx;
		display: flex;
		gap: 16rpx;
		align-items: flex-start;

		.tip-icon { font-size: 32rpx; flex-shrink: 0; }
		.tip-text { font-size: 26rpx; color: $uni-text-secondary; line-height: 1.6; }
	}

	/* ===== Step 2: 表单 ===== */
	.form-card {
		@include card-base;
		margin-bottom: 28rpx;
		padding: 32rpx;

		&.primary-card {
			background: linear-gradient(135deg, rgba(59, 130, 246, 0.08), rgba(15, 23, 42, 0));
			border-color: rgba(59, 130, 246, 0.2);
			padding: 16rpx 32rpx;
		}
	}

	.section-title {
		font-size: 24rpx;
		color: $uni-text-muted;
		text-transform: uppercase;
		letter-spacing: 2rpx;
		margin-bottom: 24rpx;
	}

	.form-item.row-mode {
		display: flex;
		align-items: center;
		padding: 28rpx 0;

		&.thin { padding: 20rpx 0; }
		&.divider { border-top: 1rpx solid rgba(255, 255, 255, 0.05); }

		.label {
			font-size: 30rpx;
			font-weight: 500;
			color: $uni-text-secondary;
		}

		.fee-label {
			display: flex;
			align-items: center;
			gap: 12rpx;
			flex: 1;

			.fee-icon { font-size: 28rpx; }
			.label { font-size: 28rpx; }
		}

		.price-input {
			display: flex;
			align-items: center;
			justify-content: flex-end;
			gap: 6rpx;

			.currency {
				font-size: 32rpx;
				color: $uni-color-primary;
				font-weight: bold;

				&.sm { font-size: 26rpx; color: $uni-text-secondary; }
			}
		}

		.seamless {
			background: transparent;
			border: none;
			text-align: right;
			color: $uni-text-primary;

			&.money {
				font-size: 52rpx;
				font-family: $uni-font-tabular;
				color: $uni-color-primary;
				font-weight: bold;
				width: 300rpx;
			}

			&.fee-input {
				font-size: 32rpx;
				font-family: $uni-font-tabular;
				color: $uni-text-secondary;
				width: 160rpx;
			}
		}
	}

	.pill-group {
		display: flex;
		flex-wrap: wrap;
		gap: 20rpx;

		.pill {
			padding: 14rpx 28rpx;
			border-radius: 40rpx;
			background: rgba(255, 255, 255, 0.04);
			border: 1px solid rgba(255, 255, 255, 0.08);
			font-size: 26rpx;
			color: $uni-text-secondary;
			transition: all 0.2s;

			&.active {
				background: rgba(59, 130, 246, 0.12);
				border-color: $uni-color-primary;
				color: $uni-color-primary;
			}
		}
	}

	/* ===== Step 3: 汇总 ===== */
	.summary-card {
		@include card-base;
		padding: 40rpx;
	}

	.summary-row {
		display: flex;
		justify-content: space-between;
		align-items: center;
		padding: 18rpx 0;

		&.platform-row { margin-top: 12rpx; }

		.s-label { font-size: 28rpx; color: $uni-text-secondary; }

		.s-val {
			font-size: 30rpx;
			color: $uni-text-primary;
			font-family: $uni-font-tabular;
			font-weight: 600;

			&.green { color: $uni-color-success; }
			&.red { color: $uni-color-danger; }
		}
	}

	.divider-line {
		height: 1rpx;
		background: rgba(255, 255, 255, 0.06);
		margin: 12rpx 0;
	}

	.net-profit-block {
		border-radius: $uni-radius-md;
		padding: 32rpx;
		margin: 20rpx 0;
		display: flex;
		justify-content: space-between;
		align-items: center;

		&.profit {
			background: rgba(0, 255, 157, 0.06);
			border: 1rpx solid rgba(0, 255, 157, 0.2);
		}

		&.loss {
			background: rgba(239, 68, 68, 0.06);
			border: 1rpx solid rgba(239, 68, 68, 0.2);
		}

		.net-label {
			font-size: 28rpx;
			color: $uni-text-secondary;
			font-weight: 500;
		}

		.net-val {
			font-size: 52rpx;
			font-weight: 700;
			font-family: $uni-font-tabular;

			.profit & { color: $uni-color-success; }
			.loss & { color: $uni-color-danger; }
		}
	}

	.warn-tip {
		margin-top: 20rpx;
		padding: 20rpx 24rpx;
		background: rgba(239, 68, 68, 0.08);
		border-radius: $uni-radius-sm;
		font-size: 26rpx;
		color: $uni-color-danger;
	}

	.loading {
		@include flex-center;
		padding: 80rpx;
		color: $uni-text-muted;
	}

	/* ===== 底部操作栏 ===== */
	.footer-action {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		background: rgba(15, 23, 42, 0.9);
		backdrop-filter: blur(16px);
		padding: 24rpx 40rpx;
		padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
		border-top: 1px solid rgba(255, 255, 255, 0.05);
		z-index: 100;

		.btn-group {
			display: flex;
			gap: 24rpx;
		}
	}

	.btn-back {
		flex: 1;
		height: 96rpx;
		background: rgba(255, 255, 255, 0.05);
		color: $uni-text-secondary;
		border: 1rpx solid rgba(255, 255, 255, 0.1);
		border-radius: 44rpx;
		font-size: 30rpx;
		display: flex;
		align-items: center;
		justify-content: center;

		&::after { display: none; }
	}

	.btn-next {
		flex: 2;
		height: 96rpx;
		background: linear-gradient(135deg, $uni-color-primary 0%, rgba(96, 165, 250, 1) 100%);
		color: #fff;
		font-weight: 600;
		border-radius: 44rpx;
		font-size: 30rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		box-shadow: 0 8rpx 24rpx rgba(59, 130, 246, 0.3);
		border: none;

		&.confirm {
			background: linear-gradient(135deg, $uni-color-success, rgba(0, 200, 120, 1));
			box-shadow: 0 8rpx 24rpx rgba(0, 255, 157, 0.25);
		}

		&::after { display: none; }
	}
</style>
