package com.digiledger.backend.model.dto.asset;

import com.digiledger.backend.model.enums.SaleScope;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 出售向导请求体。
 */
@Data
public class AssetSellRequest {

    private Long platformId;

    @NotNull(message = "出售范围不能为空")
    private SaleScope saleScope = SaleScope.ASSET;

    private Long purchaseId;

    @Size(max = 200, message = "买家信息过长")
    private String buyer;

    @NotNull(message = "售价不能为空")
    @DecimalMin(value = "0", message = "售价需大于等于 0")
    private BigDecimal salePrice;

    @DecimalMin(value = "0", message = "费用需大于等于 0")
    private BigDecimal fee = BigDecimal.ZERO;

    @DecimalMin(value = "0", message = "运费需大于等于 0")
    private BigDecimal shippingCost = BigDecimal.ZERO;

    @DecimalMin(value = "0", message = "其他费用需大于等于 0")
    private BigDecimal otherCost = BigDecimal.ZERO;

    @NotNull(message = "售出日期不能为空")
    private LocalDate saleDate;

    private List<@Size(max = 500, message = "附件 URL 过长") String> attachments;

    private String notes;

    @AssertTrue(message = "配件出售需要选择购买记录")
    public boolean isPurchaseSelectedWhenAccessory() {
        if (saleScope == null) {
            return false;
        }
        if (saleScope == SaleScope.ACCESSORY) {
            return purchaseId != null;
        }
        return true;
    }
}
