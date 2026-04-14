<template>
	<view class="upload-image">
		<view class="preview-list">
			<view 
				class="preview-item" 
				v-for="(url, index) in modelValue" 
				:key="index"
				@click="previewImage(index)"
			>
				<image :src="url" mode="aspectFill"></image>
				<view class="delete-btn" @click.stop="removeImage(index)">
					<text class="icon">×</text>
				</view>
			</view>
			
			<view class="add-btn" @click="chooseImage" v-if="modelValue.length < maxCount" hover-class="card-hover-active">
				<text class="icon">📷</text>
				<text class="label">添加图片</text>
			</view>
		</view>
	</view>
</template>

<script>
	import { getBaseUrl } from '@/utils/request.js';

	export default {
		props: {
			modelValue: {
				type: Array,
				default: () => []
			},
			maxCount: {
				type: Number,
				default: 1
			}
		},
		methods: {
			chooseImage() {
				uni.chooseImage({
					count: this.maxCount - this.modelValue.length,
					sizeType: ['compressed'],
					sourceType: ['album', 'camera'],
					success: (res) => {
						const tempFilePaths = res.tempFilePaths;
						this.uploadFiles(tempFilePaths);
					}
				});
			},
			async uploadFiles(paths) {
				const uploadUrl = `${getBaseUrl()}/files/upload`; 
				
				uni.showLoading({ title: '上传中...', mask: true });
				
				for (const path of paths) {
					try {
						await this.uploadOne(uploadUrl, path);
					} catch (e) {
						console.error(e);
						uni.showToast({ title: '上传失败', icon: 'none' });
					}
				}
				
				uni.hideLoading();
			},
			uploadOne(url, filePath) {
				return new Promise((resolve, reject) => {
					uni.uploadFile({
						url: url,
						filePath: filePath,
						name: 'file',
						header: {
							// 补齐 Token
						},
						formData: {
							'usage': 'asset_cover'
						},
						success: (uploadFileRes) => {
							if(uploadFileRes.statusCode === 200) {
								const data = JSON.parse(uploadFileRes.data);
								if(data.code === 200) {
									const fileUrl = data.data.url || data.data; 
									this.$emit('update:modelValue', [...this.modelValue, fileUrl]);
									resolve(data.data);
								} else {
									reject(new Error(data.msg));
								}
							} else {
								reject(uploadFileRes);
							}
						},
						fail: (err) => {
							reject(err);
						}
					});
				});
			},
			removeImage(index) {
				const newParams = [...this.modelValue];
				newParams.splice(index, 1);
				this.$emit('update:modelValue', newParams);
			},
			previewImage(index) {
				uni.previewImage({
					urls: this.modelValue,
					current: index
				});
			}
		}
	}
</script>

<style lang="scss">
	.preview-list {
		display: flex;
		flex-wrap: wrap;
		gap: 24rpx;
	}
	
	.preview-item {
		width: 180rpx;
		height: 180rpx;
		border-radius: $uni-radius-md;
		overflow: hidden;
		position: relative;
		border: 1px solid rgba(255, 255, 255, 0.1);
		box-shadow: 0 4rpx 15rpx rgba(0,0,0,0.3);
		
		image {
			width: 100%;
			height: 100%;
		}
		
		.delete-btn {
			position: absolute;
			top: 0;
			right: 0;
			width: 48rpx;
			height: 48rpx;
			background: rgba(239, 68, 68, 0.8); /* Red-500 */
			color: #fff;
			display: flex;
			align-items: center;
			justify-content: center;
			border-bottom-left-radius: 16rpx;
			backdrop-filter: blur(4px);
			
			.icon { font-size: 36rpx; line-height: 1; font-weight: 300; margin-top: -4rpx; }
		}
	}
	
	.add-btn {
		width: 180rpx;
		height: 180rpx;
		background: rgba(255, 255, 255, 0.03);
		border: 2rpx dashed $uni-text-muted;
		border-radius: $uni-radius-md;
		display: flex;
		flex-direction: column;
		align-items: center;
		justify-content: center;
		color: $uni-text-muted;
		transition: all 0.2s ease;
		
		.icon { font-size: 56rpx; margin-bottom: 12rpx; opacity: 0.8; }
		.label { font-size: 24rpx; font-weight: 500; }
		
		&.card-hover-active {
			background: rgba(59, 130, 246, 0.05); /* blue glow */
			border-color: $uni-color-primary;
			color: $uni-color-primary;
		}
	}
</style>
