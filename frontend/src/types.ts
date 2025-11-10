// 资产状态枚举，对应设计文档中的状态机
export type AssetStatus = 'IN_USE' | 'IDLE' | 'FOR_SALE' | 'SOLD' | 'RETIRED'

export type PurchaseType = 'PRIMARY' | 'ACCESSORY' | 'SERVICE'

// 资产购买记录的前端模型
export interface PurchaseRecord {
  id: number
  assetId: number
  name: string
  type: PurchaseType
  platform?: string
  price: number
  shippingCost?: number
  purchaseDate: string
  warrantyExpireDate?: string
  warrantyChannel?: string
  notes?: string
}

// 维护记录的前端模型
export interface MaintenanceRecord {
  id: number
  assetId: number
  title: string
  cost: number
  date: string
  notes?: string
  type?: string
}

// 售出记录的前端模型
export interface SaleRecord {
  id: number
  assetId: number
  platform?: string
  saleDate: string
  salePrice: number
  fee?: number
  shippingCost?: number
  otherCost?: number
  netIncome: number
  realizedPnl: number
  notes?: string
}

// 仪表盘提醒模块使用的轻量数据模型
export interface ReminderItem {
  assetId: number
  name: string
  type: 'WARRANTY' | 'SALE' | 'MAINTENANCE'
  dueDate: string
  daysLeft: number
  message: string
}

// 仪表盘界面使用的资产汇总对象
export interface AssetSummary {
  id: number
  name: string
  category: string
  brand?: string
  model?: string
  serialNo?: string
  status: AssetStatus
  wishStatus?: string
  totalInvest: number
  avgCostPerDay: number
  accumulatedDepreciation: number
  bookValue: number
  enabledDate: string
  purchaseDate?: string
  purchasePlatform?: string
  annualRate: number
  useDays: number
  warrantyChannel?: string
  warrantyExpireDate?: string
  tags?: string[]
  relatedAssets?: number[]
  purchaseEvidence?: string
  coverImageUrl?: string
  notes?: string
  retiredDate?: string
  purchases: PurchaseRecord[]
  maintenanceRecords: MaintenanceRecord[]
  saleRecords: SaleRecord[]
}
