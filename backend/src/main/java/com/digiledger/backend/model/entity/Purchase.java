package com.digiledger.backend.model.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class Purchase {
    /** 主键 ID */
    private Long id;
    /** 资产 ID */
    private Long assetId;
    /** 购买类型 */
    private String type;
    /** 购买平台 ID */
    private Long platformId;
    /** 购买平台名称（冗余） */
    private String platformName;
    /** 卖家信息 */
    private String seller;
    /** 金额 */
    private BigDecimal price;
    /** 币种 */
    private String currency;
    /** 数量 */
    private Integer quantity;
    /** 运费 */
    private BigDecimal shippingCost;
    /** 购买日期 */
    private LocalDate purchaseDate;
    /** 发票号 */
    private String invoiceNo;
    /** 质保月数 */
    private Integer warrantyMonths;
    /** 质保到期日期 */
    private LocalDate warrantyExpireDate;
    /** 附件 JSON */
    private String attachments;
    /** 备注 */
    private String notes;
    /** 创建时间 */
    private LocalDateTime createdAt;
    /** 更新时间 */
    private LocalDateTime updatedAt;
}
