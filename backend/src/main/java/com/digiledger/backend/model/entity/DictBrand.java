package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DictBrand {
    private Long id;
    private String name;
    private String alias;
    private String initial;
    private Integer sort;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
