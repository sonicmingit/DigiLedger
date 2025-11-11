import http from './http'
import type { AssetPayload } from './asset'
import type { WishlistItem } from '../types'

// 获取心愿单列表
export const fetchWishlist = () => http.get<WishlistItem[]>('/wishlist')

// 新增心愿条目
export const createWishlist = (payload: {
  name: string
  category?: string
  brand?: string
  model?: string
  expectedPrice?: number
  plannedPlatform?: string
  link?: string
  notes?: string
  priority?: number
}) => http.post<number>('/wishlist', payload)

// 更新指定心愿条目
export const updateWishlist = (
  id: number,
  payload: {
    name: string
    category?: string
    brand?: string
    model?: string
    expectedPrice?: number
    plannedPlatform?: string
    link?: string
    notes?: string
    priority?: number
  }
) => http.put<void>(`/wishlist/${id}`, payload)

// 删除心愿条目
export const deleteWishlist = (id: number) => http.delete<void>(`/wishlist/${id}`)

// 转换心愿条目为物品，返回新物品 ID
export const convertWishlist = (id: number, payload: AssetPayload) => http.post<number>(`/wishlist/${id}/convert`, payload)
