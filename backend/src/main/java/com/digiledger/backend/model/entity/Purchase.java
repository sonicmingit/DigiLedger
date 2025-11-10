package com.digiledger.backend.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Purchase {
    private Long id;
    private Long assetId;
    private String type;
    private BigDecimal price;
    private BigDecimal shippingCost;
    private BigDecimal tax;
    private BigDecimal otherCost;
    private LocalDate purchaseDate;
    private String note;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
