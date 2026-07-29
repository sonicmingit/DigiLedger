import { http } from './http'
import type { AssetDetail, AssetPage, AssetPayload, AssetSummary, AssetStatus, PurchaseRecord, SaleRecord } from '@/types'

export interface AssetQuery { status?: string; keyword?: string; categoryId?: number; brandId?: number; platformId?: number; tagIds?: number[] }
export interface AssetPageQuery extends AssetQuery { page?: number; pageSize?: number; sortBy?: string; sortOrder?: 'asc' | 'desc' }
export interface SellPayload { platformId?: number; saleScope: 'ASSET' | 'ACCESSORY'; purchaseId?: number; buyer?: string; salePrice: number; fee?: number; shippingCost?: number; otherCost?: number; saleDate: string; attachments?: string[]; notes?: string }

export const fetchAssets = (filters: AssetQuery = {}) => http.get<AssetSummary[]>('/assets', { params: { status: filters.status || undefined, q: filters.keyword || undefined, category_id: filters.categoryId, brand_id: filters.brandId, platform_id: filters.platformId, tag_ids: filters.tagIds?.join(',') } })
export const fetchAssetsPage = (filters: AssetPageQuery = {}) => http.get<AssetPage>('/assets/page', { params: { status: filters.status || undefined, q: filters.keyword || undefined, category_id: filters.categoryId, brand_id: filters.brandId, platform_id: filters.platformId, tag_ids: filters.tagIds?.join(','), page: filters.page || 1, page_size: filters.pageSize || 20, sort_by: filters.sortBy || 'purchaseDate', sort_order: filters.sortOrder || 'desc' } })
export const fetchAsset = (id: number) => http.get<AssetDetail>(`/assets/${id}`)
export const createAsset = (payload: AssetPayload) => http.post<number>('/assets', payload)
export const updateAsset = (id: number, payload: AssetPayload) => http.put<void>(`/assets/${id}`, payload)
export const deleteAsset = (id: number) => http.delete<void>(`/assets/${id}`)
export const updateAssetStatus = (id: number, status: AssetStatus) => http.patch<void>(`/assets/${id}/status`, { status })
export const sellAsset = (id: number, payload: SellPayload) => http.post<SaleRecord>(`/assets/${id}/sell`, payload)
export const createPurchase = (payload: PurchaseRecord & { assetId: number }) => http.post<number>('/purchases', payload)
export const updateSale = (assetId: number, saleId: number, payload: SellPayload) => http.put<SaleRecord>(`/assets/${assetId}/sales/${saleId}`, payload)
export const deleteSale = (assetId: number, saleId: number) => http.delete<void>(`/assets/${assetId}/sales/${saleId}`)
export const updatePurchase = (id: number, payload: PurchaseRecord & { assetId: number }) => http.put<void>(`/purchases/${id}`, payload)
export const deletePurchase = (id: number) => http.delete<void>(`/purchases/${id}`)
