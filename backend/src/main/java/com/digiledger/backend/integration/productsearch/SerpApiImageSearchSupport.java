package com.digiledger.backend.integration.productsearch;

import com.digiledger.backend.integration.externalapi.model.ExternalApiConfig;
import com.digiledger.backend.model.cover.CoverCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URL;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

abstract class SerpApiImageSearchSupport {
    protected final RestTemplateBuilder builder;
    protected final ObjectMapper json;

    protected SerpApiImageSearchSupport(RestTemplateBuilder builder, ObjectMapper json) { this.builder = builder; this.json = json; }

    protected Map<String, Object> config(ExternalApiConfig config) {
        try {
            JsonNode node = json.readTree(config.getConfigJson());
            return node == null || node.isNull() ? Map.of() : json.convertValue(node, Map.class);
        }
        catch (Exception ignored) { return Map.of(); }
    }

    protected List<String> domains(Map<String, Object> extra) {
        Object value = extra.get("domains");
        if (!(value instanceof List<?> list)) return List.of();
        return list.stream().filter(String.class::isInstance).map(String.class::cast).map(String::trim).filter(StringUtils::hasText).toList();
    }

    protected String googleScopedQuery(String query, List<String> domains) {
        if (domains.isEmpty()) return query.trim();
        return query.trim() + " (" + domains.stream().map(domain -> "site:" + domain).reduce((left, right) -> left + " OR " + right).orElse("") + ")";
    }

    protected String request(ExternalApiConfig config, Map<String, String> params) {
        UriComponentsBuilder uri = UriComponentsBuilder.fromHttpUrl(config.getBaseUrl());
        params.forEach(uri::queryParam);
        int timeout = config.getTimeoutMs() == null ? 30000 : config.getTimeoutMs();
        RestTemplate client = builder.setConnectTimeout(Duration.ofMillis(timeout)).setReadTimeout(Duration.ofMillis(timeout)).build();
        return client.exchange(RequestEntity.get(uri.build().encode().toUri()).header(HttpHeaders.ACCEPT, "application/json").build(), String.class).getBody();
    }

    protected boolean allowedSource(String pageUrl, List<String> domains) {
        if (domains.isEmpty()) return true;
        try {
            String host = new URL(pageUrl).getHost().toLowerCase();
            return domains.stream().anyMatch(domain -> host.equals(domain) || host.endsWith("." + domain));
        } catch (Exception ignored) { return false; }
    }

    protected List<CoverCandidate> googleImages(String body, String provider, String label, List<String> domains, int limit) {
        try {
            List<CoverCandidate> results = new ArrayList<>();
            for (JsonNode item : json.readTree(body).path("images_results")) {
                String thumbnail = item.path("thumbnail").asText(); String original = item.path("original").asText(); String page = item.path("link").asText();
                if (!StringUtils.hasText(thumbnail) || !StringUtils.hasText(original) || !allowedSource(page, domains)) continue;
                results.add(new CoverCandidate(thumbnail, original, provider, item.path("title").asText("商品图片"), Map.of("sourceLabel", label, "pageUrl", page)));
                if (results.size() >= limit) break;
            }
            return results;
        } catch (Exception ex) { throw new IllegalStateException("解析 SerpApi Google 图片结果失败", ex); }
    }

}
