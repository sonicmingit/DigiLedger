import http from './http'
import type { UpgradeRouteItem, UpgradeRouteGraph } from '@/types'

export type UpgradeRoutePayload = {
  name: string
  rootAssetId?: number | null
  remark?: string | null
}

export type UpgradeNodePayload = {
  assetId: number
  level?: number
  sort?: number
  label?: string
  remark?: string
}

export type UpgradeLinkPayload = {
  fromNodeId: number
  toNodeId: number
  remark?: string
}

export const fetchUpgradeRoutes = () => http.get<UpgradeRouteItem[]>('/upgrade-routes')

export const createUpgradeRoute = (payload: UpgradeRoutePayload) => http.post<number>('/upgrade-routes', payload)

export const updateUpgradeRoute = (id: number, payload: UpgradeRoutePayload) =>
  http.put<void>(`/upgrade-routes/${id}`, payload)

export const deleteUpgradeRoute = (id: number) => http.delete<void>(`/upgrade-routes/${id}`)

export const fetchUpgradeGraph = (routeId: number) =>
  http.get<UpgradeRouteGraph>(`/upgrade-routes/${routeId}/graph`)

export const addUpgradeNode = (routeId: number, payload: UpgradeNodePayload) =>
  http.post<number>(`/upgrade-routes/${routeId}/nodes`, payload)

export const deleteUpgradeNode = (routeId: number, nodeId: number) =>
  http.delete<void>(`/upgrade-routes/${routeId}/nodes/${nodeId}`)

export const addUpgradeLink = (routeId: number, payload: UpgradeLinkPayload) =>
  http.post<number>(`/upgrade-routes/${routeId}/links`, payload)

export const deleteUpgradeLink = (routeId: number, linkId: number) =>
  http.delete<void>(`/upgrade-routes/${routeId}/links/${linkId}`)
