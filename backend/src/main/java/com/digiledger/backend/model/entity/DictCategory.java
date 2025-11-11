package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DictCategory {
    private Long id;
    private String name;
    private Long parentId;
    private Integer level;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
