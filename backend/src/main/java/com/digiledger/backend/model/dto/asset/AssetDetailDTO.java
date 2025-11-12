package com.digiledger.backend.model.dto.asset;

import com.digiledger.backend.model.dto.dict.BrandDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 资产详情响应体。
 */
public record AssetDetailDTO(
        Long id,
        String name,
        Long categoryId,
        String categoryPath,
        BrandDTO brand,
        String model,
        String serialNo,
        String status,
        LocalDate purchaseDate,
        LocalDate enabledDate,
        LocalDate retiredDate,
        String coverImageUrl,
        String notes,
        List<TagDTO> tags,
        BigDecimal totalInvest,
        long useDays,
        BigDecimal avgCostPerDay,
        BigDecimal lastNetIncome,
        List<PurchaseDTO> purchases,
        List<SaleDTO> sales
) {
}
