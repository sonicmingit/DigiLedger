/**
 * 网络请求封装
 * - 统一拦截 { code, data, msg } 格式的后端返回体
 * - 支持运行时动态切换 API 地址（通过 `settings/index.vue` 配置并持久化）
 */

const DEFAULT_BASE_URL = import.meta.env.VITE_API_BASE || '/api';
const DEV_PROXY_BASE_URL = '/api';

export const API_PATHS = {
	assets: '/assets',
	wishlist: '/wishlist',
	dict: '/dict',
	purchases: '/purchases',
	attachments: '/attachments'
};

function trimTrailingSlash(url) {
	return (url || '').trim().replace(/\/+$/, '');
}

function shouldForceDevProxy(url) {
	if (!(typeof window !== 'undefined' && import.meta.env.DEV)) return false;
	const normalized = (url || '').toLowerCase();
	return normalized.startsWith('http://127.0.0.1:') || normalized.startsWith('http://localhost:');
}

export function getConfig() {
	try {
		const raw = uni.getStorageSync('__digi_server_config__');
		if (raw) return JSON.parse(raw);
	} catch (_) {}
	return { primaryUrl: '', backupUrl: '', useBackup: false };
}

export function saveConfig(cfg) {
	uni.setStorageSync('__digi_server_config__', JSON.stringify(cfg));
}

export function getBaseUrl() {
	const cfg = getConfig();
	const preferredUrl = cfg.useBackup ? cfg.backupUrl : cfg.primaryUrl;
	const baseUrl = trimTrailingSlash(preferredUrl) || trimTrailingSlash(DEFAULT_BASE_URL) || DEV_PROXY_BASE_URL;
	if (shouldForceDevProxy(baseUrl)) return DEV_PROXY_BASE_URL;
	return baseUrl;
}

export function resolveApiUrl(path = '') {
	const baseUrl = getBaseUrl();
	const normalizedPath = String(path || '').startsWith('/') ? path : `/${path}`;
	return `${baseUrl}${normalizedPath}`;
}

export const request = (options) => {
	return new Promise((resolve, reject) => {
		uni.request({
			url: resolveApiUrl(options.url),
			method: options.method || 'GET',
			data: options.data || {},
			header: {
				'Content-Type': 'application/json',
				...options.header
			},
			success: (res) => {
				const { statusCode, data } = res;
				if (statusCode === 200) {
					if (data && data.code === 200) {
						resolve(data.data);
					} else {
						uni.showToast({ title: (data && data.msg) || '操作失败', icon: 'none' });
						reject(new Error((data && data.msg) || 'Business Error'));
					}
				} else {
					uni.showToast({ title: `网络异常: ${statusCode}`, icon: 'none' });
					reject(new Error(`HTTP Error: ${statusCode}`));
				}
			},
			fail: (err) => {
				uni.showToast({ title: '网络请求失败，请检查连接', icon: 'none' });
				reject(err);
			}
		});
	});
};

export default request;
