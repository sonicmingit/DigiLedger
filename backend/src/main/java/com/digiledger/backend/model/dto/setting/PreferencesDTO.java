package com.digiledger.backend.model.dto.setting;

public record PreferencesDTO(String currency, String dateFormat, boolean autoBackupEnabled, String autoBackupTime) { }
