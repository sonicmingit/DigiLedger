<template>
	<view class="drawer" v-if="visible">
		<view class="mask" @click="$emit('close')"></view>
		<view class="panel">
			<view class="row"><text>分类</text><picker mode="selector" :range="categoryOptions" @change="onPick('category',$event)"><view>{{ model.category || '全部' }}</view></picker></view>
			<view class="row"><text>平台</text><picker mode="selector" :range="platformOptions" @change="onPick('platform',$event)"><view>{{ model.platform || '全部' }}</view></picker></view>
			<view class="row"><text>标签</text><picker mode="selector" :range="tagOptions" @change="onPick('tag',$event)"><view>{{ model.tag || '全部' }}</view></picker></view>
			<view class="actions">
				<button size="mini" @click="reset">重置</button>
				<button size="mini" type="primary" @click="apply">应用</button>
			</view>
		</view>
	</view>
</template>
<script>
export default {
	props: { visible: Boolean, value: Object, categoryOptions: Array, platformOptions: Array, tagOptions: Array },
	data() { return { model: { status: '', q: '', category: '', platform: '', tag: '' } } },
	watch: { value: { immediate: true, deep: true, handler(v) { this.model = { ...this.model, ...(v || {}) }; } } },
	methods: {
		onPick(key,e) { this.model[key] = this[`${key}Options`][e.detail.value] || ''; },
		reset() { this.model = { status: '', q: '', category: '', platform: '', tag: '' }; this.$emit('reset'); },
		apply() { this.$emit('apply', this.model); }
	}
}
</script>
<style lang="scss">
.drawer{position:fixed;inset:0;z-index:99}.mask{position:absolute;inset:0;background:rgba(0,0,0,.4)}.panel{position:absolute;left:0;right:0;bottom:0;background:#111827;padding:24rpx;border-radius:24rpx 24rpx 0 0}.row{display:flex;justify-content:space-between;padding:18rpx 0;color:#fff}.actions{display:flex;gap:20rpx;justify-content:flex-end}
</style>
