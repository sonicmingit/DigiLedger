package com.digiledger.backend.model.dto.setting;

import com.digiledger.backend.integration.cover.ImageSearchProvider;

/**
 * 智能找图服务描述
 */
public record ImageSearchProviderDTO(String name, String displayName, String description, boolean available) {

    public static ImageSearchProviderDTO fromProvider(ImageSearchProvider provider, boolean available) {
        return new ImageSearchProviderDTO(
                provider.getName(),
                provider.getDisplayName(),
                provider.getDescription(),
                available
        );
    }
}
