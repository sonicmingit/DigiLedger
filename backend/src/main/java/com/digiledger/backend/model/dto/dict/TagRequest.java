package com.digiledger.backend.model.dto.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 标签创建/更新请求。
 */
public class TagRequest {

    @NotBlank(message = "标签名称不能为空")
    @Size(max = 100, message = "标签名称长度需在 100 字以内")
    private String name;

    private Long parentId;

    @Size(max = 16, message = "颜色值长度需在 16 字以内")
    private String color;

    @Size(max = 64, message = "图标标识长度需在 64 字以内")
    private String icon;

    private Integer sort = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Long getParentId() {
        return parentId;
    }

    public void setParentId(Long parentId) {
        this.parentId = parentId;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
