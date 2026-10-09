package com.digiledger.backend.service;

import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.mapper.DictPlatformMapper;
import com.digiledger.backend.mapper.PurchaseMapper;
import com.digiledger.backend.model.dto.asset.PurchaseUpsertRequest;
import com.digiledger.backend.model.entity.DeviceAsset;
import com.digiledger.backend.model.entity.Purchase;
import com.digiledger.backend.service.impl.PurchaseServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PurchaseWarrantyServiceTest {
    private final PurchaseMapper purchases = mock(PurchaseMapper.class);
    private final AssetMapper assets = mock(AssetMapper.class);
    private final PurchaseServiceImpl service = new PurchaseServiceImpl(purchases, assets,
            mock(DictPlatformMapper.class), new ObjectMapper());

    @Test void accessoryWarrantyIsClearedOnCreateAndUpdate() {
        DeviceAsset asset = new DeviceAsset(); asset.setId(4L);
        when(assets.findById(4L)).thenReturn(asset);
        when(purchases.findByAssetId(4L)).thenReturn(List.of());
        var request = request("ACCESSORY");
        service.createPurchase(request);
        ArgumentCaptor<Purchase> inserted = ArgumentCaptor.forClass(Purchase.class);
        verify(purchases).insert(inserted.capture());
        assertNull(inserted.getValue().getWarrantyMonths());
        assertNull(inserted.getValue().getWarrantyExpireDate());

        Purchase existing = new Purchase(); existing.setId(7L); existing.setAssetId(4L);
        when(purchases.findById(7L)).thenReturn(existing);
        service.updatePurchase(7L, request);
        ArgumentCaptor<Purchase> updated = ArgumentCaptor.forClass(Purchase.class);
        verify(purchases).update(updated.capture());
        assertNull(updated.getValue().getWarrantyMonths());
        assertNull(updated.getValue().getWarrantyExpireDate());
    }

    @Test void primaryWarrantyRemainsAvailable() {
        DeviceAsset asset = new DeviceAsset(); asset.setId(4L);
        when(assets.findById(4L)).thenReturn(asset);
        when(purchases.findByAssetId(4L)).thenReturn(List.of());
        service.createPurchase(request("PRIMARY"));
        ArgumentCaptor<Purchase> inserted = ArgumentCaptor.forClass(Purchase.class);
        verify(purchases).insert(inserted.capture());
        assertEquals(12, inserted.getValue().getWarrantyMonths());
        assertEquals(LocalDate.of(2027, 1, 1), inserted.getValue().getWarrantyExpireDate());
    }

    private PurchaseUpsertRequest request(String type) {
        PurchaseUpsertRequest request = new PurchaseUpsertRequest();
        request.setAssetId(4L); request.setType(type); request.setName("ACCESSORY".equals(type) ? "充电器" : null);
        request.setPrice(new BigDecimal("99.00")); request.setPurchaseDate(LocalDate.of(2026, 1, 1));
        request.setWarrantyMonths(12); request.setWarrantyExpireDate(LocalDate.of(2027, 1, 1));
        return request;
    }
}
