import axios from 'axios'
import type { Router } from 'vue-router'

export interface LoginStatus { configured: boolean; authenticated: boolean; csrfToken: string }
let csrfToken = ''
export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api', timeout: 15000, withCredentials: true
})
apiClient.interceptors.request.use((config) => {
  if (!['get', 'head', 'options'].includes((config.method || 'get').toLowerCase()) && csrfToken) {
    config.headers.set('X-CSRF-Token', csrfToken)
  }
  return config
})
apiClient.interceptors.response.use((response) => response, (error) => {
  if (error.response?.status === 401 && !error.config?.url?.startsWith('/auth/')) {
    csrfToken = ''
    if (window.location.pathname !== '/login') {
      window.location.replace('/login?redirect=' + encodeURIComponent(window.location.pathname + window.location.search + window.location.hash))
    }
  }
  return Promise.reject(error)
})
export async function checkLogin(): Promise<LoginStatus> {
  const response = await apiClient.get('/auth/status')
  const status = response.data.data as LoginStatus
  csrfToken = status.csrfToken
  return status
}
export async function login(password: string) {
  await checkLogin()
  const response = await apiClient.post('/auth/login', { password })
  csrfToken = (response.data.data as LoginStatus).csrfToken
}
export async function logout() {
  await apiClient.post('/auth/logout')
  csrfToken = ''
  window.location.replace('/login')
}
export function authError(error: unknown): string {
  if (!axios.isAxiosError(error)) return (error as Error).message
  const response = error.response
  if (response?.data?.msg) return response.data.msg
  if (response?.status === 403 && String(response.data).includes('Invalid CORS request')) {
    return '请求被跨域校验拒绝（HTTP 403），请检查反向代理的域名、端口和协议配置'
  }
  if (response?.status === 502 || response?.status === 504) {
    return `代理无法连接后端或等待超时（HTTP ${response.status}），请检查后端和代理日志`
  }
  if (response) return `服务器请求失败（HTTP ${response.status}），请查看请求响应和服务日志`
  if (error.code === 'ECONNABORTED') return '请求超时，请检查网络和后端服务'
  return '无法连接服务器，请检查网络或服务地址'
}
export function safeRedirect(value: unknown): string {
  return typeof value === 'string' && value.startsWith('/') && !value.startsWith('//') &&
    !value.includes('\\') && value.split(/[?#]/)[0] !== '/login' ? value : '/'
}
export function installAuthGuard(router: Router) {
  router.beforeEach(async (to) => {
    try {
      const status = await checkLogin()
      if (to.name === 'login') return status.authenticated ? safeRedirect(to.query.redirect) : true
      return status.authenticated ? true : { name: 'login', query: { redirect: to.fullPath } }
    } catch {
      return to.name === 'login' ? true : { name: 'login', query: { redirect: to.fullPath } }
    }
  })
}
