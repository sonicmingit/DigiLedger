package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.SystemSettingMapper;
import com.digiledger.backend.model.dto.setting.ImageSearchProviderDTO;
import com.digiledger.backend.model.entity.SystemSetting;
import com.digiledger.backend.service.cover.ImageSearchProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * 智能找图服务配置
 */
@Service
public class ImageSearchPreferenceService {

    private final SystemSettingMapper systemSettingMapper;
    private final List<ImageSearchProvider> imageSearchProviders;

    public ImageSearchPreferenceService(SystemSettingMapper systemSettingMapper,
                                        List<ImageSearchProvider> imageSearchProviders) {
        this.systemSettingMapper = systemSettingMapper;
        this.imageSearchProviders = CollectionUtils.isEmpty(imageSearchProviders)
                ? List.of()
                : imageSearchProviders;
    }

    public List<ImageSearchProviderDTO> listProviders() {
        if (imageSearchProviders.isEmpty()) {
            return List.of();
        }
        return imageSearchProviders.stream()
                .map(ImageSearchProviderDTO::fromProvider)
                .toList();
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
}
