package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.model.dto.AssetSummaryDTO;
import com.digiledger.backend.model.entity.DeviceAsset;
import com.digiledger.backend.service.AssetService;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    @GetMapping
    public ApiResponse<List<AssetSummaryDTO>> listAssets() {
        return ApiResponse.success(assetService.listAssets());
    }

    @GetMapping("/{id}")
    public ApiResponse<DeviceAsset> getAsset(@PathVariable @NotNull Long id) {
        DeviceAsset asset = assetService.getAsset(id);
        if (asset == null) {
            return ApiResponse.failure(404, "Asset not found");
        }
        return ApiResponse.success(asset);
    }
}
