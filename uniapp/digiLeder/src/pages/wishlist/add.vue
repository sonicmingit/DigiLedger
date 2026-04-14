<template>
	<view class="container">
		<view class="header-banner">
			<text class="title">添加心愿</text>
			<text class="subtitle">记录想入手的好物</text>
		</view>

		<view class="form-body">
			<!-- 名称 + 预算 核心区域 -->
			<view class="form-card highlight-card">
				<view class="form-item row-mode">
					<text class="label">名称</text>
					<input
						class="input seamless"
						type="text"
						v-model="form.name"
						placeholder="想入手什么？"
						placeholder-style="color: rgba(255,255,255,0.2)"
					/>
				</view>

				<view class="form-item row-mode divider">
					<text class="label">预算</text>
					<view class="price-input">
						<text class="currency">¥</text>
						<input
							class="input seamless money"
							type="digit"
							v-model="form.budget"
							placeholder="不填则不限"
							placeholder-style="color: rgba(255,255,255,0.2)"
						/>
					</view>
				</view>
			</view>

			<!-- 优先级 -->
			<view class="form-card">
				<view class="section-title">期望强度</view>
				<view class="priority-group">
					<view
						class="priority-pill"
						:class="[{ active: form.priority === item.value }, item.value]"
						v-for="item in priorityOptions"
						:key="item.value"
						@click="form.priority = item.value"
					>
						<text class="pill-icon">{{ item.icon }}</text>
						<text class="pill-label">{{ item.label }}</text>
					</view>
				</view>
			</view>

			<!-- 购买来源 + 分类 -->
			<view class="form-card">
				<view class="section-title">渠道与分类</view>
				<view class="form-item">
					<text class="label">期望购买平台</text>
					<input
						class="input box"
						type="text"
						v-model="form.targetPlatform"
						placeholder="京东 / 淘宝 / 闲鱼…"
					/>
				</view>
				<view class="form-item mt-24">
					<text class="label">分类</text>
					<view class="pill-group">
						<view
							class="pill"
							:class="{ active: form.category === item }"
							v-for="item in categoryOptions"
							:key="item"
							@click="form.category = item"
						>
							{{ item }}
						</view>
					</view>
				</view>
			</view>

			<!-- 备注 -->
			<view class="form-card">
				<view class="section-title">备注</view>
				<view class="form-item">
					<textarea
						class="textarea box"
						v-model="form.notes"
						placeholder="型号偏好、参考链接... 随便写"
						:show-confirm-bar="false"
					/>
				</view>
			</view>
		</view>

		<view class="footer-action">
			<button class="btn-submit" @click="submit" :loading="submitting" hover-class="btn-hover-active">
				加入心愿单
			</button>
		</view>
	</view>
</template>

<script>
	import request from '@/utils/request.js';

	export default {
		data() {
			return {
				form: {
					name: '',
					budget: '',
					priority: 'medium',
					targetPlatform: '',
					category: '',
					notes: ''
				},
				submitting: false,
				priorityOptions: [
					{ value: 'low',    label: '普通',  icon: '💭' },
					{ value: 'medium', label: '想要',  icon: '⭐' },
					{ value: 'high',   label: '必入',  icon: '🔥' }
				],
				categoryOptions: ['手机', '电脑', '相机', '耳机', '平板', '手表', '配件', '其他']
			}
		},
		methods: {
			async submit() {
				if (!this.form.name.trim()) {
					return uni.showToast({ title: '请输入心愿名称', icon: 'none' });
				}

				this.submitting = true;
				try {
					const payload = {
						...this.form,
						budget: this.form.budget ? Number(this.form.budget) : null
					};

					await request({
						url: '/wishlists',
						method: 'POST',
						data: payload
					});

					uni.showToast({ title: '已加入心愿单', icon: 'success' });
					setTimeout(() => {
						uni.navigateBack();
					}, 1000);
				} catch (e) {
					console.error(e);
				} finally {
					this.submitting = false;
				}
			}
		}
	}
</script>

