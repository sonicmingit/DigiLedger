import axios, { AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiEnvelope } from '@/types'

export class ApiError extends Error {
  constructor(message: string, public readonly status?: number) { super(message); this.name = 'ApiError' }
}

const client = axios.create({ baseURL: import.meta.env.VITE_API_BASE || '/api', timeout: 15000 })

// Unwrap exactly once so every screen treats transport and business failures consistently.
async function unwrap<T>(request: Promise<AxiosResponse<ApiEnvelope<T>>>): Promise<T> {
  try {
    const response = await request
    if (response.data.code !== 200) throw new ApiError(response.data.msg || '服务暂时不可用', response.status)
    return response.data.data
  } catch (error) {
    if (error instanceof ApiError) throw error
    const axiosError = error as AxiosError<ApiEnvelope<unknown>>
    throw new ApiError(axiosError.response?.data?.msg || axiosError.message || '网络连接失败', axiosError.response?.status)
  }
}

export const http = {
  get: <T>(url: string, config?: AxiosRequestConfig) => unwrap<T>(client.get(url, config)),
  post: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) => unwrap<T>(client.post(url, data, config)),
  put: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) => unwrap<T>(client.put(url, data, config)),
  patch: <T>(url: string, data?: unknown, config?: AxiosRequestConfig) => unwrap<T>(client.patch(url, data, config)),
  delete: <T>(url: string, config?: AxiosRequestConfig) => unwrap<T>(client.delete(url, config))
}
