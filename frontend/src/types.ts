// 物品状态枚举（中文）
export type AssetStatus = '使用中' | '已闲置' | '待出售' | '已出售' | '已丢弃'

export interface TagItem {
  id: number
  name: string
  color?: string
  icon?: string
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
  tags: TagItem[]
}

// 购买记录
export interface PurchaseRecord {
  id: number
  type: 'PRIMARY' | 'ACCESSORY' | 'SERVICE'
  platformId?: number
  platformName?: string
  seller?: string
  price: number
  shippingCost: number
  currency: string
  quantity: number
  purchaseDate: string
  invoiceNo?: string
  warrantyMonths?: number
  warrantyExpireDate?: string
  attachments: string[]
  notes?: string
}

// 售出记录
export interface SaleRecord {
  id: number
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
  brand?: string
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
  brand?: string
  model?: string
  expectedPrice?: number
  plannedPlatform?: string
  link?: string
  notes?: string
  priority?: number
  convertedAssetId?: number
  createdAt: string
  updatedAt: string
}
