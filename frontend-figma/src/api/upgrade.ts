import { http } from './http'
import type { UpgradeNodeStatus, UpgradeRouteGraph, UpgradeRouteItem } from '@/types'
export interface RoutePayload { name: string; rootAssetId?: number | null; remark?: string | null; planYear?: number; annualBudget?: number }
export interface NodePayload { assetId?: number; title: string; targetName?: string; periodLabel?: string; plannedBudget?: number; expectedRecovery?: number; status?: UpgradeNodeStatus; level?: number; sort?: number; label?: string; remark?: string }
export const fetchRoutes = () => http.get<UpgradeRouteItem[]>('/upgrade-routes')
export const fetchRouteGraph = (id: number) => http.get<UpgradeRouteGraph>(`/upgrade-routes/${id}/graph`)
export const createRoute = (payload: RoutePayload) => http.post<number>('/upgrade-routes', payload)
export const updateRoute = (id: number, payload: RoutePayload) => http.put<void>(`/upgrade-routes/${id}`, payload)
export const deleteRoute = (id: number) => http.delete<void>(`/upgrade-routes/${id}`)
export const createRouteNode = (routeId: number, payload: NodePayload) => http.post<number>(`/upgrade-routes/${routeId}/nodes`, payload)
export const updateRouteNode = (routeId: number, nodeId: number, payload: NodePayload) => http.put<void>(`/upgrade-routes/${routeId}/nodes/${nodeId}`, payload)
export const deleteRouteNode = (routeId: number, nodeId: number) => http.delete<void>(`/upgrade-routes/${routeId}/nodes/${nodeId}`)
export const createRouteLink = (routeId: number, payload: { fromNodeId: number; toNodeId: number; remark?: string }) => http.post<number>(`/upgrade-routes/${routeId}/links`, payload)
export const deleteRouteLink = (routeId: number, linkId: number) => http.delete<void>(`/upgrade-routes/${routeId}/links/${linkId}`)
