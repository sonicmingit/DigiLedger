package com.digiledger.backend.model.dto.asset;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Asset sell request payload
 */
@Data
public class AssetSellRequest {

    private Long platformId;

    private Long purchaseId;

    @Size(max = 200, message = "买方信息过长")
    private String buyer;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0", message = "售价必须大于等于 0")
    private BigDecimal salePrice;

    @DecimalMin(value = "0", message = "手续费必须大于等于 0")
    private BigDecimal fee = BigDecimal.ZERO;

    @DecimalMin(value = "0", message = "运费必须大于等于 0")
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @DecimalMin(value = "0", message = "其他费用必须大于等于 0")
    private BigDecimal otherCost = BigDecimal.ZERO;

    @NotNull(message = "销售日期不能为空")
    private LocalDate saleDate;

    private List<@Size(max = 500, message = "附件 URL 过长") String> attachments;

    private String notes;
}
