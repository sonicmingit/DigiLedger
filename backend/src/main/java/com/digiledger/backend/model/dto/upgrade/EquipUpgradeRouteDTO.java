package com.digiledger.backend.model.dto.upgrade;

import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.Map;

/**
 * 升级路线列表展示 DTO。
 */
public record EquipUpgradeRouteDTO(
        Long id,
        String name,
        Long rootAssetId,
        Long mainAssetId,
        String rootAssetName,
        String remark,
        Integer planYear,
        BigDecimal annualBudget,
        BigDecimal totalPlannedBudget,
        BigDecimal totalExpectedRecovery,
        Map<String, Long> statusDistribution,
        LocalDateTime updatedAt,
        String routeType,
        String status,
        UpgradeActualSummaryDTO actualSummary,
        UpgradePlanSummaryDTO planSummary,
        java.time.LocalDate periodStart,
        java.time.LocalDate periodEnd,
        java.util.List<String> coverImageUrls
) {
}
