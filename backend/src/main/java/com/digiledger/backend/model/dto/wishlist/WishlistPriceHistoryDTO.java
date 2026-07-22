package com.digiledger.backend.model.dto.wishlist;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WishlistPriceHistoryDTO(BigDecimal price, LocalDateTime capturedAt) { }
