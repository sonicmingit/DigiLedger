package com.digiledger.backend.service.cover.bing;

import com.digiledger.backend.config.BingImageSearchProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 封装 Bing 图片搜索 API 调用。
 */
@Component
public class BingImageSearchClient {

    private static final Logger log = LoggerFactory.getLogger(BingImageSearchClient.class);

    private final BingImageSearchProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public BingImageSearchClient(BingImageSearchProperties properties,
                                 RestTemplateBuilder builder,
                                 ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .defaultHeader(HttpHeaders.USER_AGENT, "DigiLedger/cover-suggest")
                .build();
    }

    public List<BingImageResult> searchImages(String query, int count) {
        if (!StringUtils.hasText(properties.getApiKey())) {
            log.warn("未配置 Bing API Key，跳过图片搜索");
            return List.of();
        }
        int finalCount = Math.min(Math.max(count, 1), properties.getMaxCount());
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getEndpoint())
                .queryParam("q", query)
                .queryParam("count", finalCount)
                .queryParam("safeSearch", properties.getSafeSearch())
                .queryParam("imageType", "Photo")
                .queryParam("mkt", properties.getMarket())
                .build(true)
                .toUri();
        HttpHeaders headers = new HttpHeaders();
        headers.add("Ocp-Apim-Subscription-Key", properties.getApiKey());
        RequestEntity<Void> request = RequestEntity.get(uri).headers(headers).build();
        try {
            ResponseEntity<String> response = restTemplate.exchange(request, String.class);
            return parseResponse(response.getBody());
        } catch (RestClientException ex) {
            log.warn("调用 Bing 图片搜索失败：{}", ex.getMessage());
            return List.of();
        }
    }

    private List<BingImageResult> parseResponse(String body) {
        if (!StringUtils.hasText(body)) {
            return List.of();
        }
        List<BingImageResult> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode value = root.path("value");
            if (!value.isArray()) {
                return List.of();
            }
            for (JsonNode node : value) {
                String originalUrl = node.path("contentUrl").asText(null);
                String thumbUrl = node.path("thumbnailUrl").asText(null);
                if (!StringUtils.hasText(originalUrl) || !StringUtils.hasText(thumbUrl)) {
                    continue;
                }
                String name = node.path("name").asText("");
                String pageUrl = node.path("hostPageDisplayUrl").asText(null);
                results.add(new BingImageResult(thumbUrl, originalUrl, pageUrl, name));
            }
        } catch (Exception ex) {
            log.warn("解析 Bing 响应失败：{}", ex.getMessage());
            return List.of();
        }
        return results;
    }

    public record BingImageResult(String thumbnailUrl, String originalUrl, String pageUrl, String title) {
    }
}
