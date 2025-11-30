package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

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
    /** 层级 */
    private Integer level;
    /** 同层排序 */
    private Integer sort;
    /** 节点标签 */
    private String label;
    /** 备注 */
    private String remark;
    /** 删除标记 */
    private Integer isDeleted;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
