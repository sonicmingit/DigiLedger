package com.digiledger.backend.service;

import com.digiledger.backend.model.dto.asset.PurchaseUpsertRequest;

public interface PurchaseService {

    Long createPurchase(PurchaseUpsertRequest request);

    void updatePurchase(Long id, PurchaseUpsertRequest request);

    void deletePurchase(Long id);
}
