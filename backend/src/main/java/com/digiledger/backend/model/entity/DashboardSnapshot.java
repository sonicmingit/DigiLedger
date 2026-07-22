package com.digiledger.backend.model.entity;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class DashboardSnapshot {
    /** 数据库使用 YYYY-MM 字符串，避免数据库驱动对 YearMonth 的实现差异。 */
    private String snapshotMonth;
    private BigDecimal totalValue;
    private BigDecimal avgDailyCost;
}
