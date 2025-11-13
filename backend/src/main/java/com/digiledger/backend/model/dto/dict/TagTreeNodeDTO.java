package com.digiledger.backend.model.dto.dict;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 标签树节点。
 */
@Data
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
}
