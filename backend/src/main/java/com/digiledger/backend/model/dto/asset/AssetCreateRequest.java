package com.digiledger.backend.model.dto.asset;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

/**
 * 资产创建请求。
 */
@Data
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
}
