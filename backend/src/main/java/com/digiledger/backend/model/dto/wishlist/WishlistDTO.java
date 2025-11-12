package com.digiledger.backend.model.dto.wishlist;

import java.time.LocalDateTime;

/**
 * 心愿单响应 DTO。
 */
public record WishlistDTO(
        Long id,
        String name,
        Long categoryId,
        Long brandId,
        String imageUrl,
        String status,
        String link,
        String notes,
        Integer priority,
        Long convertedAssetId,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
