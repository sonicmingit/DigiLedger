package com.digiledger.backend.model.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record AssetSummaryDTO(
        Long id,
        String name,
        String category,
        String status,
        BigDecimal totalInvest,
        BigDecimal avgCostPerDay,
        BigDecimal accumulatedDepreciation,
        BigDecimal bookValue,
        LocalDate enabledDate
) {}
