import { http } from './http'
import type { DashboardSummary, DashboardSpending, PurchaseType } from '@/types'
export const fetchDashboardSummary = () => http.get<DashboardSummary>('/dashboard/summary')
export interface SpendingQuery {
  dateFrom?: string; dateTo?: string; categoryId?: number; type?: PurchaseType
  platformId?: number; q?: string; page?: number; pageSize?: number
}
export const fetchDashboardSpending = (query: SpendingQuery) =>
  http.get<DashboardSpending>('/dashboard/spending', { params: query })
