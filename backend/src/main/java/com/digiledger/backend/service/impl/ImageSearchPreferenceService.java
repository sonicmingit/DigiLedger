package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.SystemSettingMapper;
import com.digiledger.backend.integration.cover.ImageSearchProvider;
import com.digiledger.backend.integration.externalapi.dto.ExternalApiConfigResponse;
import com.digiledger.backend.integration.externalapi.service.ExternalApiConfigService;
import com.digiledger.backend.model.dto.setting.ImageSearchProviderDTO;
import com.digiledger.backend.model.entity.SystemSetting;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.Objects;
import java.util.Optional;

/**
 * 智能找图服务配置
 */
@Service
public class ImageSearchPreferenceService {

    private final SystemSettingMapper systemSettingMapper;
    private final List<ImageSearchProvider> imageSearchProviders;
    private final ExternalApiConfigService externalApiConfigService;
    private final ObjectMapper objectMapper;

    public ImageSearchPreferenceService(SystemSettingMapper systemSettingMapper,
                                        List<ImageSearchProvider> imageSearchProviders,
                                        ExternalApiConfigService externalApiConfigService,
                                        ObjectMapper objectMapper) {
        this.systemSettingMapper = systemSettingMapper;
        this.imageSearchProviders = CollectionUtils.isEmpty(imageSearchProviders)
                ? List.of()
                : imageSearchProviders;
        this.externalApiConfigService = externalApiConfigService;
        this.objectMapper = objectMapper;
    }

    public List<ImageSearchProviderDTO> listProviders() {
        if (imageSearchProviders.isEmpty()) {
            return List.of();
        }
        Set<String> availableCodes = availableProviderCodes();
        return imageSearchProviders.stream()
                .map(provider -> ImageSearchProviderDTO.fromProvider(provider, availableCodes.contains(provider.getName())))
                .toList();
    }

    /** 返回既被管理员选中、又仍处于“已配置且启用”状态的服务。 */
    public List<String> getEnabledProviders() {
        Set<String> availableCodes = availableProviderCodes();
        List<String> saved = parseSavedProviderCodes();
        if (saved.isEmpty()) {
            return imageSearchProviders.stream()
                    .map(ImageSearchProvider::getName)
                    .filter(availableCodes::contains)
                    .toList();
        }
        return saved.stream().filter(availableCodes::contains).toList();
    }

    @Transactional
    public void updateEnabledProviders(List<String> providers) {
        Set<String> supported = imageSearchProviders.stream().map(ImageSearchProvider::getName).collect(java.util.stream.Collectors.toSet());
        Set<String> available = availableProviderCodes();
        LinkedHashSet<String> selected = new LinkedHashSet<>();
        if (providers != null) {
            for (String provider : providers) {
                if (!StringUtils.hasText(provider)) continue;
                String code = provider.trim().toUpperCase();
                if (!supported.contains(code)) throw new BizException(ErrorCode.VALIDATION_ERROR, "不支持的搜图服务：" + code);
                if (!available.contains(code)) throw new BizException(ErrorCode.VALIDATION_ERROR, "请先在外接服务中心配置并启用：" + code);
                selected.add(code);
            }
        }
        String serialized;
        try {
            serialized = objectMapper.writeValueAsString(selected);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "保存搜图配置失败");
        }
        SystemSetting current = systemSettingMapper.findLatest();
        if (current == null) systemSettingMapper.insertImageSearchProviders(serialized);
        else systemSettingMapper.updateImageSearchProviders(current.getId(), serialized);
    }

    public Optional<String> getDefaultProvider() {
        return Optional.ofNullable(systemSettingMapper.findLatest())
                .map(SystemSetting::getDefaultCoverProvider)
                .filter(StringUtils::hasText);
    }

    @Transactional
    public void updateDefaultProvider(String providerName) {
        String normalized = StringUtils.hasText(providerName) ? providerName.trim() : null;
        if (normalized != null && findProvider(normalized).isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "找图服务不存在");
        }
        SystemSetting current = systemSettingMapper.findLatest();
        if (current == null) {
            if (normalized != null) {
                systemSettingMapper.insertDefaultProvider(normalized);
            }
            return;
        }
        if (!Objects.equals(current.getDefaultCoverProvider(), normalized)) {
            systemSettingMapper.updateDefaultProvider(current.getId(), normalized);
        }
    }

    public Optional<ImageSearchProvider> findProvider(String providerName) {
        if (!StringUtils.hasText(providerName) || imageSearchProviders.isEmpty()) {
            return Optional.empty();
        }
        String target = providerName.trim();
        return imageSearchProviders.stream()
                .filter(provider -> provider.getName().equalsIgnoreCase(target))
                .findFirst();
    }

    private Set<String> availableProviderCodes() {
        return externalApiConfigService.list().stream()
                .filter(config -> config.enabled() && config.apiKeyConfigured())
                .map(ExternalApiConfigResponse::apiCode)
                .collect(java.util.stream.Collectors.toSet());
    }

    private List<String> parseSavedProviderCodes() {
        String raw = Optional.ofNullable(systemSettingMapper.findLatest())
                .map(SystemSetting::getImageSearchProviderCodes)
                .orElse(null);
        if (!StringUtils.hasText(raw)) return List.of();
        try {
            return objectMapper.readValue(raw, new TypeReference<List<String>>() {}).stream()
                    .filter(StringUtils::hasText)
                    .map(value -> value.trim().toUpperCase())
                    .distinct()
                    .toList();
        } catch (Exception ex) {
            return List.of();
        }
    }
}
