package com.digiledger.backend.service.cover.bing;

import com.digiledger.backend.config.BingImageSearchProperties;
import com.digiledger.backend.model.cover.CoverCandidate;
import com.digiledger.backend.service.cover.ImageSearchProvider;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 Bing 的图片搜索实现。
 */
/*@Component
@Order(100)*/
public class BingImageSearchProvider implements ImageSearchProvider {

    private final BingImageSearchClient client;
    private final BingImageSearchProperties properties;

    public BingImageSearchProvider(BingImageSearchClient client,
                                   BingImageSearchProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public String getName() {
        return "BING_IMAGE_SEARCH";
    }

    @Override
    public String getDisplayName() {
        return "Bing 图片搜索";
    }

    @Override
    public String getDescription() {
        return "微软 Bing 通用图片搜索";
    }

    @Override
    public List<CoverCandidate> search(String query, int limit) {
        int target = limit > 0 ? limit : properties.getDefaultCount();
        List<BingImageSearchClient.BingImageResult> results = client.searchImages(query, target);
        if (CollectionUtils.isEmpty(results)) {
            return List.of();
        }
        List<CoverCandidate> candidates = new ArrayList<>();
        for (BingImageSearchClient.BingImageResult result : results) {
            Map<String, Object> extra = new HashMap<>();
            extra.put("sourceLabel", "Bing 图片搜索");
            extra.put("pageUrl", result.pageUrl());
            candidates.add(new CoverCandidate(
                    result.thumbnailUrl(),
                    result.originalUrl(),
                    getName(),
                    result.title(),
                    extra
            ));
        }
        return candidates;
    }
}
