<template>
	<div class="settings-page">
		<div class="header">
			<h1>设置</h1>
		</div>
		
		<div class="card">
			<div class="card-title">服务器配置</div>
			
			<div class="form-item">
				<label>主服务器地址</label>
				<input type="text" v-model="config.primaryUrl" placeholder="例如 http://192.168.1.100:8080/api" />
			</div>
			
			<div class="form-item">
				<label>备用服务器地址</label>
				<input type="text" v-model="config.backupUrl" placeholder="例如 http://localhost:8080/api" />
			</div>
			
			<div class="form-item switch-item">
				<label>使用备用服务器</label>
				<switch :checked="config.useBackup" @change="onSwitchChange" color="#00f3ff" />
			</div>
			
			<div class="actions">
				<button class="btn-save" @click="save">保存配置</button>
			</div>
		</div>
		
		<div class="info">
			<p>当前API地址: {{ currentApiUrl }}</p>
		</div>
	</div>
</template>

<script>
	import { getConfig, saveConfig, getBaseUrl } from '@/utils/request.js';
	
	export default {
		data() {
			return {
				config: {
					primaryUrl: '',
					backupUrl: '',
					useBackup: false
				},
				currentApiUrl: ''
			};
		},
		onShow() {
			this.loadConfig();
		},
		methods: {
			loadConfig() {
				this.config = getConfig();
				this.currentApiUrl = getBaseUrl();
			},
			onSwitchChange(e) {
				this.config.useBackup = e.detail.value;
			},
			save() {
				saveConfig(this.config);
				this.currentApiUrl = getBaseUrl();
				uni.showToast({
					title: '保存成功',
					icon: 'success'
				});
			}
		}
	}
</script>

<style lang="scss">
	.settings-page {
		padding: 40rpx 30rpx;
	}
	
	.header {
		margin-bottom: 40rpx;
		h1 {
			font-size: 48rpx;
			font-weight: bold;
			color: $uni-text-primary;
		}
	}
	
	.card {
		@include card-base;
		margin-bottom: 30rpx;
	}
	
	.card-title {
		font-size: 32rpx;
		font-weight: bold;
		margin-bottom: 30rpx;
		padding-bottom: 20rpx;
		border-bottom: 1px solid $uni-border-color;
	}
	
	.form-item {
		margin-bottom: 30rpx;
		
		label {
			display: block;
			font-size: 28rpx;
			color: $uni-text-secondary;
			margin-bottom: 16rpx;
		}
		
		input {
			height: 80rpx;
			background: rgba(255, 255, 255, 0.05);
			border: 1px solid $uni-border-color;
			border-radius: $uni-radius-sm;
			padding: 0 20rpx;
			color: $uni-text-primary;
			font-size: 28rpx;
			
			&:focus {
				border-color: $uni-color-primary;
			}
		}
		
		&.switch-item {
			display: flex;
			justify-content: space-between;
			align-items: center;
			
			label { margin-bottom: 0; }
		}
	}
	
	.btn-save {
		background: $uni-color-primary;
		color: #000;
		font-weight: bold;
		border-radius: 99rpx;
		margin-top: 20rpx;
		
		&:active {
			opacity: 0.9;
		}
	}
	
	.info {
		text-align: center;
		font-size: 24rpx;
		color: $uni-text-muted;
	}
</style>
