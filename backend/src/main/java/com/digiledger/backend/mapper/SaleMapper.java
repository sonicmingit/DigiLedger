package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.Sale;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface SaleMapper {

    Sale findLatestByAsset(@Param("assetId") Long assetId);
}
