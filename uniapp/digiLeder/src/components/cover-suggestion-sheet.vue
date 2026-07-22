<template>
	<view class="sheet" v-if="visible">
		<view class="mask" @click="$emit('close')"></view>
		<view class="panel">
			<view class="title">封面推荐</view>
			<view class="grid">
				<image v-for="(url,idx) in suggestions" :key="idx" :src="url" mode="aspectFill" class="img" @click="$emit('apply', url)" />
			</view>
		</view>
	</view>
</template>
<script>export default { props: { visible: Boolean, suggestions: { type: Array, default: () => [] } } }</script>
<style lang="scss">.sheet{position:fixed;inset:0;z-index:99}.mask{position:absolute;inset:0;background:rgba(0,0,0,.5)}.panel{position:absolute;left:0;right:0;bottom:0;background:#111827;border-radius:24rpx 24rpx 0 0;padding:20rpx}.grid{display:grid;grid-template-columns:repeat(3,1fr);gap:12rpx}.img{width:100%;height:180rpx;border-radius:12rpx}.title{color:#fff;margin-bottom:8rpx}</style>
