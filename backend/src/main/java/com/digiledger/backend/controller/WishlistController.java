package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.model.dto.wishlist.WishlistDTO;
import com.digiledger.backend.model.dto.wishlist.WishlistRequest;
import com.digiledger.backend.service.WishlistService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 心愿单接口，支持 CRUD 与转资产。
 */
@RestController
@RequestMapping("/api/wishlist")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ApiResponse<List<WishlistDTO>> list() {
        return ApiResponse.success(wishlistService.listAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<WishlistDTO> detail(@PathVariable @NotNull @Min(1) Long id) {
        return ApiResponse.success(wishlistService.getById(id));
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid WishlistRequest request) {
        return ApiResponse.success(wishlistService.create(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable @NotNull @Min(1) Long id,
                                    @RequestBody @Valid WishlistRequest request) {
        wishlistService.update(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable @NotNull @Min(1) Long id) {
        wishlistService.delete(id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/convert")
    public ApiResponse<Long> convert(@PathVariable @NotNull @Min(1) Long id) {
        return ApiResponse.success(wishlistService.convertToAsset(id));
    }
}
