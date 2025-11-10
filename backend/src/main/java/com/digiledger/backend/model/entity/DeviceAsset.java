package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
public class DeviceAsset {
    private Long id;
    private String name;
    private String category;
    private String brand;
    private String model;
    private String serialNo;
    private String status;
    private Long purchaseId;
    private LocalDate purchaseDate;
    private LocalDate enabledDate;
    private LocalDate saleDate;
    private LocalDate retiredDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
