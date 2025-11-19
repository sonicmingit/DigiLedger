package com.digiledger.backend.model.dto.setting;

import java.util.List;

/**
 * 智能找图服务列表响应
 */
public record ImageSearchProvidersResponse(List<ImageSearchProviderDTO> providers, String defaultProvider) {
}
