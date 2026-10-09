package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.model.dto.dashboard.DashboardSummaryDTO;
import com.digiledger.backend.model.dto.dashboard.DashboardSpendingDTO;
import com.digiledger.backend.service.DashboardService;
import com.digiledger.backend.service.impl.DashboardSpendingService;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;
    private final DashboardSpendingService spendingService;
    public DashboardController(DashboardService service, DashboardSpendingService spendingService) {
        this.service = service;
        this.spendingService = spendingService;
    }
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryDTO> summary() { return ApiResponse.success(service.getSummary()); }

    @GetMapping("/spending")
    public ApiResponse<DashboardSpendingDTO> spending(
            @RequestParam(name = "dateFrom", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(name = "dateTo", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @RequestParam(name = "categoryId", required = false) Long categoryId,
            @RequestParam(name = "type", required = false) String type,
            @RequestParam(name = "platformId", required = false) Long platformId,
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(name = "page", defaultValue = "1") int page,
            @RequestParam(name = "pageSize", defaultValue = "20") int pageSize) {
        return ApiResponse.success(spendingService.getSpending(dateFrom, dateTo, categoryId, type,
                platformId, keyword, page, pageSize));
    }
}
