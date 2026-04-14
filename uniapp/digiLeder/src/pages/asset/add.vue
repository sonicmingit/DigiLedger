<template>
	<view class="container">
		<!-- 头图或名称吸顶 -->
		<view class="header-banner">
			录入新物品
		</view>
		
		<view class="form-body">
			<!-- 重点投入与名称区域 -->
			<view class="form-card highlight-card">
				<view class="form-item row-mode">
					<text class="label">名称</text>
					<input class="input seamless" type="text" v-model="form.name" placeholder="请输入核心名称" placeholder-style="color: rgba(255,255,255,0.2)" />
				</view>
				
				<view class="form-item row-mode divider">
					<text class="label">价格</text>
					<view class="price-input">
						<text class="currency">¥</text>
						<input class="input seamless money" type="digit" v-model="form.price" placeholder="0.00" placeholder-style="color: rgba(255,255,255,0.2)" />
					</view>
				</view>
			</view>
			
			<!-- 图片上传区 -->
			<view class="form-card">
				<view class="section-title">封面照片</view>
				<upload-image v-model="coverImages" :maxCount="1"></upload-image>
			</view>
			
			<!-- 状态与时间选取 -->
			<view class="form-card">
				<view class="section-title">状态划分</view>
				<view class="pill-group">
					<view 
						class="pill" 
						:class="{ active: form.status === item }" 
						v-for="item in statusOptions" 
						:key="item"
						@click="form.status = item"
					>
						{{ item }}
					</view>
				</view>
				
				<view class="form-item mt-32">
					<text class="label">购入首日</text>
					<picker mode="date" :value="form.purchaseDate" @change="onDateChange">
						<view class="picker-box" hover-class="card-hover-active">
							<text class="val" :class="{ empty: !form.purchaseDate }">{{ form.purchaseDate || '请选择购入日期' }}</text>
							<text class="arrow">▼</text>
						</view>
					</picker>
				</view>
			</view>
			
			<!-- 附加信息域 -->
			<view class="form-card">
				<view class="section-title">标签与备注</view>
				<view class="form-item">
					<text class="label">所属平台/渠道</text>
					<input class="input box" type="text" v-model="form.platform" placeholder="例如：京东、闲鱼、Apple Store" />
				</view>
				<view class="form-item mt-24">
					<text class="label">详细型号 / 序列号</text>
					<input class="input box" type="text" v-model="form.model" placeholder="便于以后挂闲鱼参考" />
				</view>
				<view class="form-item mt-24">
					<text class="label">回忆与备注</text>
					<textarea class="textarea box" v-model="form.notes" placeholder="记录一些属于它的故事..." :show-confirm-bar="false" />
				</view>
			</view>
		</view>
		
		<view class="footer-action">
			<button class="btn-submit" @click="submit" :loading="submitting" hover-class="btn-hover-active">确认入库</button>
		</view>
	</view>
</template>

