// 资产状态字典
export type AssetStatus = '使用中' | '已闲置' | '待出售' | '已出售' | '已丢弃'

export type SaleScope = 'ASSET' | 'ACCESSORY'

export interface CoverSuggestion {
  thumbUrl: string
  sourceUrl: string
  source?: string
  title?: string
  extra?: Record<string, any>
}

export interface CoverApplyResult {
  attachmentId: number
  url: string
  objectKey: string
}

export interface RemoveBgResult {
  attachmentId: number
  url: string
}

export interface TagItem {
  id: number
  name: string
  color?: string
  icon?: string
}

export interface BrandInfo {
  id: number | null
  name: string
  alias?: string | null
  initial?: string | null
  sort?: number | null
}

export interface AssetSummary {
  id: number
  name: string
  categoryId?: number
  categoryPath?: string
  brandName?: string
  status: AssetStatus
  coverImageUrl?: string
  totalInvest: number
  avgCostPerDay: number
  useDays: number
  lastNetIncome: number
  purchaseDate?: string
  primaryPrice?: number
  primaryPurchaseDate?: string
  tags: TagItem[]
}

export interface PurchaseRecord {
  id: number
  type: 'PRIMARY' | 'ACCESSORY' | 'SERVICE'
  name?: string
  platformId?: number
  platformName?: string
  seller?: string
  price: number
  shippingCost: number
  quantity: number
  purchaseDate: string
  warrantyMonths?: number
  warrantyExpireDate?: string
  productLink?: string
  attachments: string[]
  notes?: string
}

export interface SaleRecord {
  id: number
  saleScope: SaleScope
  purchaseId?: number
  platformId?: number
  platformName?: string
  buyer?: string
  salePrice: number
  fee: number
  shippingCost: number
  otherCost: number
  netIncome: number
  saleDate: string
  useDays: number
  lossAmount: number
  dailyUsageCost: number
  monthlyUsageCost: number
  attachments: string[]
  notes?: string
}

export interface WishlistAssetRef {
  assetId: number
  assetName?: string | null
  available: boolean
}

export interface AssetDetail extends AssetSummary {
  brand?: BrandInfo | null
  model?: string
  serialNo?: string
  retiredDate?: string
  notes?: string
  purchases: PurchaseRecord[]
  sales: SaleRecord[]
}

export interface WishlistItem {
  id: number
  name: string
  category?: string
  categoryId?: number
  categoryName?: string
  brandId?: number
  brandName?: string
  model?: string
  expectedPrice?: number
  link?: string
  notes?: string
  priority?: number
  status: '待购买' | '已完成' | '已购买'
  imageUrl?: string
  tags?: TagItem[]
  convertedAssetId?: number
  relatedAssets?: WishlistAssetRef[]
  createdAt: string
  updatedAt: string
}
