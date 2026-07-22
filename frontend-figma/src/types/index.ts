export type AssetStatus = '使用中' | '已闲置' | '待出售' | '已出售' | '已丢弃'
export type PurchaseType = 'PRIMARY' | 'ACCESSORY' | 'SERVICE'
export type UpgradeNodeStatus = 'PLANNED' | 'READY' | 'EXECUTING' | 'COMPLETED' | 'CANCELLED'

export interface ApiEnvelope<T> { code: number; data: T; msg: string }
export interface TagItem { id: number; name: string; color?: string; icon?: string }
export interface BrandInfo { id: number | null; name: string; alias?: string | null; initial?: string | null }
export interface PurchaseRecord {
  id?: number; type: PurchaseType; name?: string; platformId?: number; platformName?: string; seller?: string
  price: number; shippingCost?: number; quantity?: number; purchaseDate: string; warrantyMonths?: number
  warrantyExpireDate?: string; productLink?: string; attachments?: string[]; notes?: string
}
export interface SaleRecord {
  id: number; saleScope: 'ASSET' | 'ACCESSORY'; purchaseId?: number; platformId?: number; platformName?: string
  buyer?: string; salePrice: number; fee: number; shippingCost: number; otherCost: number; netIncome: number
  saleDate: string; useDays: number; lossAmount: number; dailyUsageCost: number; monthlyUsageCost: number
  attachments: string[]; notes?: string
}
export interface AssetSummary {
  id: number; name: string; categoryId?: number; categoryPath?: string; brandName?: string; status: AssetStatus
  coverImageUrl?: string; totalInvest: number; avgCostPerDay: number; useDays: number; lastNetIncome: number
  purchaseDate?: string; primaryPrice?: number; primaryPurchaseDate?: string; tags?: TagItem[]
}
export interface AssetPage { records: AssetSummary[]; total: number; page: number; pageSize: number }
export interface AssetDetail extends AssetSummary {
  brand?: BrandInfo | null; model?: string; serialNo?: string; retiredDate?: string; notes?: string
  purchases: PurchaseRecord[]; sales: SaleRecord[]
}
export interface AssetPayload {
  name: string; categoryId: number; brandId?: number; brand?: string; model?: string; serialNo?: string
  status: AssetStatus; purchaseDate?: string; retiredDate?: string; coverImageUrl?: string; notes?: string
  tagIds?: number[]; targetCostStrategy?: 'NONE' | 'PRICE' | 'DATE' | 'CUSTOM'; targetCostValue?: number
  attachAssetIds?: number[]; purchases?: PurchaseRecord[]
}
export interface DashboardSummary {
  totalAssetValue: number; assetCount: number; activeCount: number; idleCount: number; pendingSaleCount: number
  avgDailyCost: number; monthValueChangeRate: number; monthCostChangeRate: number
  statusDistribution: Array<{ status: string; count: number }>
  categoryDistribution: Array<{ categoryId: number; categoryName: string; value: number; count: number }>
  valueTrend: Array<{ month: string; value: number }>; recentAssets: AssetSummary[]
}
export interface WishlistItem {
  id: number; name: string; categoryId?: number; categoryName?: string; category?: string; brandId?: number
  brandName?: string; model?: string; expectedPrice?: number; currentPrice?: number; priceChangeRate?: number
  lastPriceAt?: string; link?: string; notes?: string; priority?: number; status: '待购买' | '已完成' | '已购买'
  imageUrl?: string; tags?: TagItem[]; convertedAssetId?: number; createdAt: string; updatedAt: string
}
export interface WishlistPayload {
  name: string; categoryId?: number; brandId?: number; model?: string; expectedPrice?: number; link?: string
  notes?: string; priority?: number; imageUrl?: string; tagIds?: number[]; relatedAssetId?: number
}
export interface PricePoint { price: number; capturedAt: string }
export interface UpgradeRouteItem {
  id: number; name: string; rootAssetId?: number | null; rootAssetName?: string | null; remark?: string | null
  planYear?: number; annualBudget?: number; plannedBudget?: number; expectedRecovery?: number; updatedAt: string
}
export interface UpgradeGraphNode {
  nodeId: number; assetId?: number; name?: string; title?: string; targetName?: string; status: UpgradeNodeStatus | AssetStatus
  purchasePrice?: number; salePrice?: number; sold?: boolean; periodLabel?: string; plannedBudget?: number
  expectedRecovery?: number; level?: number; sort?: number; label?: string | null; remark?: string | null
}
export interface UpgradeGraphLink { linkId: number; fromNodeId: number; toNodeId: number; stepCost?: number; remark?: string | null }
export interface UpgradeRouteGraph { routeId: number; routeName: string; remark?: string | null; nodes: UpgradeGraphNode[]; links: UpgradeGraphLink[] }
export interface CategoryNode { id: number; name: string; parentId: number | null; level: number; sort: number; children: CategoryNode[] }
export interface TagNode { id: number; name: string; parentId: number | null; color?: string; icon?: string; sort: number; children: TagNode[] }
export interface BrandItem { id: number; name: string; alias?: string; initial?: string; sort?: number }
export interface PlatformItem { id: number; name: string; link?: string; sort: number }
export interface Preferences { currency: string; dateFormat: string; autoBackupEnabled: boolean; autoBackupTime: string }
