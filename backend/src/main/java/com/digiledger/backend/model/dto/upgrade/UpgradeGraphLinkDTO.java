package com.digiledger.backend.model.dto.upgrade;

import java.math.BigDecimal;

/**
 * 升级路线图连线视图。
 */
public record UpgradeGraphLinkDTO(
        Long linkId,
        Long fromNodeId,
        Long toNodeId,
        BigDecimal stepCost,
        String remark,
        String relationType,
        Long purchaseGapDays,
        BigDecimal purchasePriceDelta,
        BigDecimal replacementNetOutflow,
        String calculationStatus
) {
}
