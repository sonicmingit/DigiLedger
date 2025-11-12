package com.digiledger.backend.model.entity;

import lombok.Data;

import java.math.BigDecimal;
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
    /** 类别 ID */
    private Long categoryId;
    /** 品牌 ID */
    private Long brandId;
    /** 型号 */
    private String model;
    /** 期望价格 */
    private BigDecimal expectedPrice;
    /** 心愿图片 */
    private String imageUrl;
    /** 商品链接 */
    private String link;
    /** 心愿状态 */
    private String status;
    /** 备注 */
    private String notes;
    /** 转化后的资产 ID */
    private Long convertedAssetId;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
