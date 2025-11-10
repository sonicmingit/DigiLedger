package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.model.dto.asset.AssetCreateRequest;
import com.digiledger.backend.model.dto.asset.AssetDetailDTO;
import com.digiledger.backend.model.dto.asset.AssetSellRequest;
import com.digiledger.backend.model.dto.asset.AssetSummaryDTO;
import com.digiledger.backend.model.dto.asset.SaleDTO;
import com.digiledger.backend.service.AssetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 资产接口，涵盖 CRUD、详情与出售向导。
 */
@RestController
@RequestMapping("/api/assets")
public class AssetController {

    private final AssetService assetService;

    public AssetController(AssetService assetService) {
        this.assetService = assetService;
    }

    /**
     * 资产列表，支持状态与关键词过滤。
     */
    @GetMapping
    public ApiResponse<List<AssetSummaryDTO>> listAssets(@RequestParam(required = false) String status,
                                                         @RequestParam(required = false) String keyword) {
        return ApiResponse.success(assetService.listAssets(status, keyword));
    }

    /**
     * 获取资产详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<AssetDetailDTO> getAsset(@PathVariable @NotNull @Min(1) Long id) {
        return ApiResponse.success(assetService.getAssetDetail(id));
    }

    /**
     * 创建资产，并可同时录入购买记录。
     */
    @PostMapping
    public ApiResponse<Long> createAsset(@RequestBody @Valid AssetCreateRequest request) {
        return ApiResponse.success(assetService.createAsset(request));
    }

    /**
     * 更新资产信息。
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> updateAsset(@PathVariable @NotNull @Min(1) Long id,
                                         @RequestBody @Valid AssetCreateRequest request) {
        assetService.updateAsset(id, request);
        return ApiResponse.success();
    }

    /**
     * 删除资产，存在关联记录时将返回业务冲突错误。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAsset(@PathVariable @NotNull @Min(1) Long id) {
        assetService.deleteAsset(id);
        return ApiResponse.success();
    }

    /**
     * 出售向导，创建售出记录并更新资产状态。
     */
    @PostMapping("/{id}/sell")
    public ApiResponse<SaleDTO> sellAsset(@PathVariable @NotNull @Min(1) Long id,
                                          @RequestBody @Valid AssetSellRequest request) {
        return ApiResponse.success(assetService.sellAsset(id, request));
    }
}
