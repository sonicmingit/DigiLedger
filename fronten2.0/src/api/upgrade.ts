import { http } from './http'
import type { UpgradeNodeStatus, UpgradeNodeType, UpgradeRelationType, UpgradeRouteGraph, UpgradeRouteItem, UpgradeRouteStatus, UpgradeRouteType } from '@/types'

/**
 * 升级路线写入模型。firstNode 是新版原子创建流程的唯一入口；rootAssetId
 * 仅为仍在使用旧接口的服务端保留，前端界面不会再要求用户输入 ID。
 */
export interface RoutePayload {
  name: string; rootAssetId?: number | null; mainAssetId?: number | null; remark?: string | null; planYear?: number; annualBudget?: number
  routeType?: UpgradeRouteType; status?: UpgradeRouteStatus; firstNode?: { nodeType: UpgradeNodeType; assetId?: number; wishlistId?: number }
}
/** 锚点式新增节点；position 让服务端在同一事务内创建对应关系。 */
export interface NodePayload {
  assetId?: number; wishlistId?: number; title?: string; targetName?: string; periodLabel?: string; plannedBudget?: number; expectedRecovery?: number
  status?: UpgradeNodeStatus; level?: number; sort?: number; label?: string; remark?: string
  anchorNodeId?: number; position?: 'BEFORE' | 'ALTERNATIVE' | 'AFTER'; nodeType?: UpgradeNodeType; alternativePurpose?: string; mainline?: boolean
}
/** 新版锚点接口的原子写入结果；graph 可立即用于刷新局部画布。 */
export interface CreateRouteNodeResult { nodeId: number; linkId?: number; graph?: UpgradeRouteGraph }
export const fetchRoutes = () => http.get<UpgradeRouteItem[]>('/upgrade-routes')
export const fetchRouteGraph = (id: number) => http.get<UpgradeRouteGraph>(`/upgrade-routes/${id}/graph`)
export const createRoute = (payload: RoutePayload) => http.post<number | UpgradeRouteGraph>('/upgrade-routes', payload)
export const updateRoute = (id: number, payload: RoutePayload) => http.put<void>(`/upgrade-routes/${id}`, payload)
export const deleteRoute = (id: number) => http.delete<void>(`/upgrade-routes/${id}`)
export const createRouteNode = (routeId: number, payload: NodePayload) => http.post<number | CreateRouteNodeResult>(`/upgrade-routes/${routeId}/nodes`, payload)
export const updateRouteNode = (routeId: number, nodeId: number, payload: NodePayload) => http.put<void>(`/upgrade-routes/${routeId}/nodes/${nodeId}`, payload)
export const deleteRouteNode = (routeId: number, nodeId: number) => http.delete<void>(`/upgrade-routes/${routeId}/nodes/${nodeId}`)
export const createRouteLink = (routeId: number, payload: { fromNodeId: number; toNodeId: number; remark?: string; relationType?: UpgradeRelationType }) => http.post<number>(`/upgrade-routes/${routeId}/links`, payload)
export const deleteRouteLink = (routeId: number, linkId: number) => http.delete<void>(`/upgrade-routes/${routeId}/links/${linkId}`)
