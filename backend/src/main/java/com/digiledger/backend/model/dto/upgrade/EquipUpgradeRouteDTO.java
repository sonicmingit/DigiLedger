package com.digiledger.backend.model.dto.upgrade;

import java.time.LocalDateTime;

/**
 * 升级路线列表展示 DTO。
 */
public record EquipUpgradeRouteDTO(
        Long id,
        String name,
        Long rootAssetId,
        String rootAssetName,
        String remark,
        LocalDateTime updatedAt
) {
}
