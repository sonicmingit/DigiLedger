package com.digiledger.backend.model.dto.dict;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签树节点。
 */
public class TagTreeNodeDTO {
    private Long id;
    private String name;
    private Long parentId;
    private String color;
    private String icon;
    private Integer sort;
    private List<TagTreeNodeDTO> children = new ArrayList<>();

    public TagTreeNodeDTO() {
    }

    public TagTreeNodeDTO(Long id, String name, Long parentId, String color, String icon, Integer sort) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.color = color;
        this.icon = icon;
        this.sort = sort;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public List<TagTreeNodeDTO> getChildren() {
        return children;
    }

    public void setChildren(List<TagTreeNodeDTO> children) {
        this.children = children;
    }
}
