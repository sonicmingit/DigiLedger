package com.digiledger.backend.model.entity;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WishlistTagMap {
    private Long wishlistId;
    private Long tagId;
    private LocalDateTime createdAt;
}