<style lang="scss">
	.container {
		padding-bottom: env(safe-area-inset-bottom);
	}

	.header-banner {
		padding: 40rpx 40rpx 12rpx;

		.title {
			display: block;
			font-size: 48rpx;
			font-weight: 700;
			color: $uni-text-primary;
		}

		.subtitle {
			display: block;
			font-size: 26rpx;
			color: $uni-text-muted;
			margin-top: 8rpx;
		}
	}

	.form-body {
		padding: 20rpx 40rpx 160rpx 40rpx;
	}

	.form-card {
		@include card-base;
		margin-bottom: 32rpx;
		padding: 32rpx;

		&.highlight-card {
			background: linear-gradient(135deg, rgba(139, 92, 246, 0.1) 0%, rgba(15, 23, 42, 0) 100%);
			border-color: rgba(139, 92, 246, 0.25);
			padding: 16rpx 32rpx;
		}
	}

	.section-title {
		font-size: 24rpx;
		font-weight: 500;
		color: $uni-text-muted;
		margin-bottom: 28rpx;
		text-transform: uppercase;
		letter-spacing: 2rpx;
	}

	/* 行模式输入 */
	.form-item.row-mode {
		display: flex;
		align-items: center;
		padding: 32rpx 0;

		&.divider {
			border-top: 1rpx solid rgba(255, 255, 255, 0.05);
		}

		.label {
			width: 120rpx;
			font-size: 32rpx;
			font-weight: 500;
			color: $uni-text-secondary;
			flex-shrink: 0;
		}

		.seamless {
			flex: 1;
			font-size: 32rpx;
			color: $uni-text-primary;
			background: transparent;
			border: none;
			padding: 0;
			text-align: right;

			&.money {
				font-size: 44rpx;
				font-family: $uni-font-tabular;
				color: $uni-color-secondary;
				font-weight: bold;
			}
		}

		.price-input {
			flex: 1;
			display: flex;
			align-items: center;
			justify-content: flex-end;

			.currency {
				font-size: 32rpx;
				color: $uni-color-secondary;
				margin-right: 12rpx;
				font-weight: bold;
			}
		}
	}

	/* 优先级选择 */
	.priority-group {
		display: flex;
		gap: 20rpx;

		.priority-pill {
			flex: 1;
			padding: 24rpx 0;
			border-radius: $uni-radius-md;
			border: 1px solid rgba(255, 255, 255, 0.08);
			background: rgba(255, 255, 255, 0.03);
			@include flex-center;
			flex-direction: column;
			gap: 10rpx;
			transition: all 0.2s ease;

			.pill-icon { font-size: 40rpx; }
			.pill-label { font-size: 24rpx; color: $uni-text-secondary; font-weight: 500; }

			&.active {
				&.low    { background: rgba(100,116,139,0.15); border-color: rgba(100,116,139,0.4); .pill-label{ color: $uni-text-secondary; } }
				&.medium { background: rgba(59,130,246,0.12); border-color: rgba(59,130,246,0.4); .pill-label{ color: $uni-color-primary; } }
				&.high   { background: rgba(139,92,246,0.12); border-color: rgba(139,92,246,0.4); .pill-label{ color: $uni-color-secondary; } box-shadow: 0 4rpx 20rpx rgba(139,92,246,0.2); }
			}
		}
	}

	/* 常规 form-item */
	.form-item {
		.label {
			display: block;
			font-size: 26rpx;
			color: $uni-text-secondary;
			margin-bottom: 16rpx;
		}

		.input.box, .textarea.box {
			width: 100%;
			box-sizing: border-box;
			background: rgba(15, 23, 42, 0.6);
			border: 1px solid rgba(255, 255, 255, 0.08);
			border-radius: $uni-radius-sm;
			padding: 24rpx;
			color: $uni-text-primary;
			font-size: 28rpx;
		}

		.textarea.box { height: 160rpx; }
	}

	/* 分类 Pill 组 */
	.pill-group {
		display: flex;
		flex-wrap: wrap;
		gap: 20rpx;

		.pill {
			padding: 14rpx 30rpx;
			background: rgba(15, 23, 42, 0.6);
			border-radius: 40rpx;
			border: 1px solid rgba(255, 255, 255, 0.08);
			font-size: 26rpx;
			color: $uni-text-secondary;
			transition: all 0.2s ease;

			&.active {
				background: rgba(139, 92, 246, 0.12);
				border-color: $uni-color-secondary;
				color: $uni-color-secondary;
			}
		}
	}

	.mt-24 { margin-top: 24rpx; }

	/* Footer */
	.footer-action {
		position: fixed;
		bottom: 0;
		left: 0;
		right: 0;
		background: rgba(15, 23, 42, 0.85);
		backdrop-filter: blur(16px);
		padding: 24rpx 40rpx;
		padding-bottom: calc(24rpx + env(safe-area-inset-bottom));
		border-top: 1px solid rgba(255, 255, 255, 0.05);
		z-index: 100;
	}

	.btn-submit {
		background: linear-gradient(135deg, $uni-color-secondary 0%, $uni-color-primary 100%);
		color: #fff;
		font-weight: 600;
		border-radius: 44rpx;
		height: 96rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 32rpx;
		box-shadow: 0 8rpx 30rpx rgba(139, 92, 246, 0.35);
		border: none;

		&::after { display: none; }
	}
</style>
