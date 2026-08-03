import axios, { AxiosError, type AxiosRequestConfig, type AxiosResponse } from 'axios'
import type { ApiEnvelope } from '@/types'
import { normalizeMediaUrls } from '@/utils/storage'

export class ApiError extends Error {
  constructor(message: string, public readonly status?: number) { super(message); this.name = 'ApiError' }
}

const client = axios.create({ baseURL: import.meta.env.VITE_API_BASE || '/api', timeout: 15000 })

// Unwrap exactly once so every screen treats transport and business failures consistently.
async function unwrap<T>(request: Promise<AxiosResponse<ApiEnvelope<T>>>): Promise<T> {
  try {
    const response = await request
    if (response.data.code !== 200) throw new ApiError(response.data.msg || '服务暂时不可用', response.status)
    return normalizeMediaUrls(response.data.data)
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

/** 用于图片等非 ApiEnvelope 二进制响应，同时保留统一的错误提示。 */
export async function postBlob(url: string, data?: unknown): Promise<Blob> {
  try {
    const response = await client.post<ArrayBuffer>(url, data, { responseType: 'arraybuffer' })
    const contentType = String(response.headers['content-type'] || '').split(';')[0].trim().toLowerCase()
    if (!contentType.startsWith('image/')) {
      const message = new TextDecoder().decode(response.data)
      throw new ApiError(message || '服务未返回图片', response.status)
    }
    return new Blob([response.data], { type: contentType })
  } catch (error) {
    if (error instanceof ApiError) throw error
    const axiosError = error as AxiosError<Blob>
    let message = axiosError.message || '网络连接失败'
    if (axiosError.response?.data instanceof Blob) {
      try {
        const body = JSON.parse(await axiosError.response.data.text()) as { msg?: string }
        message = body.msg || message
      } catch {
        // 非 JSON 错误体时保留网络错误描述。
      }
    }
    throw new ApiError(message, axiosError.response?.status)
  }
}
