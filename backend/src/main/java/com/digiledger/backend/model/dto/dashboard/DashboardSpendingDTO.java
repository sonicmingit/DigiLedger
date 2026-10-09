package com.digiledger.backend.model.dto.dashboard;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DashboardSpendingDTO(
        BigDecimal totalSpend, BigDecimal primarySpend, BigDecimal accessorySpend, BigDecimal serviceSpend,
        long purchaseCount, long assetCount, List<MonthlySpend> monthlyTrend,
        List<CategorySpend> categoryBreakdown, Page records) {
    public record MonthlySpend(String month, BigDecimal amount) { }
    public record CategorySpend(Long categoryId, String categoryName, BigDecimal amount, long purchaseCount) { }
    public record Page(long total, int page, int pageSize, List<PurchaseItem> items) { }
    public record PurchaseItem(Long id, Long assetId, String assetName, Long categoryId, String type,
                               String name, String platformName, LocalDate purchaseDate,
                               BigDecimal price, BigDecimal shippingCost, BigDecimal amount) { }
}
