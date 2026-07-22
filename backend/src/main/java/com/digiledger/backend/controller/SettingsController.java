package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.model.dto.setting.*;
import com.digiledger.backend.service.PreferencesService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/settings/preferences")
public class SettingsController {
    private final PreferencesService service;
    public SettingsController(PreferencesService service) { this.service = service; }
    @GetMapping public ApiResponse<PreferencesDTO> get() { return ApiResponse.success(service.get()); }
    @PutMapping public ApiResponse<Void> update(@RequestBody @Valid PreferencesRequest request) {
        service.update(request); return ApiResponse.success();
    }
}
