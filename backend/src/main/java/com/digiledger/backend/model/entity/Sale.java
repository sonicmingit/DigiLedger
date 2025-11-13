package com.digiledger.backend.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Sale {
    /** 主键 ID */
    private Long id;
    /** 资产 ID */
    private Long assetId;
    /** 出售范围 */
    private String saleScope;
    /** 关联购买记录 ID（配件出售） */
    private Long purchaseId;
    /** 出售平台 ID */
    private Long platformId;
    /** 出售平台名称（冗余） */
    private String platformName;
    /** 买家名称 */
    private String buyer;
    /** 售价 */
    private BigDecimal salePrice;
    /** 平台手续费 */
    private BigDecimal fee;
    /** 运费 */
    private BigDecimal shippingCost;
    /** 其他费用 */
    private BigDecimal otherCost;
    /** 净收入 */
    private BigDecimal netIncome;
    /** 售出日期 */
    private LocalDate saleDate;
    /** 附件 JSON */
    private String attachments;
    /** 备注 */
    private String notes;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
