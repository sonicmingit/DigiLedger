package com.digiledger.backend.integration.productsearch;

import com.digiledger.backend.integration.cover.ImageSearchProvider;
import com.digiledger.backend.integration.externalapi.model.ExternalApiConfig;
import com.digiledger.backend.integration.externalapi.service.ExternalApiConfigService;
import com.digiledger.backend.model.cover.CoverCandidate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@Order(40)
public class SerpApiGoogleImagesProvider extends SerpApiImageSearchSupport implements ImageSearchProvider {
    private final ExternalApiConfigService configs;
    public SerpApiGoogleImagesProvider(ExternalApiConfigService configs, RestTemplateBuilder builder, ObjectMapper json) { super(builder, json); this.configs = configs; }
    @Override public String getName() { return "SERPAPI_GOOGLE_IMAGES"; }
    @Override public String getDisplayName() { return "SerpApi Google 商品图片搜索"; }
    @Override public String getDescription() { return "Google 图片搜索的结构化 API；按来源域名严格过滤商品图。"; }
    @Override public List<CoverCandidate> search(String query, int limit) {
        ExternalApiConfig config = configs.requireConfiguredEnabled(getName()); Map<String, Object> extra = config(config); List<String> domains = domains(extra);
        String scoped = googleScopedQuery(query, domains);
        Map<String, String> params = new LinkedHashMap<>(); params.put("engine", "google_images"); params.put("q", scoped); params.put("api_key", config.getCredentialSecret()); params.put("ijn", "0");
        params.put("gl", String.valueOf(extra.getOrDefault("gl", "cn"))); params.put("hl", String.valueOf(extra.getOrDefault("hl", "zh-CN")));
        return googleImages(request(config, params), getName(), getDisplayName(), domains, Math.min(Math.max(limit, 1), 50));
    }
}
