package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.model.dto.asset.AssetCreateRequest;
import com.digiledger.backend.model.dto.asset.AssetDetailDTO;
import com.digiledger.backend.model.dto.asset.AssetPredecessorDTO;
import com.digiledger.backend.model.dto.asset.AssetSellRequest;
import com.digiledger.backend.model.dto.asset.AssetStatusUpdateRequest;
import com.digiledger.backend.model.dto.asset.AssetSummaryDTO;
import com.digiledger.backend.model.dto.asset.SaleDTO;
import com.digiledger.backend.service.AssetService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import com.fasterxml.jackson.databind.JsonNode;
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
    public ApiResponse<List<AssetSummaryDTO>> listAssets(@RequestParam(name = "status", required = false) String status,
                                                         @RequestParam(name = "keyword", required = false) String keyword,
                                                         @RequestParam(name = "q", required = false) String q,
                                                         @RequestParam(name = "category_id", required = false) Long categoryId,
                                                         @RequestParam(name = "brand_id", required = false) Long brandId,
                                                         @RequestParam(name = "platform_id", required = false) Long platformId,
                                                         @RequestParam(name = "tag_ids", required = false) List<Long> tagIds,
                                                         @RequestParam(name = "view", required = false) String view) {
        String search = keyword != null ? keyword : q;
        return ApiResponse.success(assetService.listAssets(status, search, categoryId, brandId, platformId, tagIds));
    }

    @GetMapping("/page")
    public ApiResponse<com.digiledger.backend.model.dto.asset.AssetPageDTO> pageAssets(
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "keyword", required = false) String keyword,
            @RequestParam(name = "q", required = false) String q,
            @RequestParam(name = "category_id", required = false) Long categoryId,
            @RequestParam(name = "brand_id", required = false) Long brandId,
            @RequestParam(name = "platform_id", required = false) Long platformId,
            @RequestParam(name = "tag_ids", required = false) List<Long> tagIds,
            @RequestParam(name = "page", defaultValue = "1") @Min(1) int page,
            @RequestParam(name = "page_size", defaultValue = "20") @Min(1) @Max(100) int pageSize,
            @RequestParam(name = "sort_by", defaultValue = "purchaseDate") String sortBy,
            @RequestParam(name = "sort_order", defaultValue = "desc") String sortOrder) {
        String search = keyword != null ? keyword : q;
        return ApiResponse.success(assetService.pageAssets(status, search, categoryId, brandId, platformId, tagIds, page, pageSize, sortBy, sortOrder));
    }

    /**
     * 获取资产详情。
     */
    @GetMapping("/{id}")
    public ApiResponse<AssetDetailDTO> getAsset(@PathVariable(name = "id") @NotNull @Min(1) Long id) {
        return ApiResponse.success(assetService.getAssetDetail(id));
    }

    /** 仅搜索同一类别的已有物品，排除当前物品，最多返回 20 条。 */
    @GetMapping("/predecessor-options")
    public ApiResponse<List<AssetPredecessorDTO>> predecessorOptions(
            @RequestParam(name = "category_id") @Min(1) Long categoryId,
            @RequestParam(name = "q", required = false) String keyword,
            @RequestParam(name = "exclude_asset_id", required = false) Long excludeId) {
        return ApiResponse.success(assetService.predecessorOptions(categoryId, keyword, excludeId));
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
    public ApiResponse<Void> updateAsset(@PathVariable(name = "id") @NotNull @Min(1) Long id,
                                         @RequestBody @Valid AssetCreateRequest request) {
        assetService.updateAsset(id, request);
        return ApiResponse.success();
    }

    /**
     * 删除资产，存在关联记录时将返回业务冲突错误。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteAsset(@PathVariable(name = "id") @NotNull @Min(1) Long id) {
        assetService.deleteAsset(id);
        return ApiResponse.success();
    }

    /**
     * 出售向导，创建售出记录并更新资产状态。
     */
    @PostMapping("/{id}/sell")
    public ApiResponse<SaleDTO> sellAsset(@PathVariable(name = "id") @NotNull @Min(1) Long id,
                                          @RequestBody @Valid AssetSellRequest request) {
        return ApiResponse.success(assetService.sellAsset(id, request));
    }

    @PatchMapping("/{id}/status")
    public ApiResponse<Void> updateStatus(@PathVariable(name = "id") @NotNull @Min(1) Long id,
                                          @RequestBody @Valid AssetStatusUpdateRequest request) {
        assetService.updateAssetStatus(id, request.getStatus());
        return ApiResponse.success();
    }

    /** 仅更新上代产品关联，字段必传；null 表示解除关联。 */
    @PatchMapping("/{id}/predecessor")
    public ApiResponse<Void> updatePredecessor(
            @PathVariable(name = "id") @NotNull @Min(1) Long id,
            @RequestBody JsonNode request) {
        if (request == null || !request.has("predecessorAssetId")) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "请提供 predecessorAssetId；null 可解除关联");
        }
        JsonNode value = request.get("predecessorAssetId");
        Long predecessorId = null;
        if (!value.isNull()) {
            if (!value.isIntegralNumber() || !value.canConvertToLong() || value.longValue() <= 0) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "上代产品ID必须为正数或 null");
            }
            predecessorId = value.longValue();
        }
        assetService.updateAssetPredecessor(id, predecessorId);
        return ApiResponse.success();
    }

    @PutMapping("/{assetId}/sales/{saleId}")
    public ApiResponse<SaleDTO> updateSale(@PathVariable("assetId") @NotNull @Min(1) Long assetId,
                                           @PathVariable("saleId") @NotNull @Min(1) Long saleId,
                                           @RequestBody @Valid AssetSellRequest request) {
        return ApiResponse.success(assetService.updateSale(assetId, saleId, request));
    }

    @DeleteMapping("/{assetId}/sales/{saleId}")
    public ApiResponse<Void> deleteSale(@PathVariable("assetId") @NotNull @Min(1) Long assetId,
                                        @PathVariable("saleId") @NotNull @Min(1) Long saleId) {
        assetService.deleteSale(assetId, saleId);
        return ApiResponse.success();
    }
}
