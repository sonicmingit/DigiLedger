package com.digiledger.backend.model.dto.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 品牌新增/编辑请求。
 */
public class BrandRequest {

    @NotBlank(message = "品牌名称不能为空")
    @Size(max = 100, message = "品牌名称过长")
    private String name;

    @Size(max = 200, message = "别名过长")
    private String alias;

    @Size(max = 1, message = "首字母格式不正确")
    private String initial;

    private Integer sort;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getAlias() {
        return alias;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public String getInitial() {
        return initial;
    }

    public void setInitial(String initial) {
        this.initial = initial;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
