package com.digiledger.backend.openapi;

import com.digiledger.backend.common.ApiResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/settings/open-api")
public class OpenApiSettingsController {
    private final OpenApiAccessService access;
    public OpenApiSettingsController(OpenApiAccessService access) { this.access = access; }
    public record Settings(boolean enabled, boolean allowWrite, boolean tokenConfigured, String tokenPrefix, LocalDateTime updatedAt) {}
    public record Update(@NotNull Boolean enabled, @NotNull Boolean allowWrite) {}
    public record Token(String token) {}

    @GetMapping public ApiResponse<Settings> get() {
        OpenApiAccess a = access.get();
        return ApiResponse.success(a == null ? new Settings(false, false, false, null, null)
                : new Settings(a.isEnabled(), a.isAllowWrite(), a.getTokenHash() != null, a.getTokenPrefix(), a.getUpdatedAt()));
    }
    @PutMapping public ApiResponse<Void> update(@RequestBody @Valid Update update) {
        access.update(update.enabled(), update.allowWrite());
        return ApiResponse.success();
    }
    @PostMapping("/token") public ResponseEntity<ApiResponse<Token>> rotate() {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(ApiResponse.success(new Token(access.rotate())));
    }
}
