package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.EquipUpgradeNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EquipUpgradeNodeMapper {

    List<EquipUpgradeNode> findByRouteId(@Param("routeId") Long routeId);

    EquipUpgradeNode findById(@Param("id") Long id);

    int insert(EquipUpgradeNode node);
    int update(EquipUpgradeNode node);

    int softDelete(@Param("id") Long id);

    int softDeleteByRoute(@Param("routeId") Long routeId);
    
    int softDeleteByAssetId(@Param("assetId") Long assetId);
}
