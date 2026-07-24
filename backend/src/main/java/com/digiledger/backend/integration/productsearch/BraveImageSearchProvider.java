package com.digiledger.backend.integration.productsearch;

import com.digiledger.backend.integration.cover.ImageSearchProvider;
import com.digiledger.backend.integration.externalapi.model.ExternalApiConfig;
import com.digiledger.backend.integration.externalapi.service.ExternalApiConfigService;
import com.digiledger.backend.integration.externalapi.service.ExternalApiConfigServiceImpl;
import com.digiledger.backend.model.cover.CoverCandidate;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import java.time.Duration; import java.util.*;

@Component @Order(40)
public class BraveImageSearchProvider implements ImageSearchProvider {
  private final ExternalApiConfigService configs; private final RestTemplateBuilder builder; private final ObjectMapper json;
  public BraveImageSearchProvider(ExternalApiConfigService configs, RestTemplateBuilder builder, ObjectMapper json){this.configs=configs;this.builder=builder;this.json=json;}
  public String getName(){return ExternalApiConfigServiceImpl.BRAVE_IMAGE_SEARCH;} public String getDisplayName(){return "Brave 图片搜索";} public String getDescription(){return "稳定的独立图片搜索 API，适合作为通用封面兜底。";}
  public List<CoverCandidate> search(String query,int limit){ try { ExternalApiConfig c=configs.requireConfiguredEnabled(getName()); int n=Math.min(Math.max(limit,1),20); String body=client(c).getForObject(UriComponentsBuilder.fromHttpUrl(c.getBaseUrl()).queryParam("q",query).queryParam("count",n).queryParam("country",str(c,"country","CN")).queryParam("search_lang",str(c,"searchLang","zh-hans")).queryParam("safesearch",str(c,"safeSearch","strict")).build().encode().toUri(),String.class); List<CoverCandidate> out=new ArrayList<>(); for(JsonNode x:json.readTree(body).path("results")){String thumb=x.path("thumbnail").path("src").asText();String original=x.path("properties").path("url").asText();if(!thumb.isBlank()&&!original.isBlank())out.add(new CoverCandidate(thumb,original,getName(),x.path("title").asText(""),Map.of("pageUrl",x.path("url").asText(""),"sourceLabel","Brave Image Search")));}return out;}catch(Exception e){throw new IllegalStateException("Brave 图片搜索失败："+e.getMessage(),e);} }
  private RestTemplate client(ExternalApiConfig c){int t=c.getTimeoutMs()==null?15000:c.getTimeoutMs();return builder.setConnectTimeout(Duration.ofMillis(t)).setReadTimeout(Duration.ofMillis(t)).defaultHeader("X-Subscription-Token",c.getCredentialSecret()).defaultHeader(HttpHeaders.ACCEPT,"application/json").build();} private String str(ExternalApiConfig c,String k,String d){try{String v=json.readTree(c.getConfigJson()).path(k).asText();return v.isBlank()?d:v;}catch(Exception e){return d;}}
}
