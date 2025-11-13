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
import com.digiledger.backend.model.dto.dict.BrandRequest;
import com.digiledger.backend.service.impl.DictServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DictServiceImplTest {

    @Test
    void shouldFailWhenBrandCreatedConcurrently() {
        DictCategoryMapper dictCategoryMapper = mock(DictCategoryMapper.class);
        DictPlatformMapper dictPlatformMapper = mock(DictPlatformMapper.class);
        DictTagMapper dictTagMapper = mock(DictTagMapper.class);
        DictBrandMapper dictBrandMapper = mock(DictBrandMapper.class);
        AssetMapper assetMapper = mock(AssetMapper.class);
        PurchaseMapper purchaseMapper = mock(PurchaseMapper.class);
        SaleMapper saleMapper = mock(SaleMapper.class);
        AssetTagMapMapper assetTagMapMapper = mock(AssetTagMapMapper.class);

        DictServiceImpl service = new DictServiceImpl(
                dictCategoryMapper,
                dictPlatformMapper,
                dictTagMapper,
                dictBrandMapper,
                assetMapper,
                purchaseMapper,
                saleMapper,
                assetTagMapMapper
        );

        when(dictBrandMapper.countByName("Sony", null)).thenReturn(0L);
        when(dictBrandMapper.insert(any())).thenThrow(new DuplicateKeyException("duplicate"));

        BrandRequest request = new BrandRequest();
        request.setName("Sony");

        assertThatThrownBy(() -> service.createBrand(request))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("品牌名称已存在");
    }
}
