/**
 * 计算质保到期日
 * 采用“对齐原日期所在月份的最后一天”策略，避免 1 月 31 日 + 1 个月出现 3 月 2 日的跳月问题
 * @param purchaseDate 购买日期（YYYY-MM-DD）
 * @param months 质保月数
 */
export const calcWarrantyExpireDate = (purchaseDate?: string, months?: number | null): string => {
  if (!purchaseDate || !purchaseDate.trim()) {
    return ''
  }
  if (months == null || Number.isNaN(months)) {
    return ''
  }
  const normalized = purchaseDate.trim()
  if (!months || months <= 0) {
    return normalized
  }
  const [year, month, day] = normalized.split('-').map((value) => Number(value))
  if ([year, month, day].some((item) => Number.isNaN(item))) {
    return normalized
  }
  // 先定位到目标月份的第一天，再将日期对齐到该月实际存在的最大天数
  const target = new Date(year, month - 1, 1)
  target.setMonth(target.getMonth() + months)
  const lastDayOfTargetMonth = new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate()
  const safeDay = Math.min(day, lastDayOfTargetMonth)
  const result = new Date(target.getFullYear(), target.getMonth(), safeDay)
  const targetYear = result.getFullYear()
  const targetMonth = String(result.getMonth() + 1).padStart(2, '0')
  const targetDay = String(result.getDate()).padStart(2, '0')
  return `${targetYear}-${targetMonth}-${targetDay}`
}
