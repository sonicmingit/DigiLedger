package com.digiledger.backend.service;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.mapper.AssetTagMapMapper;
import com.digiledger.backend.mapper.DictBrandMapper;
import com.digiledger.backend.mapper.DictCategoryMapper;
import com.digiledger.backend.mapper.DictPlatformMapper;
import com.digiledger.backend.mapper.DictTagMapper;
import com.digiledger.backend.mapper.PurchaseMapper;
import com.digiledger.backend.mapper.SaleMapper;
import com.digiledger.backend.model.dto.asset.AssetSummaryDTO;
import com.digiledger.backend.model.dto.asset.PurchaseRequest;
import com.digiledger.backend.model.entity.DeviceAsset;
import com.digiledger.backend.model.entity.DictCategory;
import com.digiledger.backend.service.impl.AssetServiceImpl;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AssetServiceImplTest {

    private AssetMapper assetMapper;
    private PurchaseMapper purchaseMapper;
    private SaleMapper saleMapper;
    private DictCategoryMapper dictCategoryMapper;
    private DictPlatformMapper dictPlatformMapper;
    private DictTagMapper dictTagMapper;
    private AssetTagMapMapper assetTagMapMapper;
    private DictBrandMapper dictBrandMapper;
    private AssetServiceImpl assetService;

    @BeforeEach
    void setUp() {
        assetMapper = mock(AssetMapper.class);
        purchaseMapper = mock(PurchaseMapper.class);
        saleMapper = mock(SaleMapper.class);
        dictCategoryMapper = mock(DictCategoryMapper.class);
        dictPlatformMapper = mock(DictPlatformMapper.class);
        dictTagMapper = mock(DictTagMapper.class);
        assetTagMapMapper = mock(AssetTagMapMapper.class);
        dictBrandMapper = mock(DictBrandMapper.class);
        assetService = new AssetServiceImpl(
                assetMapper,
                purchaseMapper,
                saleMapper,
                dictCategoryMapper,
                dictPlatformMapper,
                dictTagMapper,
                assetTagMapMapper,
                dictBrandMapper,
                new ObjectMapper()
        );
    }

    @Test
    void shouldReturnAssetsUnderChildCategoriesWhenFilteringByParent() {
        Long parentId = 1L;
        DictCategory parent = new DictCategory();
        parent.setId(parentId);
        parent.setName("摄影器材");
        when(dictCategoryMapper.findById(parentId)).thenReturn(parent);

        DeviceAsset childAsset = new DeviceAsset();
        childAsset.setId(10L);
        childAsset.setName("Sony A7M4");
        childAsset.setCategoryId(5L);
        childAsset.setCategoryPath("/1/4/5");
        childAsset.setStatus("使用中");
        childAsset.setEnabledDate(LocalDate.now());
        childAsset.setPurchaseDate(LocalDate.now());

        when(assetMapper.findAll(eq(null), eq(null), eq(parentId), eq("%/" + parentId + "/%"), eq("%/" + parentId),
                eq(null), eq(null), eq(null))).thenReturn(List.of(childAsset));
        when(purchaseMapper.findByAssetId(10L)).thenReturn(Collections.emptyList());
        when(saleMapper.findByAssetId(10L)).thenReturn(Collections.emptyList());
        when(dictTagMapper.findByAssetId(10L)).thenReturn(Collections.emptyList());

        List<AssetSummaryDTO> result = assetService.listAssets(null, null, parentId, null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).id()).isEqualTo(10L);

        ArgumentCaptor<Long> categoryCaptor = ArgumentCaptor.forClass(Long.class);
        verify(assetMapper).findAll(eq(null), eq(null), categoryCaptor.capture(), any(), any(), eq(null), eq(null), eq(null));
        assertThat(categoryCaptor.getValue()).isEqualTo(parentId);
    }

    @Test
    void shouldRequireAccessoryName() {
        Long parentId = 1L;
        DictCategory parent = new DictCategory();
        parent.setId(parentId);
        parent.setParentId(null);
        parent.setLevel(1);

        DictCategory child = new DictCategory();
        child.setId(2L);
        child.setParentId(parentId);
        child.setLevel(2);

        when(dictCategoryMapper.findAll()).thenReturn(List.of(parent, child));
        when(dictCategoryMapper.countChildren(2L)).thenReturn(0);
        when(assetMapper.insert(any(DeviceAsset.class))).thenAnswer(invocation -> {
            DeviceAsset saved = invocation.getArgument(0);
            saved.setId(100L);
            return 1;
        });
        doNothing().when(assetTagMapMapper).deleteByAssetId(anyLong());
        when(purchaseMapper.findByAssetId(anyLong())).thenReturn(Collections.emptyList());

        com.digiledger.backend.model.dto.asset.AssetCreateRequest request = new com.digiledger.backend.model.dto.asset.AssetCreateRequest();
        request.setName("测试资产");
        request.setCategoryId(2L);
        request.setStatus("使用中");
        request.setEnabledDate(LocalDate.now());
        request.setPurchaseDate(LocalDate.now());

        PurchaseRequest accessory = new PurchaseRequest();
        accessory.setType("ACCESSORY");
        accessory.setName(" ");
        accessory.setPrice(new BigDecimal("100"));
        accessory.setShippingCost(BigDecimal.ZERO);
        accessory.setPurchaseDate(LocalDate.now());
        request.setPurchases(List.of(accessory));

        assertThatThrownBy(() -> assetService.createAsset(request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("配件/服务名称必填");
    }
}
