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
        String rootAssetName,
        String remark,
        Integer planYear,
        BigDecimal annualBudget,
        BigDecimal totalPlannedBudget,
        BigDecimal totalExpectedRecovery,
        Map<String, Long> statusDistribution,
        LocalDateTime updatedAt
) {
}
