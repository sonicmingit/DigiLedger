export type AssetStatus = '使用中' | '已闲置' | '待出售' | '已出售' | '已丢弃'
export type PurchaseType = 'PRIMARY' | 'ACCESSORY' | 'SERVICE'
/** 升级路线节点的执行状态；实际物品也可能直接复用物品状态。 */
export type UpgradeNodeStatus = 'PLANNED' | 'READY' | 'EXECUTING' | 'COMPLETED' | 'CANCELLED'
/** 路线的资料来源口径，避免未来计划与真实支出混算。 */
export type UpgradeRouteType = 'ACTUAL' | 'PLAN' | 'MIXED'
/** 路线生命周期状态。 */
export type UpgradeRouteStatus = 'ACTIVE' | 'COMPLETED' | 'ARCHIVED'
/** 节点来源：真实物品或尚未购买的心愿物品；旧 PLANNED 仅为历史数据兼容。 */
export type UpgradeNodeType = 'ASSET' | 'WISHLIST' | 'PLANNED'
/** 路线边的业务语义。 */
export type UpgradeRelationType = 'SEQUENCE' | 'ALTERNATIVE'

export interface ApiEnvelope<T> { code: number; data: T; msg: string }
export interface TagItem { id: number; name: string; color?: string; icon?: string }
export interface AssetRelatedLink { url: string; description?: string }
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
  brand?: BrandInfo | null; model?: string; serialNo?: string; retiredDate?: string; notes?: string; relatedLinks?: AssetRelatedLink[]; manualUseMonths?: number
  purchases: PurchaseRecord[]; sales: SaleRecord[]
}
export interface AssetPayload {
  name: string; categoryId: number; brandId?: number; brand?: string; model?: string; serialNo?: string
  status: AssetStatus; purchaseDate?: string; retiredDate?: string; coverImageUrl?: string; notes?: string
  relatedLinks?: AssetRelatedLink[]; manualUseMonths?: number
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
  lastPriceAt?: string; link?: string; source?: string; notes?: string; priority?: number; status: '未购买' | '待购买' | '已完成' | '已购买'
  imageUrl?: string; tags?: TagItem[]; convertedAssetId?: number; purchasedAt?: string; purchasedPrice?: number; purchasePriceDiff?: number; createdAt: string; updatedAt: string
}
export interface WishlistPayload {
  name: string; categoryId?: number; brandId?: number; model?: string; expectedPrice?: number; currentPrice?: number; link?: string
  source?: string; notes?: string; priority?: number; imageUrl?: string; tagIds?: number[]; relatedAssetId?: number
}
export interface PricePoint { price: number; capturedAt: string }
export interface UpgradeRouteItem {
  id: number; name: string; rootAssetId?: number | null; rootAssetName?: string | null; remark?: string | null
  mainAssetId?: number | null; periodStart?: string | null; periodEnd?: string | null; coverImageUrls?: string[]
  planYear?: number; annualBudget?: number; plannedBudget?: number; expectedRecovery?: number; updatedAt: string
  /** total* 字段是服务端列表 DTO 的命名；旧页面仍可能返回简写字段。 */
  totalPlannedBudget?: number; totalExpectedRecovery?: number
  routeType?: UpgradeRouteType; status?: UpgradeRouteStatus; actualSummary?: UpgradeActualSummary; planSummary?: UpgradePlanSummary
}
/** 路线内真实物品的汇总，所有金额均来自物品与出售记录。 */
export interface UpgradeActualSummary {
  assetCount: number; totalSpend: number; primarySpend: number; extraSpend: number; totalIncome: number; netInvestment: number; dailyCost?: number
}
/** 计划物品的独立汇总，不参与真实净投入。 */
export interface UpgradePlanSummary { plannedBudget: number; expectedRecovery: number; expectedNetInvestment?: number }
export interface UpgradeGraphNode {
  nodeId: number; assetId?: number; name?: string; title?: string; targetName?: string; status: UpgradeNodeStatus | AssetStatus
  purchasePrice?: number; salePrice?: number; sold?: boolean; periodLabel?: string; plannedBudget?: number
  expectedRecovery?: number; level?: number; sort?: number; label?: string | null; remark?: string | null
  nodeType?: UpgradeNodeType; brandName?: string; model?: string; coverImageUrl?: string; assetStatus?: AssetStatus
  purchaseDate?: string; primaryPurchaseAmount?: number; totalInvest?: number; useDays?: number
  mainSaleDate?: string; mainSalePrice?: number; mainSaleNetIncome?: number; dataWarnings?: string[]
  wishlistId?: number; mainline?: boolean; alternativePurpose?: string
}
export interface UpgradeGraphLink {
  linkId: number; fromNodeId: number; toNodeId: number; stepCost?: number; remark?: string | null
  relationType?: UpgradeRelationType; purchaseGapDays?: number | null; purchasePriceDelta?: number | null
  replacementNetOutflow?: number | null; calculationStatus?: string
}
/** 路线图接口。新字段均为可选，便于平滑读取旧后端返回。 */
export interface UpgradeRouteGraph {
  routeId: number; routeName: string; remark?: string | null; nodes: UpgradeGraphNode[]; links: UpgradeGraphLink[]
  route?: UpgradeRouteItem; actualSummary?: UpgradeActualSummary; planSummary?: UpgradePlanSummary; warnings?: string[]
}
export interface CategoryNode { id: number; name: string; parentId: number | null; level: number; sort: number; children: CategoryNode[] }
export interface TagNode { id: number; name: string; parentId: number | null; color?: string; icon?: string; sort: number; children: TagNode[] }
export interface BrandItem { id: number; name: string; alias?: string; initial?: string; sort?: number }
export interface PlatformItem { id: number; name: string; link?: string; sort: number }
export interface Preferences { currency: string; dateFormat: string; autoBackupEnabled: boolean; autoBackupTime: string }
export interface ExternalApiConfig {
  apiCode: string; displayName: string; baseUrl: string; authType: string; apiKeyConfigured: boolean
  maskedApiKey?: string | null; configJson?: string | null; timeoutMs: number; enabled: boolean
}
export interface MtPhotosSearchResponse {
  mode: 'KEYWORD' | 'CLIP'; totalCount: number; page: number; pageSize: number; totalPages: number
  items: MtPhotosSearchItem[]
}
export interface MtPhotosSearchItem {
  id: number; fileName?: string | null; capturedAt?: string | null; fileType?: string | null; thumbnailUrl: string
}
export interface ExternalApiTestItem {
  thumbnailUrl?: string | null; originalUrl?: string | null; title?: string | null; sourceUrl?: string | null
}
export interface ExternalApiTestResponse {
  apiCode: string; success: boolean; message: string; resultCount: number; items: ExternalApiTestItem[]
}
export interface ImageSearchProvider {
  name: string; displayName: string; description: string; available: boolean
}
export interface ImageSearchProvidersResponse {
  providers: ImageSearchProvider[]; enabledProviders: string[]; defaultProvider?: string | null
}
