package com.digiledger.backend.model.dto.dict;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 平台创建/更新请求。
 */
public class PlatformRequest {

    @NotBlank(message = "平台名称不能为空")
    @Size(max = 100, message = "平台名称长度需在 100 字以内")
    private String name;

    @Size(max = 255, message = "平台链接长度需在 255 字以内")
    private String link;

    private Integer sort = 0;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLink() {
        return link;
    }

    public void setLink(String link) {
        this.link = link;
    }

    public Integer getSort() {
        return sort;
    }

    public void setSort(Integer sort) {
        this.sort = sort;
    }
}
