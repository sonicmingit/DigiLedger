import axios from 'axios'

// 创建 axios 实例并统一设置接口根路径与超时时间
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

// 统一拦截响应：约定后端返回 { code, data, msg }
http.interceptors.response.use(
  (resp) => {
    const { code, data, msg } = resp.data
    if (code !== 200) return Promise.reject(new Error(msg || 'Request Error'))
    return data
  },
  (err) => Promise.reject(err)
)

export default http
