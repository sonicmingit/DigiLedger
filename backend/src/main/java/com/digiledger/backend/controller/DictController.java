package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.model.dto.dict.BrandDTO;
import com.digiledger.backend.model.dto.dict.BrandRequest;
import com.digiledger.backend.model.dto.dict.CategoryRequest;
import com.digiledger.backend.model.dto.dict.CategoryTreeNodeDTO;
import com.digiledger.backend.model.dto.dict.PlatformDTO;
import com.digiledger.backend.model.dto.dict.PlatformRequest;
import com.digiledger.backend.model.dto.dict.TagRequest;
import com.digiledger.backend.model.dto.dict.TagTreeNodeDTO;
import com.digiledger.backend.service.DictService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dict")
public class DictController {

    private final DictService dictService;

    public DictController(DictService dictService) {
        this.dictService = dictService;
    }

    @GetMapping("/categories/tree")
    public ApiResponse<List<CategoryTreeNodeDTO>> getCategoryTree() {
        return ApiResponse.success(dictService.getCategoryTree());
    }

    @PostMapping("/categories")
    public ApiResponse<Long> createCategory(@RequestBody @Valid CategoryRequest request) {
        return ApiResponse.success(dictService.createCategory(request));
    }

    @PutMapping("/categories/{id}")
    public ApiResponse<Void> updateCategory(@PathVariable("id") @NotNull @Min(1) Long id,
                                            @RequestBody @Valid CategoryRequest request) {
        dictService.updateCategory(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/categories/{id}")
    public ApiResponse<Void> deleteCategory(@PathVariable("id") @NotNull @Min(1) Long id) {
        dictService.deleteCategory(id);
        return ApiResponse.success();
    }

    @GetMapping("/platforms")
    public ApiResponse<List<PlatformDTO>> listPlatforms() {
        return ApiResponse.success(dictService.listPlatforms());
    }

    @PostMapping("/platforms")
    public ApiResponse<Long> createPlatform(@RequestBody @Valid PlatformRequest request) {
        return ApiResponse.success(dictService.createPlatform(request));
    }

    @PutMapping("/platforms/{id}")
    public ApiResponse<Void> updatePlatform(@PathVariable("id") @NotNull @Min(1) Long id,
                                            @RequestBody @Valid PlatformRequest request) {
        dictService.updatePlatform(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/platforms/{id}")
    public ApiResponse<Void> deletePlatform(@PathVariable("id") @NotNull @Min(1) Long id) {
        dictService.deletePlatform(id);
        return ApiResponse.success();
    }

    @GetMapping("/brands")
    public ApiResponse<List<BrandDTO>> listBrands() {
        return ApiResponse.success(dictService.listBrands());
    }

    @PostMapping("/brands")
    public ApiResponse<Long> createBrand(@RequestBody @Valid BrandRequest request) {
        return ApiResponse.success(dictService.createBrand(request));
    }

    @PutMapping("/brands/{id}")
    public ApiResponse<Void> updateBrand(@PathVariable("id") @NotNull @Min(1) Long id,
                                         @RequestBody @Valid BrandRequest request) {
        dictService.updateBrand(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/brands/{id}")
    public ApiResponse<Void> deleteBrand(@PathVariable("id") @NotNull @Min(1) Long id) {
        dictService.deleteBrand(id);
        return ApiResponse.success();
    }

    @GetMapping("/tags/tree")
    public ApiResponse<List<TagTreeNodeDTO>> getTagTree() {
        return ApiResponse.success(dictService.getTagTree());
    }

    @PostMapping("/tags")
    public ApiResponse<Long> createTag(@RequestBody @Valid TagRequest request) {
        return ApiResponse.success(dictService.createTag(request));
    }

    @PutMapping("/tags/{id}")
    public ApiResponse<Void> updateTag(@PathVariable("id") @NotNull @Min(1) Long id,
                                       @RequestBody @Valid TagRequest request) {
        dictService.updateTag(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/tags/{id}")
    public ApiResponse<Void> deleteTag(@PathVariable("id") @NotNull @Min(1) Long id) {
        dictService.deleteTag(id);
        return ApiResponse.success();
    }


}
