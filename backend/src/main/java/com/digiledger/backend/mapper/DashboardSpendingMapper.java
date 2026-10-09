package com.digiledger.backend.mapper;

import com.digiledger.backend.model.dto.dashboard.SpendingPurchaseRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import java.time.LocalDate;
import java.util.List;

@Mapper
public interface DashboardSpendingMapper {
    List<SpendingPurchaseRow> findPurchases(@Param("dateFrom") LocalDate dateFrom,
                                           @Param("dateTo") LocalDate dateTo,
                                           @Param("categoryId") Long categoryId,
                                           @Param("type") String type,
                                           @Param("platformId") Long platformId,
                                           @Param("keyword") String keyword);
}
