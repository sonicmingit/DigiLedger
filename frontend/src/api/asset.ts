import http from './http'
import type { AssetDetail, AssetSummary, SaleRecord } from '../types'

// 物品表单载荷，涵盖基础信息与购买记录
export type AssetPayload = {
  name: string
  categoryId: number
  brand?: string
  model?: string
  serialNo?: string
  status: string
  purchaseDate?: string
  enabledDate: string
  retiredDate?: string
  coverImageUrl?: string
  notes?: string
  tagIds?: number[]
  purchases?: Array<{
    type: 'PRIMARY' | 'ACCESSORY' | 'SERVICE'
    name?: string
    platformId?: number
    seller?: string
    price: number
    shippingCost?: number
    currency?: string
    quantity?: number
    purchaseDate: string
    invoiceNo?: string
    warrantyMonths?: number
    warrantyExpireDate?: string
    attachments?: string[]
    notes?: string
  }>
}

// 售出请求载荷，用于出售向导
export type SellPayload = {
  platformId?: number
  buyer?: string
  salePrice: number
  fee?: number
  shippingCost?: number
  otherCost?: number
  saleDate: string
  attachments?: string[]
  notes?: string
}

// 获取物品列表，支持多维过滤
export const fetchAssets = (params?: {
  status?: string
  keyword?: string
  categoryId?: number
  platformId?: number
  tagIds?: number[]
}) => {
  const query: Record<string, unknown> = {}
  if (params?.status) query.status = params.status
  if (params?.keyword) query.q = params.keyword
  if (params?.categoryId) query.category_id = params.categoryId
  if (params?.platformId) query.platform_id = params.platformId
  if (params?.tagIds && params.tagIds.length > 0) query.tag_ids = params.tagIds
  return http.get<AssetSummary[]>('/assets', { params: query })
}

// 根据主键获取物品详情
export const fetchAssetDetail = (id: number) => http.get<AssetDetail>(`/assets/${id}`)

// 创建物品并返回新物品 ID
export const createAsset = (payload: AssetPayload) => http.post<number>('/assets', payload)

// 更新物品信息
export const updateAsset = (id: number, payload: AssetPayload) => http.put<void>(`/assets/${id}`, payload)

// 删除物品
export const deleteAsset = (id: number) => http.delete<void>(`/assets/${id}`)

// 提交出售向导数据，返回售出记录
export const sellAsset = (id: number, payload: SellPayload) =>
  http.post<SaleRecord>(`/assets/${id}/sell`, payload)
