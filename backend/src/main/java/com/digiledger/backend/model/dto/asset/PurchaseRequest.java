package com.digiledger.backend.model.dto.asset;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 购买记录创建请求。
 */
public class PurchaseRequest {

    @NotBlank(message = "购买类型不能为空")
    private String type;

    @Size(max = 100, message = "平台名称过长")
    private String platform;

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

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getSeller() {
        return seller;
    }

    public void setSeller(String seller) {
        this.seller = seller;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getShippingCost() {
        return shippingCost;
    }

    public void setShippingCost(BigDecimal shippingCost) {
        this.shippingCost = shippingCost;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public String getInvoiceNo() {
        return invoiceNo;
    }

    public void setInvoiceNo(String invoiceNo) {
        this.invoiceNo = invoiceNo;
    }

    public Integer getWarrantyMonths() {
        return warrantyMonths;
    }

    public void setWarrantyMonths(Integer warrantyMonths) {
        this.warrantyMonths = warrantyMonths;
    }

    public LocalDate getWarrantyExpireDate() {
        return warrantyExpireDate;
    }

    public void setWarrantyExpireDate(LocalDate warrantyExpireDate) {
        this.warrantyExpireDate = warrantyExpireDate;
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
