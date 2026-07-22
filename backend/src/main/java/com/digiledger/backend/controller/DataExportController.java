package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.model.dto.export.DataExportDTO;
import com.digiledger.backend.service.DataExportService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/data")
public class DataExportController {
    private final DataExportService service;
    public DataExportController(DataExportService service) { this.service = service; }
    @GetMapping("/export")
    public ApiResponse<DataExportDTO> export(@RequestParam(defaultValue="json") String format) {
        return ApiResponse.success(service.export(format));
    }
}
