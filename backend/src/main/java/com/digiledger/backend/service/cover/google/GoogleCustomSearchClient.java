package com.digiledger.backend.service.cover.google;

import com.digiledger.backend.config.GoogleCustomSearchProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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
 * 封装 Google Custom Search 调用。
 */
@Component
public class GoogleCustomSearchClient {

    private static final Logger log = LoggerFactory.getLogger(GoogleCustomSearchClient.class);

    private final GoogleCustomSearchProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GoogleCustomSearchClient(GoogleCustomSearchProperties properties,
                                    RestTemplateBuilder builder,
                                    ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .defaultHeader(HttpHeaders.USER_AGENT, "DigiLedger/cover-suggest")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    public List<GoogleImageResult> searchImages(String query, int limit) {
        int finalCount = Math.min(Math.max(limit, 1), Math.max(1, properties.getMaxResults()));
        URI uri = UriComponentsBuilder.fromHttpUrl(properties.getEndpoint())
                .queryParam("key", properties.getApiKey())
                .queryParam("cx", properties.getCseId())
                .queryParam("q", query)
                .queryParam("searchType", "image")
                .queryParam("num", finalCount)
                .queryParam("safe", properties.getSafe())
                .build(true)
                .toUri();
        try {
            ResponseEntity<String> response = restTemplate.getForEntity(uri, String.class);
            return parseResponse(query, response.getBody());
        } catch (RestClientException ex) {
            log.error("调用 Google 图片搜索失败，关键词：{}，错误：{}", query, ex.getMessage());
            return List.of();
        }
    }

    private List<GoogleImageResult> parseResponse(String query, String body) {
        if (!StringUtils.hasText(body)) {
            return List.of();
        }
        List<GoogleImageResult> results = new ArrayList<>();
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode items = root.path("items");
            if (!items.isArray()) {
                return List.of();
            }
            for (JsonNode item : items) {
                String originalUrl = item.path("link").asText(null);
                if (!StringUtils.hasText(originalUrl)) {
                    continue;
                }
                JsonNode imageNode = item.path("image");
                String thumbnailUrl = imageNode.path("thumbnailLink").asText(null);
                if (!StringUtils.hasText(thumbnailUrl)) {
                    thumbnailUrl = originalUrl;
                }
                String contextLink = imageNode.path("contextLink").asText(null);
                String title = item.path("title").asText("");
                String mime = item.path("mime").asText(null);
                results.add(new GoogleImageResult(thumbnailUrl, originalUrl, contextLink, title, mime));
            }
        } catch (Exception ex) {
            log.error("解析 Google 图片搜索结果失败，关键词：{}，错误：{}", query, ex.getMessage());
            return List.of();
        }
        return results;
    }

    public record GoogleImageResult(String thumbnailUrl,
                                    String originalUrl,
                                    String contextLink,
                                    String title,
                                    String mime) {
    }
}
