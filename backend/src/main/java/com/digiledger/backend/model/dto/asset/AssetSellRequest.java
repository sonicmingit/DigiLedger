package com.digiledger.backend.model.dto.asset;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 出售向导请求体。
 */
public class AssetSellRequest {

    private Long platformId;

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

    public Long getPlatformId() {
        return platformId;
    }

    public void setPlatformId(Long platformId) {
        this.platformId = platformId;
    }

    public String getBuyer() {
        return buyer;
    }

    public void setBuyer(String buyer) {
        this.buyer = buyer;
    }

    public BigDecimal getSalePrice() {
        return salePrice;
    }

    public void setSalePrice(BigDecimal salePrice) {
        this.salePrice = salePrice;
    }

    public BigDecimal getFee() {
        return fee;
    }

    public void setFee(BigDecimal fee) {
        this.fee = fee;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }

    public BigDecimal getOtherCost() {
        return otherCost;
    }

    public void setOtherCost(BigDecimal otherCost) {
        this.otherCost = otherCost;
    }

    public LocalDate getSaleDate() {
        return saleDate;
    }

    public void setSaleDate(LocalDate saleDate) {
        this.saleDate = saleDate;
    }

    public List<String> getAttachments() {
        return attachments;
    }

    public void setAttachments(List<String> attachments) {
        this.attachments = attachments;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }
}
