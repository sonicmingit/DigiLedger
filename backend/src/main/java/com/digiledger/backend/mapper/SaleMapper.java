package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.Sale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SaleMapper {

    Sale findLatestByAsset(@Param("assetId") Long assetId);

    List<Sale> findByAssetId(@Param("assetId") Long assetId);

    Sale findById(@Param("id") Long id);

    int insert(Sale sale);

    int update(Sale sale);

    int deleteByAsset(@Param("assetId") Long assetId);

    int deleteById(@Param("id") Long id);

    int countByAsset(@Param("assetId") Long assetId);

    long countByPlatform(@Param("platformId") Long platformId);
}
