package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.model.dto.dashboard.DashboardSummaryDTO;
import com.digiledger.backend.service.DashboardService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService service;
    public DashboardController(DashboardService service) { this.service = service; }
    @GetMapping("/summary")
    public ApiResponse<DashboardSummaryDTO> summary() { return ApiResponse.success(service.getSummary()); }
}
