package com.digiledger.backend.model.dto.upgrade;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

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
}
