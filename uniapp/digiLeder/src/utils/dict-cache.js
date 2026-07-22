import request, { API_PATHS } from '@/utils/request.js';

const DICT_CACHE_KEY = '__digi_dict_cache__';
const DICT_TTL_MS = 10 * 60 * 1000;

function loadCache() {
	try {
		return JSON.parse(uni.getStorageSync(DICT_CACHE_KEY) || '{}');
	} catch (_) {
		return {};
	}
}

function saveCache(cache) {
	uni.setStorageSync(DICT_CACHE_KEY, JSON.stringify(cache));
}

export async function getDictOptions(type) {
	const cache = loadCache();
	const hit = cache[type];
	if (hit && Date.now() - hit.ts < DICT_TTL_MS) return hit.data;

	const data = await request({ url: `${API_PATHS.dict}/${type}` });
	cache[type] = { ts: Date.now(), data: data || [] };
	saveCache(cache);
	return cache[type].data;
}
