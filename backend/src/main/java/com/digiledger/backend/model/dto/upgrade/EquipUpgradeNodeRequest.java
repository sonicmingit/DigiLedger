package com.digiledger.backend.model.dto.upgrade;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import jakarta.validation.constraints.Size;

/**
 * 升级节点新增请求体。
 */
public class EquipUpgradeNodeRequest {

    private Long assetId;

    @Min(value = 1, message = "层级至少为1")
    private Integer level = 1;

    private Integer sort = 0;

    @Size(max = 200, message = "节点标签长度需在200字以内")
    private String label;

    @Size(max = 2000, message = "备注长度需在2000字以内")
    private String remark;
    @Size(max = 200) private String title;
    @Size(max = 200) private String targetName;
    @Size(max = 50) private String periodLabel;
    @DecimalMin("0.00") private BigDecimal plannedBudget;
    @DecimalMin("0.00") private BigDecimal expectedRecovery;
    @Pattern(regexp = "PLANNED|READY|EXECUTING|COMPLETED|CANCELLED")
    private String status = "PLANNED";

    public Long getAssetId() {
        return assetId;
    }

    public void setAssetId(Long assetId) {
        this.assetId = assetId;
    }

    public Integer getLevel() {
        return level;
    }

    public void setLevel(Integer level) {
        this.level = level;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getTargetName() { return targetName; }
    public void setTargetName(String targetName) { this.targetName = targetName; }
    public String getPeriodLabel() { return periodLabel; }
    public void setPeriodLabel(String periodLabel) { this.periodLabel = periodLabel; }
    public BigDecimal getPlannedBudget() { return plannedBudget; }
    public void setPlannedBudget(BigDecimal plannedBudget) { this.plannedBudget = plannedBudget; }
    public BigDecimal getExpectedRecovery() { return expectedRecovery; }
    public void setExpectedRecovery(BigDecimal expectedRecovery) { this.expectedRecovery = expectedRecovery; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
