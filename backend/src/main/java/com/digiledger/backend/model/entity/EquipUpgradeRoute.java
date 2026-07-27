package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;
import java.math.BigDecimal;

/**
 * 装备升级路线主表实体。
 */
@Data
public class EquipUpgradeRoute {
    /** 主键ID */
    private Long id;
    /** 路线名称 */
    private String name;
    /** 起点资产ID */
    private Long rootAssetId;
    /** 当前正在使用的主物品；必须已经被挂入当前路线。 */
    private Long mainAssetId;
    /** 备注 */
    private String remark;
    private Integer planYear;
    private BigDecimal annualBudget;
    /** 路线类型：ACTUAL、PLAN、MIXED。 */
    private String routeType;
    /** 路线状态：ACTIVE、COMPLETED、ARCHIVED。 */
    private String status;
    /** 删除标记 */
    private Integer isDeleted;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
