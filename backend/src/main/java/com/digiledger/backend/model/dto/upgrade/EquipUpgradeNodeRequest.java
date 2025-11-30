package com.digiledger.backend.model.dto.upgrade;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 升级节点新增请求体。
 */
public class EquipUpgradeNodeRequest {

    @NotNull(message = "关联资产必填")
    private Long assetId;

    @Min(value = 1, message = "层级至少为1")
    private Integer level = 1;

    private Integer sort = 0;

    @Size(max = 200, message = "节点标签长度需在200字以内")
    private String label;

    @Size(max = 2000, message = "备注长度需在2000字以内")
    private String remark;

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
}
