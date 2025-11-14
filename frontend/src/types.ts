// 物品状态枚举（中文）
export type AssetStatus = '使用中' | '已闲置' | '待出售' | '已出售' | '已丢弃'

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

// 物品列表摘要
export interface AssetSummary {
  id: number
  name: string
  categoryId?: number
  categoryPath?: string
  status: AssetStatus
  coverImageUrl?: string
  totalInvest: number
  avgCostPerDay: number
  useDays: number
  lastNetIncome: number
  enabledDate: string
  purchaseDate?: string
  primaryPrice?: number
  primaryPurchaseDate?: string
  tags: TagItem[]
}

// 购买记录
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
  attachments: string[]
  notes?: string
}

// 售出记录
export interface SaleRecord {
  id: number
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
  attachments: string[]
  notes?: string
}

// 物品详情
export interface AssetDetail extends AssetSummary {
  brand?: BrandInfo | null
  model?: string
  serialNo?: string
  retiredDate?: string
  notes?: string
  purchases: PurchaseRecord[]
  sales: SaleRecord[]
}

// 心愿单
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
  status: '未购买' | '已购买'
  imageUrl?: string
  tags?: TagItem[]
  convertedAssetId?: number
  createdAt: string
  updatedAt: string
}
