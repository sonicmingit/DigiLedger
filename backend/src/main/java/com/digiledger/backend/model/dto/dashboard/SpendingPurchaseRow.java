package com.digiledger.backend.model.dto.dashboard;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class SpendingPurchaseRow {
    private Long id;
    private Long assetId;
    private String assetName;
    private Long categoryId;
    private String type;
    private String name;
    private String platformName;
    private BigDecimal price;
    private BigDecimal shippingCost;
    private LocalDate purchaseDate;
}
