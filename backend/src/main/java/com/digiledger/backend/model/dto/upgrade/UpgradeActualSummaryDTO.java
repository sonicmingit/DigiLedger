package com.digiledger.backend.model.dto.upgrade;

import java.math.BigDecimal;

/**
 * 路线真实收支汇总。
 * <p>仅统计去重后的真实物品节点，计划预算不会混入该汇总。</p>
 */
public record UpgradeActualSummaryDTO(
        int assetCount,
        BigDecimal totalSpend,
        BigDecimal primarySpend,
        BigDecimal extraSpend,
        BigDecimal totalIncome,
        BigDecimal netInvestment,
        BigDecimal dailyCost
) {
}
