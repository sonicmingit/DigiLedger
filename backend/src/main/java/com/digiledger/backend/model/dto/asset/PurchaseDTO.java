package com.digiledger.backend.model.dto.asset;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 购买记录数据传输对象。
 */
public record PurchaseDTO(
        Long id,
        String type,
        String name,
        Long platformId,
        String platformName,
        String seller,
        BigDecimal price,
        BigDecimal shippingCost,
        Integer quantity,
        LocalDate purchaseDate,
        Integer warrantyMonths,
        LocalDate warrantyExpireDate,
        List<String> attachments,
        String notes
) {
}
