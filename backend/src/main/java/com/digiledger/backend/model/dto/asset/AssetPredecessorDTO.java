package com.digiledger.backend.model.dto.asset;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 上代物品摘要及相对当前物品的主商品购买对比。缺少购买信息时指标为 null。 */
public record AssetPredecessorDTO(
        Long id, String name, Long categoryId, String categoryPath, String brandName,
        String model, String status, String coverImageUrl,
        BigDecimal primaryPrice, LocalDate primaryPurchaseDate,
        BigDecimal primaryPriceDelta, Long purchaseGapDays
) {
}
