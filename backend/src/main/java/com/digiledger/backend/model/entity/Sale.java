package com.digiledger.backend.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Sale {
    private Long id;
    private Long assetId;
    private BigDecimal salePrice;
    private BigDecimal fee;
    private BigDecimal shippingCost;
    private BigDecimal otherCost;
    private BigDecimal netIncome;
    private BigDecimal realizedPnl;
    private String platform;
    private LocalDate saleDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
