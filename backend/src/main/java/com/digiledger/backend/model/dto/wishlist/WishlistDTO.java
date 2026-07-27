package com.digiledger.backend.model.dto.wishlist;

import com.digiledger.backend.model.dto.asset.TagDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;

/**
 * 心愿单响应 DTO。
 */
public record WishlistDTO(
        Long id,
        String name,
        Long categoryId,
        String categoryName,
        Long brandId,
        String brandName,
        String model,
        BigDecimal expectedPrice,
        BigDecimal currentPrice,
        BigDecimal priceChangeRate,
        LocalDateTime lastPriceAt,
        String imageUrl,
        String status,
        String link,
        String source,
        String notes,
        Integer priority,
        List<TagDTO> tags,
        Long convertedAssetId,
        LocalDate purchasedAt,
        BigDecimal purchasedPrice,
        BigDecimal purchasePriceDiff,
        List<WishlistAssetRefDTO> relatedAssets,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
