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

    @Pattern(regexp = "ASSET|WISHLIST|PLANNED", message = "节点类型仅支持 ASSET、WISHLIST 或 PLANNED")
    private String nodeType;

    /** 从心愿单选择的待购物品；购买完成后自动替换为真实物品。 */
    @Min(value = 1, message = "心愿单ID必须大于0")
    private Long wishlistId;

    /** 同级节点是否作为该层主物品参与上下级计算。 */
    private Boolean mainline;

    /** 锚点节点；传入后由后端在同一事务中创建节点与关系。 */
    @Min(value = 1, message = "锚点节点必须大于0")
    private Long anchorNodeId;

    @Pattern(regexp = "BEFORE|ALTERNATIVE|AFTER", message = "添加位置仅支持 BEFORE、ALTERNATIVE 或 AFTER")
    private String position;

    /** 当锚点已有前代时，BEFORE 必须明确选择 INSERT（插入）或 BRANCH（新分支）。 */
    @Pattern(regexp = "INSERT|BRANCH", message = "上级关系处理仅支持 INSERT 或 BRANCH")
    private String beforeMode;

    @Min(value = 1, message = "层级至少为1")
    /**
     * 仅旧版独立创建接口可显式传入；编辑节点时为空，服务层必须保留原代际。
     */
    private Integer level;

    /** 编辑节点时为空，避免缺省值把纵向顺序重置为 0。 */
    private Integer sort;

    @Size(max = 200, message = "节点标签长度需在200字以内")
    private String label;

    @Size(max = 100, message = "同级用途长度需在100字以内")
    private String alternativePurpose;

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
    public String getNodeType() { return nodeType; }
    public void setNodeType(String nodeType) { this.nodeType = nodeType; }
    public Long getWishlistId() { return wishlistId; }
    public void setWishlistId(Long wishlistId) { this.wishlistId = wishlistId; }
    public Boolean getMainline() { return mainline; }
    public void setMainline(Boolean mainline) { this.mainline = mainline; }
    public Long getAnchorNodeId() { return anchorNodeId; }
    public void setAnchorNodeId(Long anchorNodeId) { this.anchorNodeId = anchorNodeId; }
    public String getPosition() { return position; }
    public void setPosition(String position) { this.position = position; }
    public String getBeforeMode() { return beforeMode; }
    public void setBeforeMode(String beforeMode) { this.beforeMode = beforeMode; }

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
    public String getAlternativePurpose() { return alternativePurpose; }
    public void setAlternativePurpose(String alternativePurpose) { this.alternativePurpose = alternativePurpose; }

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
