<template>
	<view class="sheet" v-if="visible">
		<view class="mask" @click="$emit('close')"></view>
		<view class="panel">
			<input class="input" type="digit" v-model="local.price" placeholder="价格" />
			<picker mode="date" :value="local.purchaseDate" @change="e => local.purchaseDate = e.detail.value"><view class="input">{{ local.purchaseDate || '购买日期' }}</view></picker>
			<input class="input" type="number" v-model="local.quantity" placeholder="数量" />
			<view class="actions"><button size="mini" @click="$emit('close')">取消</button><button size="mini" type="primary" @click="$emit('save', normalize())">保存</button></view>
		</view>
	</view>
</template>
<script>
export default {
	props: { visible: Boolean, value: Object },
	data() { return { local: { id: null, price: '', purchaseDate: '', quantity: 1, type: 'PRIMARY' } } },
	watch: { value: { immediate: true, deep: true, handler(v){ this.local = { ...this.local, ...(v||{}) }; } } },
	methods: { normalize(){ return { ...this.local, price: Number(this.local.price || 0), quantity: Number(this.local.quantity || 1) }; } }
}
</script>
<style lang="scss">.sheet{position:fixed;inset:0;z-index:99}.mask{position:absolute;inset:0;background:rgba(0,0,0,.4)}.panel{position:absolute;left:0;right:0;bottom:0;background:#111827;padding:24rpx}.input{margin-bottom:16rpx;border:1px solid #374151;padding:16rpx;border-radius:12rpx;color:#fff}.actions{display:flex;justify-content:flex-end;gap:16rpx}</style>
