package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.AssetTagMap;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AssetTagMapMapper {

    int deleteByAssetId(@Param("assetId") Long assetId);

    int batchInsert(@Param("list") List<AssetTagMap> mappings);

    long countByTagId(@Param("tagId") Long tagId);
}
