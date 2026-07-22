package com.digiledger.backend.model.dto.upgrade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import java.math.BigDecimal;

/**
 * 升级路线新增/编辑请求体。
 */
public class EquipUpgradeRouteRequest {

    @NotBlank(message = "路线名称不能为空")
    @Size(max = 200, message = "路线名称长度需在200字以内")
    private String name;

    private Long rootAssetId;

    @Size(max = 2000, message = "备注长度需在2000字以内")
    private String remark;
    @Min(2000) @Max(2200)
    private Integer planYear;
    @DecimalMin("0.00")
    private BigDecimal annualBudget;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getRootAssetId() {
        return rootAssetId;
    }

    public void setRootAssetId(Long rootAssetId) {
        this.rootAssetId = rootAssetId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
    public Integer getPlanYear() { return planYear; }
    public void setPlanYear(Integer planYear) { this.planYear = planYear; }
    public BigDecimal getAnnualBudget() { return annualBudget; }
    public void setAnnualBudget(BigDecimal annualBudget) { this.annualBudget = annualBudget; }
}
