/**
 * 网络请求封装
 * - 统一拦截 { code, data, msg } 格式的后端返回体
 * - 支持运行时动态切换 API 地址（通过 `settings/index.vue` 配置并持久化）
 */

const DEFAULT_BASE_URL = import.meta.env.VITE_API_BASE || '/api';
const DEV_PROXY_BASE_URL = '/api';

function trimTrailingSlash(url) {
	return (url || '').trim().replace(/\/+$/, '');
}

function shouldForceDevProxy(url) {
	// H5 开发环境下，若手动配置了 localhost/127.0.0.1 直连地址，会触发浏览器 CORS。
	// 统一改走 Vite /api 代理，避免跨域。
	if (!(typeof window !== 'undefined' && import.meta.env.DEV)) return false;
	const normalized = (url || '').toLowerCase();
	return normalized.startsWith('http://127.0.0.1:') || normalized.startsWith('http://localhost:');
}

// ===================== 配置持久化 =====================

/**
 * 读取保存在 storage 里的服务器配置
 * @returns {{ primaryUrl: string, backupUrl: string, useBackup: boolean }}
 */
export function getConfig() {
	try {
		const raw = uni.getStorageSync('__digi_server_config__');
		if (raw) return JSON.parse(raw);
	} catch (_) {}
	return { primaryUrl: '', backupUrl: '', useBackup: false };
}

/**
 * 保存配置到本地 Storage
 * @param {{ primaryUrl: string, backupUrl: string, useBackup: boolean }} cfg
 */
export function saveConfig(cfg) {
	uni.setStorageSync('__digi_server_config__', JSON.stringify(cfg));
}

/**
 * 根据当前配置计算实际使用的 Base URL
 */
export function getBaseUrl() {
	const cfg = getConfig();
	const preferredUrl = cfg.useBackup ? cfg.backupUrl : cfg.primaryUrl;
	const baseUrl = trimTrailingSlash(preferredUrl) || trimTrailingSlash(DEFAULT_BASE_URL) || DEV_PROXY_BASE_URL;
	if (shouldForceDevProxy(baseUrl)) return DEV_PROXY_BASE_URL;
	return baseUrl;
}

// ===================== 请求封装 =====================

/**
 * 封装通用请求方法，使用 uni.request 实现跨平台网络请求
 * @param {{ url: string, method?: string, data?: any, header?: Record<string,string> }} options
 */
export const request = (options) => {
	return new Promise((resolve, reject) => {
		const baseUrl = getBaseUrl();

		uni.request({
			url: baseUrl + options.url,
			method: options.method || 'GET',
			data: options.data || {},
			header: {
				'Content-Type': 'application/json',
				...options.header
			},
			success: (res) => {
				const { statusCode, data } = res;
				if (statusCode === 200) {
					// 统一返回体格式 { code, data, msg }
					if (data && data.code === 200) {
						resolve(data.data);
					} else {
						uni.showToast({
							title: (data && data.msg) || '操作失败',
							icon: 'none'
						});
						reject(new Error((data && data.msg) || 'Business Error'));
					}
				} else {
					uni.showToast({
						title: `网络异常: ${statusCode}`,
						icon: 'none'
					});
					reject(new Error(`HTTP Error: ${statusCode}`));
				}
			},
			fail: (err) => {
				uni.showToast({
					title: '网络请求失败，请检查连接',
					icon: 'none'
				});
				reject(err);
			}
		});
	});
};

export default request;
