import { http } from './http'
import type { AssetPayload, PricePoint, WishlistItem, WishlistPayload } from '@/types'
export const fetchWishlist = () => http.get<WishlistItem[]>('/wishlist')
export const createWishlist = (payload: WishlistPayload) => http.post<number>('/wishlist', payload)
export const updateWishlist = (id: number, payload: WishlistPayload) => http.put<void>(`/wishlist/${id}`, payload)
export const deleteWishlist = (id: number) => http.delete<void>(`/wishlist/${id}`)
export const convertWishlist = (id: number, payload: AssetPayload) => http.post<number>(`/wishlist/${id}/convert`, payload)
export const updateWishlistPrice = (id: number, currentPrice: number) => http.patch<void>(`/wishlist/${id}/price`, { currentPrice, capturedAt: new Date().toISOString() })
export const fetchPriceHistory = (id: number) => http.get<PricePoint[]>(`/wishlist/${id}/price-history`)
export const markWishlistPurchased = (id: number) => http.post<void>(`/wishlist/${id}/mark-purchased`)
