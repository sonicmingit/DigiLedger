package com.digiledger.backend.integration.externalapi.service;

import com.digiledger.backend.integration.externalapi.dto.ExternalApiConfigRequest;
import com.digiledger.backend.integration.externalapi.dto.ExternalApiConfigResponse;
import com.digiledger.backend.integration.externalapi.model.ExternalApiConfig;

import java.util.List;

public interface ExternalApiConfigService {
    List<ExternalApiConfigResponse> list();
    ExternalApiConfigResponse get(String apiCode);
    ExternalApiConfigResponse save(String apiCode, ExternalApiConfigRequest request);
    ExternalApiConfig requireEnabled(String apiCode);
    ExternalApiConfig requireConfiguredEnabled(String apiCode);
}
