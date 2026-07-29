package com.digiledger.backend.service;

import com.digiledger.backend.mapper.*;
import com.digiledger.backend.model.dto.asset.AssetSummaryDTO;
import com.digiledger.backend.model.entity.*;
import com.digiledger.backend.service.impl.DashboardServiceImpl;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class DashboardServiceImplTest {
    @Test void aggregatesRealAssetMetricsAndPreviousSnapshot() {
        AssetService assets = mock(AssetService.class);
        DictCategoryMapper categories = mock(DictCategoryMapper.class);
        DashboardSnapshotMapper snapshots = mock(DashboardSnapshotMapper.class);
        when(assets.listAssets(isNull(), isNull(), isNull(), isNull(), isNull(), isNull())).thenReturn(List.of(
                asset(1L, "使用中", new BigDecimal("1000"), new BigDecimal("10")),
                asset(1L, "待出售", new BigDecimal("500"), new BigDecimal("5"))));
        DictCategory category = new DictCategory(); category.setId(1L); category.setName("数码");
        when(categories.findAll()).thenReturn(List.of(category));
        DashboardSnapshot current = snapshot(YearMonth.now().toString(), "1500", "15");
        DashboardSnapshot previous = snapshot(YearMonth.now().minusMonths(1).toString(), "1000", "20");
        when(snapshots.findRecent()).thenReturn(List.of(current, previous));

        var result = new DashboardServiceImpl(assets, categories, snapshots).getSummary();
        assertEquals(new BigDecimal("1500.00"), result.totalAssetValue());
        assertEquals(new BigDecimal("50.00"), result.monthValueChangeRate());
        assertEquals(1, result.pendingSaleCount());
        assertEquals("数码", result.categoryDistribution().get(0).categoryName());
        verify(snapshots).upsert(any(DashboardSnapshot.class));
    }
    private AssetSummaryDTO asset(Long categoryId, String status, BigDecimal value, BigDecimal daily) {
        return new AssetSummaryDTO(1L, "设备", categoryId, "/1", status, null, value, daily, 100, null, null, List.of());
    }
    private DashboardSnapshot snapshot(String month, String value, String daily) {
        DashboardSnapshot s = new DashboardSnapshot(); s.setSnapshotMonth(month);
        s.setTotalValue(new BigDecimal(value)); s.setAvgDailyCost(new BigDecimal(daily)); return s;
    }
}
