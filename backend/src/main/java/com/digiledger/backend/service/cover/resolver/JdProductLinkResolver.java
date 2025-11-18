package com.digiledger.backend.service.cover.resolver;

import com.digiledger.backend.model.cover.ProductInfo;
import com.digiledger.backend.service.cover.ProductLinkResolver;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.RequestEntity;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.time.Duration;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 京东商品链接解析，后续可扩展更多平台。
 */
@Component
public class JdProductLinkResolver implements ProductLinkResolver {

    private static final Logger log = LoggerFactory.getLogger(JdProductLinkResolver.class);
private static final Pattern IMAGE_PATTERN = Pattern.compile("(https?:)?//img\\d+\\.360buyimg\\.com/[^\"']+?(?:jpg|png|jpeg)", Pattern.CASE_INSENSITIVE);

    private final RestTemplate restTemplate;

    public JdProductLinkResolver(RestTemplateBuilder builder) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(5))
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.4 Safari/605.1.15")
                .build();
    }

    @Override
    public boolean supports(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost();
            return host != null && host.contains("jd.com");
        } catch (Exception ex) {
            return false;
        }
    }

    @Override
    public Optional<ProductInfo> resolve(String url) {
        if (!supports(url)) {
            return Optional.empty();
        }
        try {
            URI uri = URI.create(url);
            RequestEntity<Void> request = RequestEntity.get(uri).build();
            ResponseEntity<String> response = restTemplate.exchange(request, String.class);
            String body = response.getBody();
            if (!StringUtils.hasText(body)) {
                return Optional.empty();
            }
            Document document = Jsoup.parse(body);
            String title = document.title();
            String imageUrl = extractImage(document, body);
            if (!StringUtils.hasText(imageUrl)) {
                return Optional.empty();
            }
            return Optional.of(new ProductInfo(title, ensureHttps(imageUrl)));
        } catch (Exception ex) {
            log.warn("解析京东商品链接失败：{}", ex.getMessage());
            return Optional.empty();
        }
    }

    private String extractImage(Document document, String body) {
        Element ogImage = document.selectFirst("meta[property=og:image]");
        if (ogImage != null) {
            String content = ogImage.attr("content");
            if (StringUtils.hasText(content)) {
                return content;
            }
        }
        Element specImg = document.getElementById("spec-img");
        if (specImg != null) {
            String dataUrl = specImg.attr("data-origin");
            if (StringUtils.hasText(dataUrl)) {
                return dataUrl;
            }
            String src = specImg.attr("src");
            if (StringUtils.hasText(src)) {
                return src;
            }
        }
        Matcher matcher = IMAGE_PATTERN.matcher(body);
        if (matcher.find()) {
            return matcher.group();
        }
        return null;
    }

    private String ensureHttps(String url) {
        if (!StringUtils.hasText(url)) {
            return url;
        }
        String trimmed = url.trim();
        if (trimmed.startsWith("//")) {
            return "https:" + trimmed;
        }
        if (trimmed.startsWith("http")) {
            return trimmed;
        }
        return "https://" + trimmed;
    }

    @Override
    public String getSource() {
        return "PURCHASE_LINK_JD";
    }

    @Override
    public String getDisplayName() {
        return "购买链接 · 京东";
    }
}
