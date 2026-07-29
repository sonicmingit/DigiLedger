package com.digiledger.backend.service.impl;

import com.digiledger.backend.mapper.DashboardSnapshotMapper;
import com.digiledger.backend.mapper.DictCategoryMapper;
import com.digiledger.backend.model.dto.asset.AssetSummaryDTO;
import com.digiledger.backend.model.dto.dashboard.DashboardSummaryDTO;
import com.digiledger.backend.model.entity.DashboardSnapshot;
import com.digiledger.backend.model.entity.DictCategory;
import com.digiledger.backend.service.AssetService;
import com.digiledger.backend.service.DashboardService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.*;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
/**
 * 从现有资产摘要计算总览，并把当月真实聚合值幂等写入快照表。
 * 这样旧库首次升级时可立即返回单点趋势，后续月份又无需额外定时任务即可积累历史。
 */
public class DashboardServiceImpl implements DashboardService {
    private final AssetService assetService;
    private final DictCategoryMapper categoryMapper;
    private final DashboardSnapshotMapper snapshotMapper;
    public DashboardServiceImpl(AssetService assetService, DictCategoryMapper categoryMapper,
                                DashboardSnapshotMapper snapshotMapper) {
        this.assetService = assetService; this.categoryMapper = categoryMapper; this.snapshotMapper = snapshotMapper;
    }

    @Override
    @Transactional
    public DashboardSummaryDTO getSummary() {
        List<AssetSummaryDTO> assets = assetService.listAssets(null, null, null, null, null, null);
        BigDecimal total = assets.stream().map(AssetSummaryDTO::totalInvest).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        BigDecimal avgDaily = assets.stream().map(AssetSummaryDTO::avgCostPerDay).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);

        DashboardSnapshot current = new DashboardSnapshot();
        String currentMonth = YearMonth.now().toString();
        current.setSnapshotMonth(currentMonth); current.setTotalValue(total); current.setAvgDailyCost(avgDaily);
        // 同月重复读取只更新同一行，避免请求次数污染趋势。
        snapshotMapper.upsert(current);
        List<DashboardSnapshot> snapshots = snapshotMapper.findRecent();
        DashboardSnapshot previous = snapshots.stream().filter(s -> !currentMonth.equals(s.getSnapshotMonth())).findFirst().orElse(null);

        Map<String, Long> statuses = assets.stream().collect(Collectors.groupingBy(AssetSummaryDTO::status, Collectors.counting()));
        Map<Long, String> categoryNames = categoryMapper.findAll().stream()
                .collect(Collectors.toMap(DictCategory::getId, DictCategory::getName));
        Map<Long, List<AssetSummaryDTO>> byCategory = assets.stream().collect(Collectors.groupingBy(
                a -> a.categoryId() == null ? -1L : a.categoryId()));
        List<DashboardSummaryDTO.CategoryDistribution> categories = byCategory.entrySet().stream().map(e ->
                new DashboardSummaryDTO.CategoryDistribution(e.getKey() == -1L ? null : e.getKey(),
                        e.getKey() == -1L ? "未分类" : categoryNames.getOrDefault(e.getKey(), "未知分类"),
                        e.getValue().stream().map(AssetSummaryDTO::totalInvest).filter(Objects::nonNull)
                                .reduce(BigDecimal.ZERO, BigDecimal::add), e.getValue().size()))
                .sorted(Comparator.comparing(DashboardSummaryDTO.CategoryDistribution::value).reversed()).toList();
        List<DashboardSummaryDTO.ValueTrend> trend = snapshots.stream()
                .sorted(Comparator.comparing(DashboardSnapshot::getSnapshotMonth))
                .map(s -> new DashboardSummaryDTO.ValueTrend(s.getSnapshotMonth(), s.getTotalValue())).toList();
        return new DashboardSummaryDTO(total, assets.size(), statuses.getOrDefault("使用中", 0L),
                statuses.getOrDefault("已闲置", 0L), statuses.getOrDefault("待出售", 0L), avgDaily,
                rate(total, previous == null ? null : previous.getTotalValue()),
                rate(avgDaily, previous == null ? null : previous.getAvgDailyCost()),
                statuses.entrySet().stream().map(e -> new DashboardSummaryDTO.StatusDistribution(e.getKey(), e.getValue())).toList(),
                categories, trend, assets.stream().limit(10).toList());
    }

    private BigDecimal rate(BigDecimal current, BigDecimal previous) {
        if (previous == null || previous.signum() == 0) return null;
        return current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 2, RoundingMode.HALF_UP);
    }
}
