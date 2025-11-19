package com.digiledger.backend.model.dto.setting;

import com.digiledger.backend.service.cover.ImageSearchProvider;

/**
 * 智能找图服务描述
 */
public record ImageSearchProviderDTO(String name, String displayName, String description) {

    public static ImageSearchProviderDTO fromProvider(ImageSearchProvider provider) {
        return new ImageSearchProviderDTO(
                provider.getName(),
                provider.getDisplayName(),
                provider.getDescription()
        );
    }
}
