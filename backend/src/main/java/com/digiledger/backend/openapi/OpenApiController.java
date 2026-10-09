package com.digiledger.backend.openapi;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.model.dto.asset.*;
import com.digiledger.backend.model.dto.dict.*;
import com.digiledger.backend.service.AssetService;
import com.digiledger.backend.service.DictService;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/open/v1")
public class OpenApiController {
    private final AssetService assets;
    private final DictService dictionaries;
    private final OpenAssetService edits;
    public OpenApiController(AssetService assets, DictService dictionaries, OpenAssetService edits) {
        this.assets = assets; this.dictionaries = dictionaries; this.edits = edits;
    }

    @GetMapping("/assets") public ApiResponse<AssetPageDTO> list(
            @RequestParam(name="keyword", required=false) String keyword,
            @RequestParam(name="status", required=false) String status,
            @RequestParam(name="category_id", required=false) Long categoryId,
            @RequestParam(name="brand_id", required=false) Long brandId,
            @RequestParam(name="platform_id", required=false) Long platformId,
            @RequestParam(name="tag_ids", required=false) List<Long> tagIds,
            @RequestParam(name="page", defaultValue="1") @Min(1) @Max(1000000) int page,
            @RequestParam(name="page_size", defaultValue="20") @Min(1) @Max(100) int pageSize) {
        return ApiResponse.success(assets.pageAssets(status, keyword, categoryId, brandId, platformId, tagIds, page, pageSize, "purchaseDate", "desc"));
    }
    @GetMapping("/assets/{id}") public ApiResponse<AssetDetailDTO> detail(@PathVariable("id") @Min(1) Long id) {
        return ApiResponse.success(assets.getAssetDetail(id));
    }
    @PostMapping("/assets") public ApiResponse<Long> create(@RequestBody @Valid AssetCreateRequest request) {
        return ApiResponse.success(assets.createAsset(request));
    }
    @PatchMapping("/assets/{id}") public ApiResponse<Void> patch(@PathVariable("id") @Min(1) Long id, @RequestBody ObjectNode patch) {
        edits.patch(id, patch); return ApiResponse.success();
    }
    @DeleteMapping("/assets/{id}") public ApiResponse<Void> delete(@PathVariable("id") @Min(1) Long id) {
        assets.deleteAsset(id); return ApiResponse.success();
    }
    @GetMapping("/dict/categories/tree") public ApiResponse<List<CategoryTreeNodeDTO>> categories() { return ApiResponse.success(dictionaries.getCategoryTree()); }
    @GetMapping("/dict/brands") public ApiResponse<List<BrandDTO>> brands() { return ApiResponse.success(dictionaries.listBrands()); }
    @GetMapping("/dict/platforms") public ApiResponse<List<PlatformDTO>> platforms() { return ApiResponse.success(dictionaries.listPlatforms()); }
    @GetMapping("/dict/tags/tree") public ApiResponse<List<TagTreeNodeDTO>> tags() { return ApiResponse.success(dictionaries.getTagTree()); }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ApiResponse<Void> invalidInput(Exception exception) {
        return ApiResponse.failure(ErrorCode.VALIDATION_ERROR, "JSON 请求或参数类型无效");
    }
}
