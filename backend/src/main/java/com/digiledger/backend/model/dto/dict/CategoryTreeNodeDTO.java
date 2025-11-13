package com.digiledger.backend.model.dto.dict;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 类别树节点。
 */
@Data
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

}
