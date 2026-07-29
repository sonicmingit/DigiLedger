package com.digiledger.backend.service;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.mapper.*;
import com.digiledger.backend.model.entity.DeviceAsset;
import com.digiledger.backend.model.entity.Purchase;
import com.digiledger.backend.service.impl.AssetServiceImpl;
import com.digiledger.backend.util.StoragePathHelper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

class AssetServiceImplTest {

    @Test
    void deletesAssetAndItsOnlyPrimaryPurchase() {
        AssetMapper assets = mock(AssetMapper.class);
        PurchaseMapper purchases = mock(PurchaseMapper.class);
        SaleMapper sales = mock(SaleMapper.class);
        DeviceAsset asset = new DeviceAsset();
        asset.setId(1L);
        Purchase primary = new Purchase();
        primary.setId(2L);
        primary.setType("PRIMARY");
        when(assets.findById(1L)).thenReturn(asset);
        when(purchases.findByAssetId(1L)).thenReturn(List.of(primary));
        when(sales.countByAsset(1L)).thenReturn(0);

        service(assets, purchases, sales).deleteAsset(1L);

        verify(purchases).delete(2L);
        verify(assets).delete(1L);
    }

    @Test
    void keepsDeletionBlockedWhenAdditionalPurchaseRecordsExist() {
        AssetMapper assets = mock(AssetMapper.class);
        PurchaseMapper purchases = mock(PurchaseMapper.class);
        SaleMapper sales = mock(SaleMapper.class);
        DeviceAsset asset = new DeviceAsset();
        asset.setId(1L);
        Purchase primary = new Purchase();
        primary.setType("PRIMARY");
        Purchase accessory = new Purchase();
        accessory.setType("ACCESSORY");
        when(assets.findById(1L)).thenReturn(asset);
        when(purchases.findByAssetId(1L)).thenReturn(List.of(primary, accessory));
        when(sales.countByAsset(1L)).thenReturn(0);

        assertThrows(BizException.class, () -> service(assets, purchases, sales).deleteAsset(1L));

        verify(purchases, never()).delete(anyLong());
        verify(assets, never()).delete(anyLong());
    }

    private AssetServiceImpl service(AssetMapper assets, PurchaseMapper purchases, SaleMapper sales) {
        return new AssetServiceImpl(assets, mock(EquipUpgradeNodeMapper.class), purchases, sales,
                mock(DictCategoryMapper.class), mock(DictBrandMapper.class), mock(DictPlatformMapper.class),
                mock(DictTagMapper.class), mock(AssetTagMapMapper.class), new ObjectMapper(), mock(StoragePathHelper.class));
    }
}
