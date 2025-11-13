package com.digiledger.backend.model.dto.asset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;

import java.time.LocalDate;
import java.util.List;

/**
 * 资产创建请求。
 */
public class AssetCreateRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 200, message = "名称长度需在 200 字以内")
    private String name;

    @NotNull(message = "类别不能为空")
    private Long categoryId;

    private Long brandId;

    @Size(max = 100, message = "品牌长度需在 100 字以内")
    private String brand;

    @Size(max = 200, message = "型号长度需在 200 字以内")
    private String model;

    @Size(max = 200, message = "序列号长度需在 200 字以内")
    private String serialNo;

    @NotBlank(message = "状态不能为空")
    private String status;

    private LocalDate purchaseDate;

    private LocalDate enabledDate;

    private LocalDate retiredDate;

    @Size(max = 500, message = "封面 URL 过长")
    private String coverImageUrl;

    private String notes;

    private List<@NotNull(message = "标签ID不能为空") Long> tagIds;

    @Valid
    private List<PurchaseRequest> purchases;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
    }

    public Long getBrandId() {
        return brandId;
    }

    public void setBrandId(Long brandId) {
        this.brandId = brandId;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getSerialNo() {
        return serialNo;
    }

    public void setSerialNo(String serialNo) {
        this.serialNo = serialNo;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getPurchaseDate() {
        return purchaseDate;
    }

    public void setPurchaseDate(LocalDate purchaseDate) {
        this.purchaseDate = purchaseDate;
    }

    public LocalDate getEnabledDate() {
        return enabledDate;
    }

    public void setEnabledDate(LocalDate enabledDate) {
        this.enabledDate = enabledDate;
    }

    public LocalDate getRetiredDate() {
        return retiredDate;
    }

    public void setRetiredDate(LocalDate retiredDate) {
        this.retiredDate = retiredDate;
    }

    public String getCoverImageUrl() {
        return coverImageUrl;
    }

    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<Long> getTagIds() {
        return tagIds;
    }

    public void setTagIds(List<Long> tagIds) {
        this.tagIds = tagIds;
    }

    public List<PurchaseRequest> getPurchases() {
        return purchases;
    }

    public void setPurchases(List<PurchaseRequest> purchases) {
        this.purchases = purchases;
    }
}
