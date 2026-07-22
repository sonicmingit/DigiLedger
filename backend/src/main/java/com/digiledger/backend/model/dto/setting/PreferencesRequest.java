package com.digiledger.backend.model.dto.setting;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record PreferencesRequest(
        @NotBlank @Size(max=10) String currency,
        @NotBlank @Size(max=32) String dateFormat,
        @NotNull Boolean autoBackupEnabled,
        @NotBlank @Pattern(regexp="(?:[01]\\d|2[0-3]):[0-5]\\d") String autoBackupTime) { }
