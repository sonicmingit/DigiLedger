package com.digiledger.backend.model.dto.wishlist;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;


import java.math.BigDecimal;

import lombok.Data;

/**
 * 心愿单创建/更新请求。
 */
@Data
public class WishlistRequest {

    @NotBlank(message = "名称不能为空")
    @Size(max = 200, message = "名称过长")
    private String name;

    private Long categoryId;

    private Long brandId;

    @Size(max = 500, message = "图片链接过长")
    private String imageUrl;

    @Size(max = 500, message = "链接过长")
    private String link;

    private String notes;

    @Min(value = 1, message = "优先级需在 1-5 之间")
    @Max(value = 5, message = "优先级需在 1-5 之间")
    private Integer priority = 3;

    @Size(max = 200, message = "型号过长")
    private String model;

    @DecimalMin(value = "0", message = "期望价格需大于等于 0")
    private BigDecimal expectedPrice;

    @Pattern(regexp = "未购买|已购买", message = "状态非法")
    private String status = "未购买";
}
