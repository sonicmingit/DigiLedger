package com.digiledger.backend.model.dto.wishlist;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * 心愿单创建/更新请求。
 */
public class WishlistRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 200, message = "名称过长")
    private String name;

    private Long categoryId;

    private Long brandId;

    @Size(max = 200, message = "型号过长")
    private String model;

    @DecimalMin(value = "0", message = "期望价格需大于等于 0")
    private BigDecimal expectedPrice;

    @Size(max = 500, message = "图片链接过长")
    private String imageUrl;

    @Size(max = 500, message = "链接过长")
    private String link;

    @Pattern(regexp = "未购买|已购买", message = "状态非法")
    private String status = "未购买";

    private String notes;

    @Min(value = 1, message = "优先级需在 1-5 之间")
    @Max(value = 5, message = "优先级需在 1-5 之间")
    private Integer priority = 3;

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

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
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
