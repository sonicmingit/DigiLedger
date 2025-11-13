package com.digiledger.backend.model.dto.asset;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 购买记录创建请求。
 */
@Data
public class PurchaseRequest {

    @NotBlank(message = "购买类型不能为空")
    private String type;

    @Size(max = 200, message = "名称长度需在 200 字以内")
    private String name;

    private Long platformId;

    @Size(max = 200, message = "卖家信息过长")
    private String seller;

    @NotNull(message = "金额不能为空")
    @DecimalMin(value = "0", message = "金额需大于等于 0")
    private BigDecimal price;

    @DecimalMin(value = "0", message = "运费需大于等于 0")
    private BigDecimal shippingCost;

    private String currency = "CNY";

    @Min(value = 1, message = "数量至少为 1")
    private Integer quantity = 1;

    @NotNull(message = "购买日期不能为空")
    private LocalDate purchaseDate;

    @Size(max = 100, message = "发票号过长")
    private String invoiceNo;

    @Min(value = 0, message = "质保月数需大于等于 0")
    private Integer warrantyMonths;

    private LocalDate warrantyExpireDate;

    private List<@Size(max = 500, message = "附件 URL 过长") String> attachments;

    private String notes;
}
