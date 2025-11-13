package com.digiledger.backend.model.dto.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class BrandRequest {

    @NotBlank(message = "品牌名称不能为空")
    @Size(max = 100, message = "品牌名称长度需在 100 字以内")
    private String name;

    @Size(max = 100, message = "别名长度需在 100 字以内")
    private String alias;

    @Size(max = 10, message = "首字母长度需在 10 字以内")
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
