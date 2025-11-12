package com.digiledger.backend.model.dto.wishlist;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 心愿单响应 DTO。
 */
public record WishlistDTO(
        Long id,
        String name,
        Long categoryId,
        Long brandId,
        String model,
        BigDecimal expectedPrice,
        String imageUrl,
        String link,
        String status,
        String notes,
        Long convertedAssetId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
