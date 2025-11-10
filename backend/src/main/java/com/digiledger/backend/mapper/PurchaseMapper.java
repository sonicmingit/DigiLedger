package com.digiledger.backend.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.math.BigDecimal;

@Mapper
public interface PurchaseMapper {

    BigDecimal sumInvestByAsset(@Param("assetId") Long assetId);
}
