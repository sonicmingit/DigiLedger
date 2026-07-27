package com.digiledger.backend.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;

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
    /** 最近一次观测价格；历史数据允许为空。 */
    private BigDecimal currentPrice;
    /** 最近一次价格采集时间。 */
    private LocalDateTime lastPriceAt;
    /** 心愿图片 */
    private String imageUrl;
    /** 商品链接 */
    private String link;
    /** 获知心愿的来源，供后续复盘购买决策。 */
    private String source;
    /** 心愿状态 */
    private String status;
    /** 备注 */
    private String notes;
    /** 优先级（1-5） */
    private Integer priority;
    /** 转化后的资产 ID */
    private Long convertedAssetId;
    /** 购买确认时固定的实际购买信息，用于保留心愿与购买之间的差异。 */
    private LocalDate purchasedAt;
    private BigDecimal purchasedPrice;
    private BigDecimal purchasePriceDiff;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
