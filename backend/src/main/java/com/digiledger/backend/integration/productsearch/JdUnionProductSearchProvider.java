package com.digiledger.backend.integration.productsearch;

import com.digiledger.backend.integration.externalapi.model.ExternalApiConfig;
import com.digiledger.backend.integration.externalapi.service.ExternalApiConfigService;
import com.digiledger.backend.model.cover.CoverCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 京东联盟关键词商品查询：jd.union.open.goods.query。 */
@Component
@Order(10)
public class JdUnionProductSearchProvider implements com.digiledger.backend.integration.cover.ImageSearchProvider {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ExternalApiConfigService configs;
    private final RestTemplateBuilder restTemplateBuilder;
    private final ObjectMapper objectMapper;

    public JdUnionProductSearchProvider(ExternalApiConfigService configs, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper) {
        this.configs = configs;
        this.restTemplateBuilder = restTemplateBuilder;
        this.objectMapper = objectMapper;
    }
    @Override public String getName() { return "JD_UNION_PRODUCT_SEARCH"; }
    @Override public String getDisplayName() { return "京东联盟商品搜索"; }
    @Override public String getDescription() { return "通过京东联盟正式商品接口查询并返回商品主图。"; }

    @Override
    public List<CoverCandidate> search(String query, int limit) {
        ExternalApiConfig config = configs.requireConfiguredEnabled(getName());
        Map<String, String> extra = config(config);
        String appKey = required(extra, "appKey", "请在扩展配置 JSON 中填写 appKey");
        int count = Math.min(Math.max(limit, 1), 20);
        Map<String, Object> goodsRequest = Map.of("goodsReqDTO", Map.of("keyword", query.trim(), "pageIndex", 1, "pageSize", count));
        String payload;
        try { payload = objectMapper.writeValueAsString(goodsRequest); }
        catch (Exception ex) { throw new IllegalStateException("无法生成京东联盟请求参数", ex); }
        Map<String, String> params = new LinkedHashMap<>();
        params.put("method", "jd.union.open.goods.query"); params.put("app_key", appKey);
        params.put("timestamp", LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(TIMESTAMP));
        params.put("format", "json"); params.put("v", "1.0"); params.put("sign_method", "md5");
        params.put("360buy_param_json", payload); params.put("sign", md5Sign(params, config.getCredentialSecret()));
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>(); params.forEach(form::add);
        String body = client(config).postForObject(config.getBaseUrl(), new HttpEntity<>(form, formHeaders()), String.class);
        return parse(body);
    }

    private List<CoverCandidate> parse(String body) {
        try {
            JsonNode root = objectMapper.readTree(body);
            JsonNode result = root.path("jd_union_open_goods_query_responce");
            JsonNode data = result.path("queryResult");
            if (data.isTextual()) data = objectMapper.readTree(data.asText());
            if (data.isMissingNode()) data = result;
            List<CoverCandidate> candidates = new ArrayList<>();
            for (JsonNode item : data.path("data")) {
                String image = item.path("imageInfo").path("imageList").path(0).path("url").asText();
                if (!StringUtils.hasText(image)) continue;
                String title = item.path("skuName").asText("京东商品");
                String pageUrl = item.path("materialUrl").asText();
                Map<String, Object> metadata = new LinkedHashMap<>(); metadata.put("sourceLabel", getDisplayName());
                if (StringUtils.hasText(pageUrl)) metadata.put("pageUrl", pageUrl);
                candidates.add(new CoverCandidate(image, image, getName(), title, metadata));
            }
            return candidates;
        } catch (Exception ex) { throw new IllegalStateException("解析京东联盟商品结果失败", ex); }
    }
    private RestTemplate client(ExternalApiConfig c) { int t=c.getTimeoutMs()==null?15000:c.getTimeoutMs(); return restTemplateBuilder.setConnectTimeout(Duration.ofMillis(t)).setReadTimeout(Duration.ofMillis(t)).build(); }
    private static HttpHeaders formHeaders(){ HttpHeaders headers=new HttpHeaders(); headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED); return headers; }
    private Map<String,String> config(ExternalApiConfig c){ try{return objectMapper.convertValue(objectMapper.readTree(c.getConfigJson()), Map.class);}catch(Exception ex){return Map.of();} }
    private static String required(Map<String,String> values,String key,String message){String value=values.get(key);if(!StringUtils.hasText(value))throw new IllegalStateException(message);return value.trim();}
    private static String md5Sign(Map<String,String> values,String secret){try{StringBuilder raw=new StringBuilder(secret);values.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->raw.append(e.getKey()).append(e.getValue()));raw.append(secret);byte[] digest=MessageDigest.getInstance("MD5").digest(raw.toString().getBytes(StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format("%02X",b));return hex.toString();}catch(Exception ex){throw new IllegalStateException("生成京东联盟签名失败",ex);}}
}
