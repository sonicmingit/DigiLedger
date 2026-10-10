import type { AssetPredecessor, PurchaseRecord } from '@/types'

const DAY_MS = 86400000
const dateValue = (value?: string | null) => {
  if (!value || !/^\d{4}-\d{2}-\d{2}$/.test(value)) return null
  const timestamp = Date.parse(`${value}T00:00:00Z`)
  return Number.isFinite(timestamp) && new Date(timestamp).toISOString().slice(0, 10) === value ? timestamp : null
}
const amount = (value: unknown) => value !== undefined && value !== null && value !== '' && Number.isFinite(Number(value)) ? Number(value) : null
/** 与后端主商品口径一致：最早购买日期，同日取较新的记录。 */
export const primaryPurchaseRecord = (purchases: PurchaseRecord[] = []) => purchases
  .filter(record => record.type === 'PRIMARY')
  .sort((left, right) => (dateValue(left.purchaseDate) ?? Infinity) - (dateValue(right.purchaseDate) ?? Infinity) || (right.id ?? 0) - (left.id ?? 0))[0]
export const purchaseComparison = (current: Pick<PurchaseRecord, 'price' | 'purchaseDate'> | undefined, previous: AssetPredecessor) => {
  const currentPrice = amount(current?.price), previousPrice = amount(previous.primaryPrice)
  const currentDate = dateValue(current?.purchaseDate), previousDate = dateValue(previous.primaryPurchaseDate)
  return {
    delta: currentPrice === null || previousPrice === null ? null : Math.round((currentPrice - previousPrice) * 100) / 100,
    gap: currentDate === null || previousDate === null ? null : Math.round((currentDate - previousDate) / DAY_MS)
  }
}
export const nullableMoney = (value: unknown) => {
  const parsed = amount(value)
  return parsed === null ? '未填写' : `¥ ${parsed.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`
}
export const signedMoney = (value: number | null) => value === null ? '暂无法计算' : `${value > 0 ? '+' : value < 0 ? '−' : ''}${nullableMoney(Math.abs(value))}`
