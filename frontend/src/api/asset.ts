import http from './http'
import type { AssetDetail, AssetSummary, SaleRecord } from '../types'

// 资产表单载荷，涵盖基础信息与购买记录
export type AssetPayload = {
  name: string
  category: string
  brand?: string
  model?: string
  serialNo?: string
  status: string
  purchaseDate?: string
  enabledDate: string
  retiredDate?: string
  coverImageUrl?: string
  notes?: string
  tags?: string[]
  purchases?: Array<{
    type: 'PRIMARY' | 'ACCESSORY' | 'SERVICE'
    platform?: string
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
  platform?: string
  buyer?: string
  salePrice: number
  fee?: number
  shippingCost?: number
  otherCost?: number
  saleDate: string
  attachments?: string[]
  notes?: string
}

// 获取资产列表，支持状态与关键字过滤
export const fetchAssets = (params?: { status?: string; keyword?: string }) =>
  http.get<AssetSummary[]>('/assets', { params })

// 根据主键获取资产详情
export const fetchAssetDetail = (id: number) => http.get<AssetDetail>(`/assets/${id}`)

// 创建资产并返回新资产 ID
export const createAsset = (payload: AssetPayload) => http.post<number>('/assets', payload)

// 更新资产信息
export const updateAsset = (id: number, payload: AssetPayload) => http.put<void>(`/assets/${id}`, payload)

// 删除资产
export const deleteAsset = (id: number) => http.delete<void>(`/assets/${id}`)

// 提交出售向导数据，返回售出记录
export const sellAsset = (id: number, payload: SellPayload) =>
  http.post<SaleRecord>(`/assets/${id}/sell`, payload)
