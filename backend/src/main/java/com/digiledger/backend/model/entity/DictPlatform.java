package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DictPlatform {
    private Long id;
    private String name;
    private String link;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