<script>
	import request from '@/utils/request.js';
	import UploadImage from '@/components/upload-image.vue';
	
	export default {
		components: {
			UploadImage
		},
		data() {
			return {
				form: {
					name: '',
					price: '',
					purchaseDate: this.getToday(),
					status: '使用中',
					platform: '',
					model: '',
					notes: ''
				},
				coverImages: [],
				statusOptions: ['使用中', '已闲置', '待出售'],
				submitting: false
			}
		},
		methods: {
			getToday() {
				const date = new Date();
				const y = date.getFullYear();
				const m = String(date.getMonth() + 1).padStart(2, '0');
				const d = String(date.getDate()).padStart(2, '0');
				return `${y}-${m}-${d}`;
			},
			onDateChange(e) {
				this.form.purchaseDate = e.detail.value;
			},
			async submit() {
				if (!this.form.name) return uni.showToast({ title: '没有名字怎么行', icon: 'none' });
				if (!this.form.price) return uni.showToast({ title: '该资产必须有个初始价格', icon: 'none' });
				
				this.submitting = true;
				
				try {
					const payload = {
						...this.form,
						// 后端 API 中价格多存放在明细表中，主表只保存聚合。
						// 按照我们重构后的约定组合体发给后端。
						targetCostValue: Number(this.form.price),
						targetCostStrategy: 'CUSTOM',
						coverImageUrl: this.coverImages[0] || '',
						purchases: [{
							type: 'PRIMARY',
							price: Number(this.form.price),
							purchaseDate: this.form.purchaseDate,
							quantity: 1
						}]
					};
					
					await request({
						url: '/assets',
						method: 'POST',
						data: payload
					});
					
					uni.showToast({ title: '入库成功', icon: 'success' });
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
		padding: 40rpx 40rpx 20rpx 40rpx;
		font-size: 48rpx;
		font-weight: 700;
		color: $uni-text-primary;
		letter-spacing: -1rpx;
	}
	
	.form-body {
		padding: 20rpx 40rpx 160rpx 40rpx;
	}
	
	.form-card {
		@include card-base;
		margin-bottom: 32rpx;
		padding: 32rpx;
		
		&.highlight-card {
			background: linear-gradient(135deg, rgba(59, 130, 246, 0.1) 0%, rgba(15, 23, 42, 0) 100%);
			border-color: rgba(59, 130, 246, 0.2);
			padding: 16rpx 32rpx;
		}
	}
	
	.section-title {
		font-size: 28rpx;
		font-weight: 500;
		color: $uni-text-muted;
		margin-bottom: 32rpx;
		text-transform: uppercase;
		letter-spacing: 2rpx;
	}
	
	/* 行模式 (如顶部的高亮输入框) */
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
				color: $uni-color-primary;
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
				color: $uni-color-primary;
				margin-right: 12rpx;
				font-weight: bold;
			}
		}
	}
	
	/* 常规输入框模式 */
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
			transition: all 0.2s ease;
			
			/* input focus 模拟伪类（uniapp 部分平台支持不好，通常用 js，此处 css 提供 H5 效果） */
			&:focus {
				border-color: rgba(59, 130, 246, 0.5);
				background: rgba(15, 23, 42, 0.8);
			}
		}
		
		.textarea.box {
			height: 180rpx;
		}
	}
	
	/* Pill 式单选组 */
	.pill-group {
		display: flex;
		flex-wrap: wrap;
		gap: 24rpx;
		
		.pill {
			padding: 16rpx 36rpx;
			background: rgba(15, 23, 42, 0.6);
			border-radius: 40rpx;
			border: 1px solid rgba(255, 255, 255, 0.08);
			font-size: 28rpx;
			color: $uni-text-secondary;
			transition: all 0.2s ease;
			
			&.active {
				background: rgba(59, 130, 246, 0.15);
				border-color: $uni-color-primary;
				color: $uni-color-primary;
				font-weight: 500;
				box-shadow: 0 4rpx 15rpx rgba(59, 130, 246, 0.15);
			}
		}
	}
	
	/* 日期选择器 */
	.picker-box {
		height: 88rpx;
		background: rgba(15, 23, 42, 0.6);
		border: 1px solid rgba(255, 255, 255, 0.08);
		border-radius: $uni-radius-sm;
		padding: 0 24rpx;
		display: flex;
		align-items: center;
		justify-content: space-between;
		
		.val {
			font-size: 28rpx;
			color: $uni-text-primary;
			
			&.empty { color: rgba(255,255,255,0.2); }
		}
		
		.arrow {
			font-size: 20rpx;
			color: $uni-text-muted;
		}
	}
	
	.mt-24 { margin-top: 24rpx; }
	.mt-32 { margin-top: 32rpx; }
	
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
		background: linear-gradient(135deg, $uni-color-primary 0%, rgba(96, 165, 250, 1) 100%);
		color: #fff;
		font-weight: 600;
		border-radius: 44rpx;
		height: 96rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 32rpx;
		box-shadow: 0 8rpx 30rpx rgba(59, 130, 246, 0.3);
		border: none;
		
		/* Remove uniapp default button borders */
		&::after { display: none; }
	}
	
	.btn-hover-active {
		transform: scale(0.98);
		box-shadow: 0 4rpx 15rpx rgba(59, 130, 246, 0.2);
	}
</style>
