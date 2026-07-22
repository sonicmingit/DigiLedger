<template>
	<view class="container">
		<view class="card" v-if="item">
			<view class="form-item">
				<text class="label">名称</text>
				<input class="input" v-model="form.name" placeholder="请输入名称" />
			</view>
			<view class="form-item">
				<text class="label">预算</text>
				<input class="input" type="digit" v-model="form.budget" placeholder="可不填" />
			</view>
			<view class="form-item">
				<text class="label">优先级</text>
				<picker mode="selector" :range="priorities" @change="onPriorityChange">
					<view class="input">{{ form.priority || 'medium' }}</view>
				</picker>
			</view>
			<view class="form-item">
				<text class="label">目标平台</text>
				<input class="input" v-model="form.targetPlatform" placeholder="如 京东" />
			</view>
			<view class="form-item">
				<text class="label">分类</text>
				<input class="input" v-model="form.category" />
			</view>
			<view class="form-item">
				<text class="label">备注</text>
				<textarea class="input textarea" v-model="form.notes" />
			</view>
			<button class="btn" @click="save" :loading="saving">保存</button>
		</view>
		<view v-else class="loading">加载中...</view>
	</view>
</template>

<script>
import request, { API_PATHS } from '@/utils/request.js';

export default {
	data() {
		return {
			id: '',
			item: null,
			form: { name: '', budget: '', priority: 'medium', targetPlatform: '', category: '', notes: '' },
			priorities: ['low', 'medium', 'high'],
			saving: false
		}
	},
	onLoad(options) {
		this.id = options?.id || '';
		if (!this.id) return uni.showToast({ title: '缺少ID', icon: 'none' });
		this.loadDetail();
	},
	methods: {
		async loadDetail() {
			this.item = await request({ url: `${API_PATHS.wishlist}/${this.id}` });
			this.form = {
				name: this.item.name || '',
				budget: this.item.budget ?? '',
				priority: this.item.priority || 'medium',
				targetPlatform: this.item.targetPlatform || '',
				category: this.item.category || '',
				notes: this.item.notes || ''
			};
		},
		onPriorityChange(e) { this.form.priority = this.priorities[e.detail.value]; },
		async save() {
			if (!this.form.name.trim()) return uni.showToast({ title: '名称不能为空', icon: 'none' });
			this.saving = true;
			try {
				await request({
					url: `${API_PATHS.wishlist}/${this.id}`,
					method: 'PUT',
					data: { ...this.item, ...this.form, budget: this.form.budget ? Number(this.form.budget) : null }
				});
				uni.showToast({ title: '已保存', icon: 'success' });
				setTimeout(() => uni.navigateBack(), 600);
			} finally {
				this.saving = false;
			}
		}
	}
}
</script>

<style lang="scss">
.container { padding: 24rpx; }
.card { @include card-base; }
.form-item { margin-bottom: 20rpx; }
.label { display:block; margin-bottom: 8rpx; color: $uni-text-secondary; }
.input { width: 100%; min-height: 76rpx; border: 1px solid $uni-border-color; border-radius: 12rpx; padding: 16rpx; color: $uni-text-primary; }
.textarea { height: 160rpx; }
.btn { margin-top: 12rpx; background: $uni-color-primary; color: #fff; }
.loading { padding: 120rpx 0; text-align:center; color: $uni-text-muted; }
</style>
