import http from './http'
import type { ImageSearchProvidersResponse } from '@/types'

export const fetchImageSearchProviders = () =>
  http.get<ImageSearchProvidersResponse>('/image-search/providers')

export const updateDefaultImageSearchProvider = (provider?: string | null) =>
  http.put<void>('/image-search/providers/default', { provider })
