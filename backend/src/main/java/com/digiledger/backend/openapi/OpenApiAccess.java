package com.digiledger.backend.openapi;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class OpenApiAccess {
    private boolean enabled;
    private boolean allowWrite;
    private String tokenHash;
    private String tokenPrefix;
    private LocalDateTime updatedAt;
}
