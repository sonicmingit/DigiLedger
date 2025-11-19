package com.digiledger.backend.service.cover.bing;

import com.digiledger.backend.config.BingImageSearchProperties;
import com.digiledger.backend.model.cover.CoverCandidate;
import com.digiledger.backend.service.cover.ImageSearchProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 京东
 * 使用 Bing 图片搜索的京东站内结果作为最高优先级的封面候选来源。
 */
@Component
@Order(10)
public class JdBingImageSearchProvider implements ImageSearchProvider {

    private static final Logger log = LoggerFactory.getLogger(JdBingImageSearchProvider.class);
    private static final String SOURCE = "BING_JD_IMAGE_SEARCH";
    private static final String ENDPOINT = "https://cn.bing.com/images/search";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final int defaultCount;
    private final int maxCount;

    public JdBingImageSearchProvider(RestTemplateBuilder builder,
                                     ObjectMapper objectMapper,
                                     BingImageSearchProperties properties) {
        this.objectMapper = objectMapper;
        this.defaultCount = properties.getDefaultCount();
        this.maxCount = properties.getMaxCount();
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .defaultHeader(HttpHeaders.USER_AGENT, "DigiLedger/cover-suggest")
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.TEXT_HTML_VALUE)
                .build();
    }

    @Override
    public String getName() {
        return SOURCE;
    }

    @Override
    public String getDisplayName() {
        return "Bing 京东搜图";
    }

    @Override
    public String getDescription() {
        return "搜索京东商城，适合国内电商封面";
    }

    @Override
    public List<CoverCandidate> search(String query, int limit) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }
        int target = limit > 0 ? Math.min(limit, maxCount) : defaultCount;
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        log.debug("Bing 京东图源查询编码：{}", encodedQuery);
        String queryWithSite = encodedQuery + "+site:jd.com";
        URI uri = UriComponentsBuilder.fromHttpUrl(ENDPOINT)
                .queryParam("q", queryWithSite)
                .queryParam("first", 1)
                .queryParam("count", target)
                //.queryParam("form", "IRFLTR")
                .build(true)
                .toUri();
        log.debug("Bing 京东图源请求：{}", uri);
        try {
            String body = restTemplate.getForObject(uri, String.class);
            return parseHtml(body, target);
        } catch (RestClientException ex) {
            log.warn("无法查询 Bing 京东图源：{}", ex.getMessage());
            return List.of();
        }
    }

    private List<CoverCandidate> parseHtml(String body, int limit) {
        if (!StringUtils.hasText(body)) {
            return List.of();
        }
        Document doc = Jsoup.parse(body);
        Elements items = doc.select("a.iusc");
        if (CollectionUtils.isEmpty(items)) {
            return List.of();
        }
        List<CoverCandidate> candidates = new ArrayList<>();
        for (Element item : items) {
            if (candidates.size() >= limit) {
                break;
            }
            String meta = item.attr("m");
            if (!StringUtils.hasText(meta)) {
                continue;
            }
            try {
                JsonNode node = objectMapper.readTree(meta);
                String originalUrl = node.path("murl").asText(null);
                String thumbnailUrl = node.path("turl").asText(null);
                if (!StringUtils.hasText(originalUrl) || !StringUtils.hasText(thumbnailUrl)) {
                    continue;
                }
                String pageUrl = node.path("purl").asText(null);
                String title = node.path("pt").asText("");
                Map<String, Object> extra = new HashMap<>();
                extra.put("sourceLabel", "Bing 京东搜索");
                if (StringUtils.hasText(pageUrl)) {
                    extra.put("pageUrl", pageUrl);
                }
                candidates.add(new CoverCandidate(
                        thumbnailUrl,
                        originalUrl,
                        SOURCE,
                        title,
                        extra
                ));
            } catch (Exception ex) {
                log.debug("解析 Bing 京东图源结果失败：{}", ex.getMessage());
            }
        }
        return candidates;
    }
}
