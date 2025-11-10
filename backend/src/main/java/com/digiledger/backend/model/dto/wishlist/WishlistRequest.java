package com.digiledger.backend.model.dto.wishlist;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 心愿单创建/更新请求。
 */
public class WishlistRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 200, message = "名称过长")
    private String name;

    @Size(max = 100, message = "类别过长")
    private String category;

    @Size(max = 100, message = "品牌过长")
    private String brand;

    @Size(max = 200, message = "型号过长")
    private String model;

    @DecimalMin(value = "0", message = "期望价格需大于等于 0")
    private BigDecimal expectedPrice;

    @Size(max = 100, message = "平台名称过长")
    private String plannedPlatform;

    @Size(max = 500, message = "链接过长")
    private String link;

    private String notes;

    private Integer priority;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
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

    public BigDecimal getExpectedPrice() {
        return expectedPrice;
    }

    public void setExpectedPrice(BigDecimal expectedPrice) {
        this.expectedPrice = expectedPrice;
    }

    public String getPlannedPlatform() {
        return plannedPlatform;
    }

    public void setPlannedPlatform(String plannedPlatform) {
        this.plannedPlatform = plannedPlatform;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}
