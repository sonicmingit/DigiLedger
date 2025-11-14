import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

interface ApiEnvelope<T> {
  code: number
  data: T
  msg: string
}

// 统一创建 axios 实例，保持所有请求指向同一 API 前缀
const axiosInstance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000
})

// 解包 `{code,data,msg}` 响应结构，统一处理业务错误
const unwrap = async <T>(promise: Promise<AxiosResponse<ApiEnvelope<T>>>) => {
  const resp = await promise
  const { code, data, msg } = resp.data
  if (code !== 200) {
    const errMsg = msg || '请求失败'
    ElMessage.error(errMsg)
    throw new Error(errMsg)
  }
  return data
}

const http = {
  get<T>(url: string, config?: AxiosRequestConfig) {
    return unwrap<T>(axiosInstance.get<ApiEnvelope<T>>(url, config))
  },
  post<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return unwrap<T>(axiosInstance.post<ApiEnvelope<T>>(url, data, config))
  },
  put<T>(url: string, data?: unknown, config?: AxiosRequestConfig) {
    return unwrap<T>(axiosInstance.put<ApiEnvelope<T>>(url, data, config))
  },
  delete<T>(url: string, config?: AxiosRequestConfig) {
    return unwrap<T>(axiosInstance.delete<ApiEnvelope<T>>(url, config))
  }
}

export default http
