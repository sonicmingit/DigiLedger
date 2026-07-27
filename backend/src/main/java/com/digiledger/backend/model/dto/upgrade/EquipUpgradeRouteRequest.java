package com.digiledger.backend.model.dto.upgrade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Pattern;
import java.math.BigDecimal;
import jakarta.validation.Valid;

/**
 * 升级路线新增/编辑请求体。
 */
public class EquipUpgradeRouteRequest {

    @NotBlank(message = "路线名称不能为空")
    @Size(max = 200, message = "路线名称长度需在200字以内")
    private String name;

    private Long rootAssetId;
    /** 路线当前正在使用的主物品，必须属于当前路线。 */
    private Long mainAssetId;

    @Size(max = 2000, message = "备注长度需在2000字以内")
    private String remark;
    @Min(2000) @Max(2200)
    private Integer planYear;
    @DecimalMin("0.00")
    private BigDecimal annualBudget;

    @Pattern(regexp = "ACTUAL|PLAN|MIXED", message = "路线类型仅支持 ACTUAL、PLAN 或 MIXED")
    private String routeType = "MIXED";

    @Pattern(regexp = "ACTIVE|COMPLETED|ARCHIVED", message = "路线状态仅支持 ACTIVE、COMPLETED 或 ARCHIVED")
    private String status = "ACTIVE";

    /**
     * 新版创建流程的首节点。保留为空是为了兼容已发布的旧客户端；新版界面必须传入。
     */
    @Valid
    private EquipUpgradeNodeRequest firstNode;

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
    public Long getMainAssetId() { return mainAssetId; }
    public void setMainAssetId(Long mainAssetId) { this.mainAssetId = mainAssetId; }

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
    public String getRouteType() { return routeType; }
    public void setRouteType(String routeType) { this.routeType = routeType; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public EquipUpgradeNodeRequest getFirstNode() { return firstNode; }
    public void setFirstNode(EquipUpgradeNodeRequest firstNode) { this.firstNode = firstNode; }
}
