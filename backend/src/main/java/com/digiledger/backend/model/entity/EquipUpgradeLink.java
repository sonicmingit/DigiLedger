package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 装备升级关系实体。
 */
@Data
public class EquipUpgradeLink {
    /** 主键ID */
    private Long id;
    /** 路线ID */
    private Long routeId;
    /** 前代节点ID */
    private Long fromNodeId;
    /** 后代节点ID */
    private Long toNodeId;
    /** 备注 */
    private String remark;
    /** 删除标记 */
    private Integer isDeleted;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
