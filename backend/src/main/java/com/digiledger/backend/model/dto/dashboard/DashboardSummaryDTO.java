package com.digiledger.backend.model.dto.dashboard;

import com.digiledger.backend.model.dto.asset.AssetSummaryDTO;
import java.math.BigDecimal;
import java.util.List;

/** PC 总览与 H5 统计共用的稳定聚合模型。 */
public record DashboardSummaryDTO(
        BigDecimal totalAssetValue, long assetCount, long activeCount, long idleCount,
        long pendingSaleCount, BigDecimal avgDailyCost, BigDecimal monthValueChangeRate,
        BigDecimal monthCostChangeRate, List<StatusDistribution> statusDistribution,
        List<CategoryDistribution> categoryDistribution, List<ValueTrend> valueTrend,
        List<AssetSummaryDTO> recentAssets) {
    public record StatusDistribution(String status, long count) { }
    public record CategoryDistribution(Long categoryId, String categoryName, BigDecimal value, long count) { }
    public record ValueTrend(String month, BigDecimal value) { }
}
