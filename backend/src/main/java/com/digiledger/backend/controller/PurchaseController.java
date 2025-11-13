package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.model.dto.asset.PurchaseUpsertRequest;
import com.digiledger.backend.service.PurchaseService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/purchases")
public class PurchaseController {

    private final PurchaseService purchaseService;

    public PurchaseController(PurchaseService purchaseService) {
        this.purchaseService = purchaseService;
    }

    @PostMapping
    public ApiResponse<Long> create(@RequestBody @Valid PurchaseUpsertRequest request) {
        return ApiResponse.success(purchaseService.createPurchase(request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable("id") @NotNull @Min(1) Long id,
                                    @RequestBody @Valid PurchaseUpsertRequest request) {
        purchaseService.updatePurchase(id, request);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") @NotNull @Min(1) Long id) {
        purchaseService.deletePurchase(id);
        return ApiResponse.success();
    }
}
