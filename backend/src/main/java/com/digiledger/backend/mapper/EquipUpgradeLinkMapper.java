package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.EquipUpgradeLink;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EquipUpgradeLinkMapper {

    List<EquipUpgradeLink> findByRouteId(@Param("routeId") Long routeId);

    EquipUpgradeLink findById(@Param("id") Long id);

    int insert(EquipUpgradeLink link);

    int softDelete(@Param("id") Long id);

    int softDeleteByNodeId(@Param("nodeId") Long nodeId);

    int softDeleteByRoute(@Param("routeId") Long routeId);

    /** 将原指向锚点的前后代关系改接至新插入的节点。 */
    int retargetSequenceLinks(@Param("routeId") Long routeId,
                              @Param("oldToNodeId") Long oldToNodeId,
                              @Param("newToNodeId") Long newToNodeId);
}
