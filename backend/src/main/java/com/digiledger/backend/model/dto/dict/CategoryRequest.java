package com.digiledger.backend.model.dto.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 类别创建/更新请求。
 */
public class CategoryRequest {

    @NotBlank(message = "类别名称不能为空")
    @Size(max = 100, message = "类别名称长度需在 100 字以内")
    private String name;

    private Long parentId;

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

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
