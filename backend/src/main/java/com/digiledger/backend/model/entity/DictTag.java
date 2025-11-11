package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DictTag {
    private Long id;
    private String name;
    private Long parentId;
    private String color;
    private String icon;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
