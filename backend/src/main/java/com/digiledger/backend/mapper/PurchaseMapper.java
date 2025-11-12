package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.Purchase;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;
import java.util.List;

@Mapper
public interface PurchaseMapper {

    List<Purchase> findByAssetId(@Param("assetId") Long assetId);

    Purchase findById(@Param("id") Long id);

    BigDecimal sumInvestByAsset(@Param("assetId") Long assetId);

    int insert(Purchase purchase);

    int update(Purchase purchase);

    int delete(@Param("id") Long id);

    int deleteByAsset(@Param("assetId") Long assetId);

    int countByAsset(@Param("assetId") Long assetId);

    long countByPlatform(@Param("platformId") Long platformId);
}
