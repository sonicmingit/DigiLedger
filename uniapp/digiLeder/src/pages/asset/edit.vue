<template>
	<view class="container">
		<view class="form-card">
			<view class="section-title">基本信息</view>
			
			<view class="form-item">
				<text class="label">物品名称 <text class="required">*</text></text>
				<input class="input" type="text" v-model="form.name" placeholder="请输入物品名称" />
			</view>
			
			<view class="form-item">
				<text class="label">当前状态</text>
				<picker mode="selector" :range="statusOptions" @change="onStatusChange">
					<view class="picker-value">
						{{ form.status }}
					</view>
				</picker>
			</view>
		</view>
		
		<view class="form-card">
			<view class="section-title">封面图片</view>
			<upload-image v-model="coverImages" :maxCount="1"></upload-image>
		</view>
		
		<view class="form-card">
			<view class="section-title">更多信息</view>
			
			<view class="form-item">
				<text class="label">品牌</text>
				<input class="input" type="text" v-model="form.brand" placeholder="品牌名称" />
			</view>
			
			<view class="form-item">
				<text class="label">型号</text>
				<input class="input" type="text" v-model="form.model" placeholder="具体型号" />
			</view>
			
			<view class="form-item">
				<text class="label">备注</text>
				<textarea class="textarea" v-model="form.notes" placeholder="填写备注信息..." />
			</view>
		</view>
		
		<view class="footer-action">
			<button class="btn-submit" @click="submit" :loading="submitting">保存修改</button>
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
				id: null,
				form: {
					name: '',
					status: '',
					brand: '',
					model: '',
					notes: '',
					// We might persist price/purchaseDate as read-only or allow edit if needed
					// For simplicity showing editable fields
				},
				coverImages: [],
				statusOptions: ['使用中', '已闲置', '待出售', '已出售'],
				submitting: false,
				originalAsset: null
			}
		},
		onLoad(options) {
			if (options.id) {
				this.id = options.id;
				this.loadData();
			}
		},
		methods: {
			async loadData() {
				try {
					const res = await request({ url: `/assets/${this.id}` });
					this.originalAsset = res;
					
					// Map response to form
					this.form.name = res.name;
					this.form.status = res.status;
					this.form.brand = res.brandName || ''; // Use brandName if flat
					this.form.model = res.model || '';
					this.form.notes = res.notes || '';
					
					if (res.coverImageUrl) {
						this.coverImages = [res.coverImageUrl];
					}
				} catch (e) {
					console.error(e);
					uni.showToast({ title: '加载失败', icon: 'none' });
				}
			},
			onStatusChange(e) {
				this.form.status = this.statusOptions[e.detail.value];
			},
			async submit() {
				if (!this.form.name) return uni.showToast({ title: '请输入名称', icon: 'none' });
				
				this.submitting = true;
				try {
					const payload = {
						...this.originalAsset, // Keep other fields
						...this.form,
						coverImageUrl: this.coverImages[0] || ''
					};
					
					await request({
						url: `/assets/${this.id}`,
						method: 'PUT',
						data: payload
					});
					
					uni.showToast({ title: '修改成功' });
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
		padding: 30rpx;
		padding-bottom: 200rpx;
	}
	
	.form-card {
		@include card-base;
		margin-bottom: 30rpx;
	}
	
	.section-title {
		font-size: 32rpx;
		font-weight: bold;
		color: $uni-text-primary;
		margin-bottom: 30rpx;
		padding-left: 16rpx;
		border-left: 6rpx solid $uni-color-primary;
	}
	
	.form-item {
		margin-bottom: 30rpx;
		
		.label {
			display: block;
			font-size: 28rpx;
			color: $uni-text-secondary;
			margin-bottom: 16rpx;
			
			.required { color: $uni-color-danger; margin-left: 8rpx; }
		}
		
		.input, .picker-value {
			height: 88rpx;
			background: rgba(255, 255, 255, 0.05);
			border: 1px solid $uni-border-color;
			border-radius: $uni-radius-sm;
			padding: 0 24rpx;
			color: $uni-text-primary;
			font-size: 28rpx;
			display: flex;
			align-items: center;
		}
		
		.textarea {
			width: 100%;
			height: 200rpx;
			background: rgba(255, 255, 255, 0.05);
			border: 1px solid $uni-border-color;
			border-radius: $uni-radius-sm;
			padding: 24rpx;
			color: $uni-text-primary;
			font-size: 28rpx;
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
		z-index: 100;
	}
	
	.btn-submit {
		background: $uni-color-primary;
		color: #000;
		font-weight: bold;
		border-radius: 44rpx;
		height: 88rpx;
		display: flex;
		align-items: center;
		justify-content: center;
		font-size: 32rpx;
		box-shadow: 0 0 20rpx $uni-color-primary-dim;
		
		&:active { opacity: 0.9; }
	}
</style>
