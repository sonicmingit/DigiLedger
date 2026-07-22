<template>
	<view class="container">
		<view class="filter-sticky">
			<view class="search-row">
				<input class="search" v-model="filters.q" placeholder="搜索名称/品牌" confirm-type="search" @confirm="loadData" />
				<view class="filter-btn" @click="drawerVisible = true">筛选</view>
			</view>
			<scroll-view class="filter-bar" scroll-x="true" show-scrollbar="false">
				<view class="filter-item" :class="{ active: filters.status === item.value }" v-for="item in statusFilters" :key="item.value" @click="setStatus(item.value)">{{ item.label }}</view>
			</scroll-view>
		</view>

		<view class="list-section">
			<asset-card v-for="item in assetList" :key="item.id" :asset="item" @select="goDetail"></asset-card>
			<view v-if="loading" class="loading"><text class="loading-dot">...</text></view>
			<view v-if="!loading && assetList.length === 0" class="empty"><text class="icon">🔍</text><text class="empty-text">暂无匹配物品</text></view>
		</view>

		<filter-drawer
			:visible="drawerVisible"
			:value="filters"
			:category-options="dicts.categories"
			:platform-options="dicts.platforms"
			:tag-options="dicts.tags"
			@close="drawerVisible = false"
			@reset="resetFilters"
			@apply="applyFilters"
		/>
	</view>
</template>

<script>
import request, { API_PATHS } from '@/utils/request.js';
import { getDictOptions } from '@/utils/dict-cache.js';
import AssetCard from '@/components/asset-card.vue';
import FilterDrawer from '@/components/filter-drawer.vue';

export default {
	components: { AssetCard, FilterDrawer },
	data() {
		return {
			assetList: [],
			loading: false,
			navigating: false,
			drawerVisible: false,
			statusFilters: [
				{ label: '全部', value: '' },
				{ label: '使用中', value: '使用中' },
				{ label: '待出售/闲置', value: 'pending' },
				{ label: '已出售', value: '已出售' }
			],
			filters: { q: '', status: '', category: '', platform: '', tag: '' },
			dicts: { categories: [], platforms: [], tags: [] }
		}
	},
	async onShow() {
		await this.loadDicts();
		this.loadData();
	},
	methods: {
		async loadDicts() {
			const [categories, platforms, tags] = await Promise.allSettled([
				getDictOptions('categories'), getDictOptions('platforms'), getDictOptions('tags')
			]);
			this.dicts.categories = categories.status === 'fulfilled' ? categories.value : [];
			this.dicts.platforms = platforms.status === 'fulfilled' ? platforms.value : [];
			this.dicts.tags = tags.status === 'fulfilled' ? tags.value : [];
		},
		buildQuery() {
			const q = [];
			Object.entries(this.filters).forEach(([k, v]) => {
				if (!v) return;
				if (k === 'status' && v === 'pending') return;
				q.push(`${encodeURIComponent(k)}=${encodeURIComponent(v)}`);
			});
			return q.length ? `?${q.join('&')}` : '';
		},
		async loadData() {
			this.loading = true;
			try {
				const query = this.buildQuery();
				let res = await request({ url: `${API_PATHS.assets}${query}` });
				res = res || [];
				if (this.filters.status === 'pending') res = res.filter(item => item.status === '待出售' || item.status === '已闲置');
				this.assetList = res;
			} catch (e) {
				console.error('Fetch asset failed', e);
			} finally {
				this.loading = false;
			}
		},
		setStatus(val) { this.filters.status = val; this.loadData(); },
		resetFilters() { this.filters = { q: '', status: '', category: '', platform: '', tag: '' }; this.drawerVisible = false; this.loadData(); },
		applyFilters(next) { this.filters = { ...this.filters, ...next }; this.drawerVisible = false; this.loadData(); },
		goDetail(asset) {
			const id = asset && asset.id;
			if (!id) return uni.showToast({ title: '物品ID无效', icon: 'none' });
			if (this.navigating) return;
			this.navigating = true;
			uni.navigateTo({ url: `/pages/asset/detail?id=${id}`, complete: () => { this.navigating = false; } });
		}
	}
}
</script>

<style lang="scss">
.container { padding-bottom: env(safe-area-inset-bottom); }
.filter-sticky { position: sticky; top: 0; z-index: 10; background: rgba(15,23,42,.85); backdrop-filter: blur(12px); padding: 16rpx 0 24rpx; }
.search-row { display:flex; gap:12rpx; padding: 0 40rpx 16rpx; }
.search { flex:1; background: rgba(255,255,255,.06); border-radius: 36rpx; padding: 14rpx 24rpx; color: #fff; }
.filter-btn { padding: 12rpx 24rpx; border-radius: 32rpx; background: rgba(59,130,246,.2); color: #93c5fd; }
.filter-bar { white-space: nowrap; padding: 0 40rpx; box-sizing: border-box; }
.filter-item { display:inline-block; padding:12rpx 26rpx; background:#1f2937; border-radius:40rpx; margin-right:16rpx; color:#94a3b8; }
.filter-item.active { background: $uni-color-primary-dim; color: $uni-color-primary; }
.list-section { padding: 20rpx 40rpx 100rpx 40rpx; min-height: 60vh; }
.loading,.empty { padding: 120rpx 0; @include flex-center; flex-direction:column; }
</style>
