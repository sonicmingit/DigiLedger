package com.digiledger.backend.service;

import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.mapper.PurchaseMapper;
import com.digiledger.backend.mapper.SaleMapper;
import com.digiledger.backend.mapper.SystemSettingMapper;
import com.digiledger.backend.model.dto.AssetSummaryDTO;
import com.digiledger.backend.model.entity.DeviceAsset;
import com.digiledger.backend.model.entity.SystemSetting;
import com.digiledger.backend.service.impl.AssetServiceImpl;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AssetServiceImplTest {

    @Test
    void shouldCalculateBookValue() {
        AssetMapper assetMapper = mock(AssetMapper.class);
        PurchaseMapper purchaseMapper = mock(PurchaseMapper.class);
        SaleMapper saleMapper = mock(SaleMapper.class);
        SystemSettingMapper systemSettingMapper = mock(SystemSettingMapper.class);

        DeviceAsset asset = new DeviceAsset();
        asset.setId(1L);
        asset.setName("Test Asset");
        asset.setCategory("Laptop");
        asset.setStatus("IN_USE");
        asset.setEnabledDate(LocalDate.now().minusDays(10));

        when(assetMapper.findAll()).thenReturn(Collections.singletonList(asset));
        when(purchaseMapper.sumInvestByAsset(1L)).thenReturn(new BigDecimal("1000"));

        SystemSetting setting = new SystemSetting();
        setting.setAnnualRate(0.5);
        when(systemSettingMapper.findLatest()).thenReturn(setting);

        AssetServiceImpl service = new AssetServiceImpl(assetMapper, purchaseMapper, saleMapper, systemSettingMapper);
        AssetSummaryDTO summary = service.listAssets().get(0);

        assertThat(summary.bookValue()).isPositive();
        assertThat(summary.totalInvest()).isEqualByComparingTo("1000.00");
    }
}
