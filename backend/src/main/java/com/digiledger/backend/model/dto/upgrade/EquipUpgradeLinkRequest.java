package com.digiledger.backend.model.dto.upgrade;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 升级关系新增请求体。
 */
public class EquipUpgradeLinkRequest {

    @NotNull(message = "前代节点必填")
    private Long fromNodeId;

    @NotNull(message = "后代节点必填")
    private Long toNodeId;

    @Size(max = 2000, message = "备注长度需在2000字以内")
    private String remark;

    public Long getFromNodeId() {
        return fromNodeId;
    }

    public void setFromNodeId(Long fromNodeId) {
        this.fromNodeId = fromNodeId;
    }

    public Long getToNodeId() {
        return toNodeId;
    }

    public void setToNodeId(Long toNodeId) {
        this.toNodeId = toNodeId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
