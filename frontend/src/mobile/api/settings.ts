import http from '@/api/http'

export interface MobilePreferencePayload {
  viewMode: 'card' | 'list'
  currency: 'CNY' | 'USD' | 'EUR'
  thousandSeparator: boolean
  decimalPlaces: number
  includeRetired: boolean
  includeSold: boolean
}

export interface MobileUserProfile {
  id: number
  nickname: string
  avatar?: string
  level?: string
  email?: string
}

export const fetchMobilePreferences = () => http.get<MobilePreferencePayload>('/settings/preferences/mobile')
export const updateMobilePreferences = (payload: MobilePreferencePayload) =>
  http.put<void>('/settings/preferences/mobile', payload)

export const fetchMobileProfile = () => http.get<MobileUserProfile>('/users/me')
