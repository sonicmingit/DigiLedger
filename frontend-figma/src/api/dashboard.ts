import { http } from './http'
import type { DashboardSummary } from '@/types'
export const fetchDashboardSummary = () => http.get<DashboardSummary>('/dashboard/summary')
