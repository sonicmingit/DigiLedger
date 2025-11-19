package com.digiledger.backend.service.cover.google;

import com.digiledger.backend.config.GoogleCustomSearchProperties;
import com.digiledger.backend.model.cover.CoverCandidate;
import com.digiledger.backend.service.cover.ImageSearchProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Google 图片搜索实现，作为主要图片来源。
 */
@Component
@Order(20)
public class GoogleImageSearchProvider implements ImageSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(GoogleImageSearchProvider.class);
    private static final String SOURCE = "GOOGLE_IMAGE_SEARCH";

    private final GoogleCustomSearchClient client;
    private final GoogleCustomSearchProperties properties;

    public GoogleImageSearchProvider(GoogleCustomSearchClient client,
                                     GoogleCustomSearchProperties properties) {
        this.client = client;
        this.properties = properties;
    }

    @Override
    public String getName() {
        return SOURCE;
    }

    @Override
    public String getDisplayName() {
        return "Google 图片搜索";
    }

    @Override
    public String getDescription() {
        return "Google CSE，覆盖更全面的图像结果";
    }

    @Override
    public List<CoverCandidate> search(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        if (!properties.isEnabled()) {
            log.warn("Google 图片搜索已被禁用，跳过查询");
            return List.of();
        }
        if (!StringUtils.hasText(properties.getApiKey()) || !StringUtils.hasText(properties.getCseId())) {
            log.warn("Google 图片搜索缺少 API Key 或 CSE ID，跳过查询");
            return List.of();
        }
        int target = limit > 0 ? Math.min(limit, properties.getMaxResults()) : properties.getMaxResults();
        List<GoogleCustomSearchClient.GoogleImageResult> results = client.searchImages(query, target);
        if (CollectionUtils.isEmpty(results)) {
            return List.of();
        }
        List<CoverCandidate> candidates = new ArrayList<>();
        for (GoogleCustomSearchClient.GoogleImageResult result : results) {
            Map<String, Object> extra = new HashMap<>();
            extra.put("sourceLabel", "Google 图片搜索");
            extra.put("pageUrl", result.contextLink());
            extra.put("mime", result.mime());
            String thumbnailUrl = StringUtils.hasText(result.thumbnailUrl()) ? result.thumbnailUrl() : result.originalUrl();
            candidates.add(new CoverCandidate(
                    thumbnailUrl,
                    result.originalUrl(),
                    SOURCE,
                    result.title(),
                    extra
            ));
        }
        return candidates;
    }
}
