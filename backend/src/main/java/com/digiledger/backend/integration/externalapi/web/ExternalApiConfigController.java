package com.digiledger.backend.integration.externalapi.web;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.integration.externalapi.dto.ExternalApiConfigRequest;
import com.digiledger.backend.integration.externalapi.dto.ExternalApiConfigResponse;
import com.digiledger.backend.integration.externalapi.dto.ExternalApiTestItem;
import com.digiledger.backend.integration.externalapi.dto.ExternalApiTestRequest;
import com.digiledger.backend.integration.externalapi.dto.ExternalApiTestResponse;
import com.digiledger.backend.integration.cover.ImageSearchProvider;
import com.digiledger.backend.model.cover.CoverCandidate;
import com.digiledger.backend.integration.mtphotos.MtPhotosSearchRequest;
import com.digiledger.backend.integration.mtphotos.MtPhotosSearchResponse;
import com.digiledger.backend.integration.mtphotos.MtPhotosService;
import com.digiledger.backend.integration.mtphotos.MtPhotosThumbnail;
import com.digiledger.backend.integration.externalapi.service.ExternalApiConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/api/external-api-configs")
public class ExternalApiConfigController {
    private final ExternalApiConfigService configService;
    private final MtPhotosService mtPhotosService;
    private final List<ImageSearchProvider> imageSearchProviders;

    public ExternalApiConfigController(ExternalApiConfigService configService,
                                       MtPhotosService mtPhotosService, List<ImageSearchProvider> imageSearchProviders) {
        this.configService = configService;
        this.mtPhotosService = mtPhotosService;
        this.imageSearchProviders = imageSearchProviders;
    }

    @GetMapping
    public ApiResponse<List<ExternalApiConfigResponse>> list() {
        return ApiResponse.success(configService.list());
    }

    @GetMapping("/{apiCode}")
    public ApiResponse<ExternalApiConfigResponse> get(@PathVariable("apiCode") String apiCode) {
        return ApiResponse.success(configService.get(apiCode));
    }

    @PutMapping("/{apiCode}")
    public ApiResponse<ExternalApiConfigResponse> save(@PathVariable("apiCode") String apiCode, @RequestBody @Valid ExternalApiConfigRequest request) {
        return ApiResponse.success(configService.save(apiCode, request));
    }

    @PostMapping("/MT_PHOTOS/test-search")
    public ApiResponse<MtPhotosSearchResponse> testMtPhotosSearch(@RequestBody @Valid MtPhotosSearchRequest request) {
        return ApiResponse.success(mtPhotosService.testSearch(request));
    }

    @PostMapping("/{apiCode}/test")
    public ApiResponse<ExternalApiTestResponse> test(@PathVariable("apiCode") String apiCode,
                                                      @RequestBody(required = false) ExternalApiTestRequest request) {
        String code = apiCode.trim().toUpperCase();
        String query = request == null || request.query() == null || request.query().isBlank() ? "数码产品" : request.query().trim();
        return ApiResponse.success(switch (code) {
            case "REMOVE_BG" -> {
                configService.requireConfiguredEnabled(code);
                yield new ExternalApiTestResponse(code, true,
                        "凭据与服务地址已保存。remove.bg 不提供无消耗的连通性接口，请在实际抠图时验证。", 0, List.of());
            }
            default -> testProvider(code, query);
        });
    }
    private ExternalApiTestResponse testProvider(String code,String query){ ImageSearchProvider provider=imageSearchProviders.stream().filter(p->p.getName().equals(code)).findFirst().orElseThrow(()->new IllegalArgumentException("暂不支持该服务的测试："+code)); return siteSearchResult(code,provider.getDisplayName()+"请求完成",provider.search(query,10)); }

    private ExternalApiTestResponse siteSearchResult(String code, String message, List<CoverCandidate> candidates) {
        List<ExternalApiTestItem> items = candidates.stream()
                .map(item -> new ExternalApiTestItem(item.thumbnailUrl(), item.originalUrl(), item.title(), String.valueOf(item.extra().get("pageUrl"))))
                .toList();
        return new ExternalApiTestResponse(code, true, message, items.size(), items);
    }

    @GetMapping("/MT_PHOTOS/thumbnail/{fileId}")
    public ResponseEntity<byte[]> thumbnail(@PathVariable("fileId") Long fileId) {
        MtPhotosThumbnail thumbnail = mtPhotosService.getThumbnail(fileId);
        MediaType type;
        try {
            type = MediaType.parseMediaType(thumbnail.contentType());
        } catch (Exception ignored) {
            type = MediaType.IMAGE_JPEG;
        }
        return ResponseEntity.ok()
                .contentType(type)
                .cacheControl(CacheControl.maxAge(java.time.Duration.ofHours(12)).cachePublic())
                .body(thumbnail.content());
    }
}
