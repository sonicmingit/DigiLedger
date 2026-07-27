package com.digiledger.backend.model.dto.upgrade;

import java.math.BigDecimal;

/** 计划节点预算汇总，与真实收支独立计算。 */
public record UpgradePlanSummaryDTO(
        BigDecimal plannedBudget,
        BigDecimal expectedRecovery,
        BigDecimal expectedNetInvestment
) {
}
