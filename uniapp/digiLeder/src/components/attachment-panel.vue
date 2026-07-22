<template>
	<view class="card">
		<view class="title">附件</view>
		<upload-image v-model="tmpImages" :max-count="1" :upload-path="`${basePath}/upload`" />
		<button size="mini" @click="submitUpload" :disabled="!tmpImages.length">添加到附件</button>
		<view class="row" v-for="file in list" :key="file.id">
			<text class="name">{{ file.fileName || file.url }}</text>
			<button size="mini" type="warn" @click="$emit('delete', file)">删除</button>
		</view>
	</view>
</template>
<script>
import UploadImage from '@/components/upload-image.vue';
export default {
	components: { UploadImage },
	props: { list: Array, basePath: { type: String, default: '/attachments' } },
	data(){ return { tmpImages: [] }; },
	methods: { submitUpload(){ this.$emit('upload', this.tmpImages[0]); this.tmpImages = []; } }
}
</script>
<style lang="scss">.card{ @include card-base; margin-bottom:24rpx;}.title{font-weight:700;margin-bottom:12rpx}.row{display:flex;justify-content:space-between;align-items:center;padding:10rpx 0}.name{color:$uni-text-secondary;max-width:70%}</style>
