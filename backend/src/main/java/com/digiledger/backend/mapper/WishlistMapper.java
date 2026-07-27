package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.WishlistItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 心愿单 Mapper。
 */
@Mapper
public interface WishlistMapper {

    List<WishlistItem> findAll(@Param("status") String status);

    WishlistItem findById(@Param("id") Long id);

    int insert(WishlistItem item);

    int update(WishlistItem item);

    int delete(@Param("id") Long id);
    int markConverted(@Param("id") Long id, @Param("assetId") Long assetId);
    int updatePurchaseSummary(@Param("id") Long id, @Param("purchasedAt") LocalDate purchasedAt,
                              @Param("purchasedPrice") BigDecimal purchasedPrice,
                              @Param("purchasePriceDiff") BigDecimal purchasePriceDiff);
    int updateCurrentPrice(@Param("id") Long id, @Param("price") BigDecimal price,
                           @Param("capturedAt") LocalDateTime capturedAt);

    long countByBrand(@Param("brandId") Long brandId);
}
