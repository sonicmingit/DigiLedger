import { http } from './http'
import type { BrandItem, CategoryNode, ExternalApiConfig, ExternalApiTestResponse, ImageSearchProvidersResponse, MtPhotosSearchResponse, PlatformItem, Preferences, TagNode } from '@/types'
export const fetchCategories = () => http.get<CategoryNode[]>('/dict/categories/tree')
export const createCategory = (payload: { name: string; parentId?: number | null; sort?: number }) => http.post<number>('/dict/categories', payload)
export const updateCategory = (id: number, payload: { name: string; parentId?: number | null; sort?: number }) => http.put<void>(`/dict/categories/${id}`, payload)
export const deleteCategory = (id: number) => http.delete<void>(`/dict/categories/${id}`)
export const fetchBrands = () => http.get<BrandItem[]>('/dict/brands')
export const createBrand = (payload: Partial<BrandItem>) => http.post<number>('/dict/brands', payload)
export const updateBrand = (id: number, payload: Partial<BrandItem>) => http.put<void>(`/dict/brands/${id}`, payload)
export const deleteBrand = (id: number) => http.delete<void>(`/dict/brands/${id}`)
export const fetchTags = () => http.get<TagNode[]>('/dict/tags/tree')
export const createTag = (payload: Partial<TagNode>) => http.post<number>('/dict/tags', payload)
export const updateTag = (id: number, payload: Partial<TagNode>) => http.put<void>(`/dict/tags/${id}`, payload)
export const deleteTag = (id: number) => http.delete<void>(`/dict/tags/${id}`)
export const fetchPlatforms = () => http.get<PlatformItem[]>('/dict/platforms')
export const createPlatform = (payload: Partial<PlatformItem>) => http.post<number>('/dict/platforms', payload)
export const updatePlatform = (id: number, payload: Partial<PlatformItem>) => http.put<void>(`/dict/platforms/${id}`, payload)
export const deletePlatform = (id: number) => http.delete<void>(`/dict/platforms/${id}`)
export const fetchPreferences = () => http.get<Preferences>('/settings/preferences')
export const savePreferences = (payload: Preferences) => http.put<void>('/settings/preferences', payload)
export const exportUrl = (format: 'json' | 'csv') => `${import.meta.env.VITE_API_BASE || '/api'}/data/export?format=${format}`
export const fetchExternalApiConfigs = () => http.get<ExternalApiConfig[]>('/external-api-configs')
export const saveExternalApiConfig = (apiCode: string, payload: {
  displayName: string; baseUrl: string; authType?: string; apiKey?: string; configJson?: string; timeoutMs?: number; enabled?: boolean
}) => http.put<ExternalApiConfig>(`/external-api-configs/${apiCode}`, payload)
export const testMtPhotosSearch = (payload: { query: string; mode: 'KEYWORD' | 'CLIP'; page?: number }) =>
  http.post<MtPhotosSearchResponse>('/external-api-configs/MT_PHOTOS/test-search', payload)
export const testExternalApiConfig = (apiCode: string, payload?: { query?: string }) =>
  http.post<ExternalApiTestResponse>(`/external-api-configs/${apiCode}/test`, payload || {})
export const fetchImageSearchProviders = () => http.get<ImageSearchProvidersResponse>('/image-search/providers')
export const saveImageSearchProviders = (providers: string[]) => http.put<void>('/image-search/providers', { providers })
