package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.model.dto.setting.ImageSearchProviderDTO;
import com.digiledger.backend.model.dto.setting.ImageSearchProvidersResponse;
import com.digiledger.backend.model.dto.setting.UpdateDefaultImageSearchProviderRequest;
import com.digiledger.backend.service.impl.ImageSearchPreferenceService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 智能找图服务相关接口
 */
@RestController
@RequestMapping("/api/image-search")
public class ImageSearchSettingController {

    private final ImageSearchPreferenceService preferenceService;

    public ImageSearchSettingController(ImageSearchPreferenceService preferenceService) {
        this.preferenceService = preferenceService;
    }

    @GetMapping("/providers")
    public ApiResponse<ImageSearchProvidersResponse> providers() {
        List<ImageSearchProviderDTO> providers = preferenceService.listProviders();
        String defaultProvider = preferenceService.getDefaultProvider().orElse(null);
        return ApiResponse.success(new ImageSearchProvidersResponse(providers, defaultProvider));
    }

    @PutMapping("/providers/default")
    public ApiResponse<Void> updateDefault(@RequestBody(required = false) UpdateDefaultImageSearchProviderRequest request) {
        String provider = request != null ? request.provider() : null;
        preferenceService.updateDefaultProvider(provider);
        return ApiResponse.success();
    }
}
