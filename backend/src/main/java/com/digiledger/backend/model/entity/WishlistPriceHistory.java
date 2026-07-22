package com.digiledger.backend.model.entity;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 一次不可变的心愿价格观测，用于计算价格变化和展示历史。 */
@Data
public class WishlistPriceHistory {
    private Long id;
    private Long wishlistId;
    private BigDecimal price;
    private LocalDateTime capturedAt;
    private LocalDateTime createdAt;
}
