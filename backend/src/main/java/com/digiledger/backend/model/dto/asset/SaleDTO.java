package com.digiledger.backend.model.dto.asset;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 售出记录数据传输对象。
 */
public record SaleDTO(
        Long id,
        Long platformId,
        String platformName,
        String buyer,
        BigDecimal salePrice,
        BigDecimal fee,
        BigDecimal shippingCost,
        BigDecimal otherCost,
        BigDecimal netIncome,
        LocalDate saleDate,
        List<String> attachments,
        String notes
) {
}
