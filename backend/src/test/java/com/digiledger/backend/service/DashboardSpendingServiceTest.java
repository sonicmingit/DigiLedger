package com.digiledger.backend.service;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.mapper.DashboardSpendingMapper;
import com.digiledger.backend.mapper.DictCategoryMapper;
import com.digiledger.backend.model.dto.dashboard.SpendingPurchaseRow;
import com.digiledger.backend.model.entity.DictCategory;
import com.digiledger.backend.service.impl.DashboardSpendingService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DashboardSpendingServiceTest {
    private final DashboardSpendingMapper purchases = mock(DashboardSpendingMapper.class);
    private final DictCategoryMapper categories = mock(DictCategoryMapper.class);
    private final DashboardSpendingService service = new DashboardSpendingService(purchases, categories);

    @Test void aggregatesPurchasesOnceAcrossTypesMonthsAndParentCategories() {
        when(categories.findAll()).thenReturn(List.of(category(1, null, "数码"), category(2, 1L, "手机"),
                category(3, 2L, "安卓"), category(4, null, "摄影")));
        when(purchases.findPurchases(any(), any(), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(
                row(1, 10, 3, "PRIMARY", "100.00", "10.00", "2026-02-28"),
                row(2, 10, 3, "ACCESSORY", "20.00", "0.00", "2026-02-01"),
                row(3, 11, 4, "SERVICE", "5.00", "1.00", "2026-03-01")));

        var result = service.getSpending(null, null, null, null, null, null, 1, 2);
        assertEquals(new BigDecimal("136.00"), result.totalSpend());
        assertEquals(new BigDecimal("110.00"), result.primarySpend());
        assertEquals(new BigDecimal("20.00"), result.accessorySpend());
        assertEquals(new BigDecimal("6.00"), result.serviceSpend());
        assertEquals(3, result.purchaseCount());
        assertEquals(2, result.assetCount());
        assertEquals(new BigDecimal("130.00"), result.categoryBreakdown().get(0).amount());
        assertEquals(1L, result.categoryBreakdown().get(0).categoryId());
        assertEquals("2026-02", result.monthlyTrend().get(0).month());
        assertEquals(new BigDecimal("130.00"), result.monthlyTrend().get(0).amount());
        assertEquals(3, result.records().total());
        assertEquals(2, result.records().items().size());

        var secondPage = service.getSpending(null, null, null, null, null, null, 2, 2);
        assertEquals(1, secondPage.records().items().size());
        assertEquals(3L, secondPage.records().items().get(0).id());
    }

    @Test void passesInclusiveRangeAndFiltersAndReturnsZeroShape() {
        when(categories.findAll()).thenReturn(List.of(category(1, null, "数码")));
        var from = LocalDate.of(2026, 2, 1);
        var to = LocalDate.of(2026, 2, 28);
        when(purchases.findPurchases(from, to, 1L, "ACCESSORY", 9L, "耳机")).thenReturn(List.of());
        var result = service.getSpending(from, to, 1L, "ACCESSORY", 9L, " 耳机 ", 1, 20);
        verify(purchases).findPurchases(from, to, 1L, "ACCESSORY", 9L, "耳机");
        assertEquals(BigDecimal.ZERO, result.totalSpend());
        assertTrue(result.monthlyTrend().isEmpty());
        assertTrue(result.categoryBreakdown().isEmpty());
        assertTrue(result.records().items().isEmpty());
        assertThrows(BizException.class, () -> service.getSpending(to, from, null, null, null, null, 1, 20));
    }

    @Test void selectedParentGroupsDirectPurchasesSeparatelyFromChildSubtrees() {
        when(categories.findAll()).thenReturn(List.of(category(1, null, "数码"), category(2, 1L, "手机"),
                category(3, 2L, "安卓")));
        when(purchases.findPurchases(isNull(), isNull(), eq(1L), isNull(), isNull(), isNull()))
                .thenReturn(List.of(row(1, 10, 1, "PRIMARY", "5.00", "0.00", "2026-02-01"),
                        row(2, 11, 3, "PRIMARY", "8.00", "0.00", "2026-02-02")));
        var result = service.getSpending(null, null, 1L, null, null, null, 1, 20);
        assertEquals(new BigDecimal("13.00"), result.totalSpend());
        assertEquals(2, result.categoryBreakdown().size());
        assertEquals(2L, result.categoryBreakdown().get(0).categoryId());
        assertEquals(new BigDecimal("8.00"), result.categoryBreakdown().get(0).amount());
        assertEquals("本分类直属", result.categoryBreakdown().get(1).categoryName());
    }

    private DictCategory category(long id, Long parent, String name) {
        DictCategory category = new DictCategory(); category.setId(id); category.setParentId(parent); category.setName(name); return category;
    }
    private SpendingPurchaseRow row(long id, long asset, long category, String type, String price, String shipping, String date) {
        SpendingPurchaseRow row = new SpendingPurchaseRow();
        row.setId(id); row.setAssetId(asset); row.setAssetName("物品" + asset); row.setCategoryId(category);
        row.setType(type); row.setPrice(new BigDecimal(price)); row.setShippingCost(new BigDecimal(shipping));
        row.setPurchaseDate(LocalDate.parse(date)); return row;
    }
}
