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

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 淘宝联盟物料搜索：taobao.tbk.dg.material.optional。 */
@Component
@Order(20)
public class TaobaoUnionProductSearchProvider implements com.digiledger.backend.integration.cover.ImageSearchProvider {
    private static final DateTimeFormatter TIMESTAMP = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final ExternalApiConfigService configs; private final RestTemplateBuilder restTemplateBuilder; private final ObjectMapper objectMapper;
    public TaobaoUnionProductSearchProvider(ExternalApiConfigService configs, RestTemplateBuilder restTemplateBuilder, ObjectMapper objectMapper){this.configs=configs;this.restTemplateBuilder=restTemplateBuilder;this.objectMapper=objectMapper;}
    @Override public String getName(){return "TAOBAO_UNION_PRODUCT_SEARCH";} @Override public String getDisplayName(){return "淘宝联盟商品搜索";} @Override public String getDescription(){return "通过淘宝联盟正式物料接口查询并返回商品主图。";}
    @Override public List<CoverCandidate> search(String query,int limit){ExternalApiConfig c=configs.requireConfiguredEnabled(getName());Map<String,String>x=config(c);String appKey=required(x,"appKey","请在扩展配置 JSON 中填写 appKey");String adzoneId=required(x,"adzoneId","请在扩展配置 JSON 中填写 adzoneId");Map<String,String> p=new LinkedHashMap<>();p.put("method","taobao.tbk.dg.material.optional");p.put("app_key",appKey);p.put("sign_method","hmac");p.put("timestamp",LocalDateTime.now(ZoneId.of("Asia/Shanghai")).format(TIMESTAMP));p.put("format","json");p.put("v","2.0");p.put("adzone_id",adzoneId);p.put("q",query.trim());p.put("page_no","1");p.put("page_size",String.valueOf(Math.min(Math.max(limit,1),100)));p.put("sign",hmacSign(p,c.getCredentialSecret()));MultiValueMap<String,String> form=new LinkedMultiValueMap<>();p.forEach(form::add);String body=client(c).postForObject(c.getBaseUrl(),new HttpEntity<>(form,headers()),String.class);return parse(body);}
    private List<CoverCandidate> parse(String body){try{JsonNode items=objectMapper.readTree(body).path("tbk_dg_material_optional_response").path("result_list").path("map_data");List<CoverCandidate> out=new ArrayList<>();for(JsonNode item:items){String image=item.path("pict_url").asText();if(!StringUtils.hasText(image))continue;String title=item.path("title").asText("淘宝商品");String page=item.path("item_url").asText();Map<String,Object> meta=new LinkedHashMap<>();meta.put("sourceLabel",getDisplayName());if(StringUtils.hasText(page))meta.put("pageUrl",page);out.add(new CoverCandidate(image,image,getName(),title,meta));}return out;}catch(Exception ex){throw new IllegalStateException("解析淘宝联盟商品结果失败",ex);}}
    private RestTemplate client(ExternalApiConfig c){int t=c.getTimeoutMs()==null?15000:c.getTimeoutMs();return restTemplateBuilder.setConnectTimeout(Duration.ofMillis(t)).setReadTimeout(Duration.ofMillis(t)).build();} private static HttpHeaders headers(){HttpHeaders h=new HttpHeaders();h.setContentType(MediaType.APPLICATION_FORM_URLENCODED);return h;} private Map<String,String> config(ExternalApiConfig c){try{return objectMapper.convertValue(objectMapper.readTree(c.getConfigJson()),Map.class);}catch(Exception e){return Map.of();}} private static String required(Map<String,String>x,String key,String message){String value=x.get(key);if(!StringUtils.hasText(value))throw new IllegalStateException(message);return value.trim();} private static String hmacSign(Map<String,String> p,String secret){try{StringBuilder raw=new StringBuilder();p.entrySet().stream().sorted(Map.Entry.comparingByKey()).forEach(e->raw.append(e.getKey()).append(e.getValue()));Mac mac=Mac.getInstance("HmacMD5");mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8),"HmacMD5"));byte[] digest=mac.doFinal(raw.toString().getBytes(StandardCharsets.UTF_8));StringBuilder hex=new StringBuilder();for(byte b:digest)hex.append(String.format("%02X",b));return hex.toString();}catch(Exception e){throw new IllegalStateException("生成淘宝联盟签名失败",e);}}
}
