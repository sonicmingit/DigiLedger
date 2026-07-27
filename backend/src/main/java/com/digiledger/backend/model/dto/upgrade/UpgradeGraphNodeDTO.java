package com.digiledger.backend.model.dto.upgrade;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 升级路线图节点视图。
 */
public record UpgradeGraphNodeDTO(
        Long nodeId,
        Long assetId,
        String name,
        String assetStatus,
        BigDecimal purchasePrice,
        BigDecimal salePrice,
        boolean sold,
        LocalDate purchaseDate,
        String coverImageUrl,
        Integer level,
        Integer sort,
        String label,
        String remark,
        String title,
        String targetName,
        String periodLabel,
        BigDecimal plannedBudget,
        BigDecimal expectedRecovery,
        String status,
        String nodeType,
        String brandName,
        String model,
        BigDecimal primaryPurchaseAmount,
        BigDecimal totalInvest,
        Long useDays,
        LocalDate mainSaleDate,
        BigDecimal mainSalePrice,
        BigDecimal mainSaleNetIncome,
        java.util.List<String> dataWarnings,
        String alternativePurpose,
        Long wishlistId,
        boolean mainline
) {
}
