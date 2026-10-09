package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.DashboardSpendingMapper;
import com.digiledger.backend.mapper.DictCategoryMapper;
import com.digiledger.backend.model.dto.dashboard.DashboardSpendingDTO;
import com.digiledger.backend.model.dto.dashboard.SpendingPurchaseRow;
import com.digiledger.backend.model.entity.DictCategory;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardSpendingService {
    private final DashboardSpendingMapper spendingMapper;
    private final DictCategoryMapper categoryMapper;

    public DashboardSpendingService(DashboardSpendingMapper spendingMapper, DictCategoryMapper categoryMapper) {
        this.spendingMapper = spendingMapper;
        this.categoryMapper = categoryMapper;
    }

    public DashboardSpendingDTO getSpending(LocalDate dateFrom, LocalDate dateTo, Long categoryId,
                                             String type, Long platformId, String keyword, int page, int pageSize) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "开始日期不能晚于结束日期");
        }
        if (page < 1 || pageSize < 1 || pageSize > 100) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "分页参数无效");
        }
        if (type != null && !List.of("PRIMARY", "ACCESSORY", "SERVICE").contains(type)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "购买类型无效");
        }

        Map<Long, DictCategory> categories = categoryMapper.findAll().stream()
                .collect(Collectors.toMap(DictCategory::getId, category -> category));
        if (categoryId != null && !categories.containsKey(categoryId)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "分类不存在");
        }
        List<SpendingPurchaseRow> rows = spendingMapper.findPurchases(dateFrom, dateTo, categoryId,
                type, platformId, keyword == null ? null : keyword.trim());
        BigDecimal primary = BigDecimal.ZERO, accessory = BigDecimal.ZERO, service = BigDecimal.ZERO;
        Set<Long> assetIds = new HashSet<>();
        Map<YearMonth, BigDecimal> months = new TreeMap<>();
        Map<Long, CategoryAccumulator> grouped = new HashMap<>();
        for (SpendingPurchaseRow row : rows) {
            BigDecimal amount = amount(row);
            assetIds.add(row.getAssetId());
            switch (row.getType()) {
                case "PRIMARY" -> primary = primary.add(amount);
                case "ACCESSORY" -> accessory = accessory.add(amount);
                case "SERVICE" -> service = service.add(amount);
                default -> throw new IllegalStateException("未知购买类型: " + row.getType());
            }
            months.merge(YearMonth.from(row.getPurchaseDate()), amount, BigDecimal::add);
            Long groupId = groupCategory(row.getCategoryId(), categoryId, categories);
            grouped.computeIfAbsent(groupId, ignored -> new CategoryAccumulator()).add(amount);
        }
        BigDecimal total = primary.add(accessory).add(service);
        List<DashboardSpendingDTO.MonthlySpend> monthlyTrend = months.entrySet().stream()
                .map(e -> new DashboardSpendingDTO.MonthlySpend(e.getKey().toString(), e.getValue())).toList();
        List<DashboardSpendingDTO.CategorySpend> categoryBreakdown = grouped.entrySet().stream()
                .map(e -> new DashboardSpendingDTO.CategorySpend(e.getKey(), categoryName(e.getKey(), categoryId, categories),
                        e.getValue().amount, e.getValue().count))
                .sorted(Comparator.comparing(DashboardSpendingDTO.CategorySpend::amount).reversed()
                        .thenComparing(DashboardSpendingDTO.CategorySpend::categoryName)).toList();

        long offset = (long) (page - 1) * pageSize;
        int from = (int) Math.min(offset, rows.size());
        int to = (int) Math.min((long) from + pageSize, rows.size());
        List<DashboardSpendingDTO.PurchaseItem> items = rows.subList(from, to).stream()
                .map(row -> new DashboardSpendingDTO.PurchaseItem(row.getId(), row.getAssetId(), row.getAssetName(),
                        row.getCategoryId(), row.getType(), row.getName(), row.getPlatformName(),
                        row.getPurchaseDate(), row.getPrice(), row.getShippingCost(), amount(row))).toList();
        return new DashboardSpendingDTO(total, primary, accessory, service, rows.size(), assetIds.size(),
                monthlyTrend, categoryBreakdown, new DashboardSpendingDTO.Page(rows.size(), page, pageSize, items));
    }

    private BigDecimal amount(SpendingPurchaseRow row) {
        return row.getPrice().add(row.getShippingCost() == null ? BigDecimal.ZERO : row.getShippingCost());
    }

    private Long groupCategory(Long actualId, Long selectedId, Map<Long, DictCategory> categories) {
        if (actualId == null) return null;
        Long current = actualId;
        Set<Long> visited = new HashSet<>();
        while (current != null && visited.add(current)) {
            DictCategory category = categories.get(current);
            if (category == null) return actualId;
            Long parent = category.getParentId();
            if (selectedId == null && parent == null) return current;
            if (selectedId != null && (current.equals(selectedId) || selectedId.equals(parent))) return current;
            current = parent;
        }
        return actualId;
    }

    private String categoryName(Long id, Long selectedId, Map<Long, DictCategory> categories) {
        if (id == null) return "未分类";
        DictCategory category = categories.get(id);
        if (category == null) return "未知分类";
        return id.equals(selectedId) ? "本分类直属" : category.getName();
    }

    private static class CategoryAccumulator {
        BigDecimal amount = BigDecimal.ZERO;
        long count;
        void add(BigDecimal value) { amount = amount.add(value); count++; }
    }
}
