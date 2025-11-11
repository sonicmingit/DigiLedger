package com.digiledger.backend.model.dto.dict;

import java.util.ArrayList;
import java.util.List;

/**
 * 类别树节点。
 */
public class CategoryTreeNodeDTO {
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private Integer sort;
    private List<CategoryTreeNodeDTO> children = new ArrayList<>();

    public CategoryTreeNodeDTO() {
    }

    public CategoryTreeNodeDTO(Long id, String name, Long parentId, Integer level, Integer sort) {
        this.id = id;
        this.name = name;
        this.parentId = parentId;
        this.level = level;
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

    public List<CategoryTreeNodeDTO> getChildren() {
        return children;
    }

    public void setChildren(List<CategoryTreeNodeDTO> children) {
        this.children = children;
    }
}
