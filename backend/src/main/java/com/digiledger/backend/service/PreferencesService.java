package com.digiledger.backend.service;
import com.digiledger.backend.model.dto.setting.*;
public interface PreferencesService { PreferencesDTO get(); void update(PreferencesRequest request); }
