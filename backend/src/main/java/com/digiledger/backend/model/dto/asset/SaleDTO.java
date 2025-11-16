package com.digiledger.backend.model.dto.asset;

import com.digiledger.backend.model.enums.SaleScope;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 售出记录数据传输对象。
 */
public record SaleDTO(
        Long id,
        SaleScope saleScope,
        Long purchaseId,
        Long platformId,
        String platformName,
        String buyer,
        BigDecimal salePrice,
        BigDecimal fee,
        BigDecimal shippingCost,
        BigDecimal otherCost,
        BigDecimal netIncome,
        LocalDate saleDate,
        Long useDays,
        BigDecimal lossAmount,
        BigDecimal dailyUsageCost,
        BigDecimal monthlyUsageCost,
        List<String> attachments,
        String notes
) {
}
