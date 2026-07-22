package com.digiledger.backend.model.dto.wishlist;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

/** capturedAt 可省略，由服务端使用当前时间；显式传入时保留客户端采集时刻。 */
public record WishlistPriceRequest(
        @NotNull @DecimalMin(value = "0.00") BigDecimal currentPrice,
        OffsetDateTime capturedAt) { }
