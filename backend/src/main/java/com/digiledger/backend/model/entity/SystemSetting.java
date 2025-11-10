package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemSetting {
    private Long id;
    private double annualRate;
    private String currency;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
