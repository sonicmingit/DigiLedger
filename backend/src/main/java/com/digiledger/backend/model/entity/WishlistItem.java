package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 心愿单实体，映射 wishlist 表。
 */
@Data
public class WishlistItem {
    /** 主键 ID */
    private Long id;
    /** 名称 */
    private String name;
    /** 目标类别 ID */
    private Long categoryId;
    /** 目标品牌 ID */
    private Long brandId;
    /** 商品图片 */
    private String imageUrl;
    /** 状态 */
    private String status;
    /** 商品链接 */
    private String link;
    /** 备注 */
    private String notes;
    /** 优先级 1-5 */
    private Integer priority;
    /** 转化后的资产 ID */
    private Long convertedAssetId;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
