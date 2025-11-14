package com.digiledger.backend.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Sale {
    /** Sale ID */
    private Long id;
    /** Asset ID */
    private Long assetId;
    /** Related purchase record ID (for accessories) */
    private Long purchaseId;
    /** Platform ID */
    private Long platformId;
    /** Platform name */
    private String platformName;
    /** Buyer */
    private String buyer;
    /** Sale price */
    private BigDecimal salePrice;
    /** Platform fee */
    private BigDecimal fee;
    /** Shipping cost */
    private BigDecimal shippingCost;
    /** Other costs */
    private BigDecimal otherCost;
    /** Net income */
    private BigDecimal netIncome;
    /** Sale date */
    private LocalDate saleDate;
    /** Attachments payload */
    private String attachments;
    /** Notes */
    private String notes;
    /** Created timestamp */
    private LocalDateTime createdAt;
    /** Updated timestamp */
    private LocalDateTime updatedAt;
}
