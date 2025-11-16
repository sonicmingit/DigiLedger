package com.digiledger.backend.model.dto.wishlist;

import com.digiledger.backend.model.dto.asset.TagDTO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
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
        String imageUrl,
        String status,
        String link,
        String notes,
        Integer priority,
        List<TagDTO> tags,
        Long convertedAssetId,
        List<WishlistAssetRefDTO> relatedAssets,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
