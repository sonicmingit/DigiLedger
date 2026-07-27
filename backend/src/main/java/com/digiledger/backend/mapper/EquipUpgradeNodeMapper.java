package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.EquipUpgradeNode;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface EquipUpgradeNodeMapper {

    List<EquipUpgradeNode> findByRouteId(@Param("routeId") Long routeId);

    EquipUpgradeNode findById(@Param("id") Long id);

    /** 查询同一路线中仍有效的真实物品节点，用于阻止重复挂载。 */
    EquipUpgradeNode findActiveByRouteAndAsset(@Param("routeId") Long routeId,
                                                @Param("assetId") Long assetId);

    int insert(EquipUpgradeNode node);
    int update(EquipUpgradeNode node);

    int softDelete(@Param("id") Long id);

    int softDeleteByRoute(@Param("routeId") Long routeId);
    
    int softDeleteByAssetId(@Param("assetId") Long assetId);

    /** 心愿变为已购物品后，保留节点 ID 和关系，仅替换为真实物品引用。 */
    int bindWishlistNodesToAsset(@Param("wishlistId") Long wishlistId, @Param("assetId") Long assetId);

    int updateLayout(@Param("id") Long id, @Param("level") Integer level, @Param("sort") Integer sort);

    int setMainlineAtLevelExcept(@Param("routeId") Long routeId, @Param("level") Integer level,
                                 @Param("nodeId") Long nodeId);

    int softDeleteSequenceLinksByRoute(@Param("routeId") Long routeId);

    /** 在插入前代时整体下移世代层级，确保代际标签连续可解释。 */
    int increaseLevelFrom(@Param("routeId") Long routeId, @Param("level") Integer level);
}
