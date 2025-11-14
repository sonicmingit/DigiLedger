import http from './http'

export interface CategoryNode {
  id: number
  name: string
  parentId: number | null
  level: number
  sort: number
  children: CategoryNode[]
}

export interface TagNode {
  id: number
  name: string
  parentId: number | null
  color?: string
  icon?: string
  sort: number
  children: TagNode[]
}

export interface PlatformItem {
  id: number
  name: string
  link?: string
  sort: number
}

export interface BrandItem {
  id: number
  name: string
  alias?: string
  initial?: string
  sort?: number
}

export const fetchCategoryTree = () => http.get<CategoryNode[]>('/dict/categories/tree')
export const createCategory = (payload: { name: string; parentId?: number | null; sort?: number }) =>
  http.post<number>('/dict/categories', payload)
export const updateCategory = (id: number, payload: { name: string; parentId?: number | null; sort?: number }) =>
  http.put<void>(`/dict/categories/${id}`, payload)
export const deleteCategory = (id: number) => http.delete<void>(`/dict/categories/${id}`)

export const fetchPlatforms = () => http.get<PlatformItem[]>('/dict/platforms')
export const createPlatform = (payload: { name: string; link?: string; sort?: number }) =>
  http.post<number>('/dict/platforms', payload)
export const updatePlatform = (id: number, payload: { name: string; link?: string; sort?: number }) =>
  http.put<void>(`/dict/platforms/${id}`, payload)
export const deletePlatform = (id: number) => http.delete<void>(`/dict/platforms/${id}`)

export const fetchBrands = () => http.get<BrandItem[]>('/dict/brands')
export const createBrand = (payload: { name: string; alias?: string; initial?: string; sort?: number }) =>
  http.post<number>('/dict/brands', payload)
export const updateBrand = (
  id: number,
  payload: { name: string; alias?: string; initial?: string; sort?: number }
) => http.put<void>(`/dict/brands/${id}`, payload)
export const deleteBrand = (id: number) => http.delete<void>(`/dict/brands/${id}`)

export const fetchTagTree = () => http.get<TagNode[]>('/dict/tags/tree')
export const createTag = (payload: { name: string; parentId?: number | null; color?: string; icon?: string; sort?: number }) =>
  http.post<number>('/dict/tags', payload)
export const updateTag = (
  id: number,
  payload: { name: string; parentId?: number | null; color?: string; icon?: string; sort?: number }
) => http.put<void>(`/dict/tags/${id}`, payload)
export const deleteTag = (id: number) => http.delete<void>(`/dict/tags/${id}`)
