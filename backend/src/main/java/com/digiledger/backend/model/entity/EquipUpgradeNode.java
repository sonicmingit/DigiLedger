package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 装备升级节点实体。
 */
@Data
public class EquipUpgradeNode {
    /** 主键ID */
    private Long id;
    /** 路线ID */
    private Long routeId;
    /** 资产ID */
    private Long assetId;
    /** 心愿单ID；心愿购买后会由 WishlistService 自动绑定到 assetId。 */
    private Long wishlistId;
    /** 节点类型：ASSET 表示真实物品，PLANNED 表示计划物品。 */
    private String nodeType;
    /** 同级节点中只有主物品参与上下级关系的时间和金额计算。 */
    private Boolean mainline;
    /** 层级 */
    private Integer level;
    /** 同层排序 */
    private Integer sort;
    /** 节点标签 */
    private String label;
    /** 同级备选或并行持有的用途说明。 */
    private String alternativePurpose;
    /** 备注 */
    private String remark;
    private String title;
    private String targetName;
    private String periodLabel;
    private BigDecimal plannedBudget;
    private BigDecimal expectedRecovery;
    private String status;
    /** 删除标记 */
    private Integer isDeleted;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
