package com.digiledger.backend.model.dto.asset;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 资产列表摘要数据。
 */
public record AssetSummaryDTO(
        Long id,
        String name,
        Long categoryId,
        String categoryPath,
        String status,
        String coverImageUrl,
        BigDecimal totalInvest,
        BigDecimal avgCostPerDay,
        long useDays,
        BigDecimal lastNetIncome,
        LocalDate enabledDate,
        LocalDate purchaseDate,
        List<TagDTO> tags
) {
}
