package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.WishlistPriceHistory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.util.List;

@Mapper
public interface WishlistPriceHistoryMapper {
    List<WishlistPriceHistory> findByWishlistId(@Param("wishlistId") Long wishlistId);
    int insert(WishlistPriceHistory history);
}
