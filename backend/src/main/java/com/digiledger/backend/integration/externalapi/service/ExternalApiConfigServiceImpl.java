package com.digiledger.backend.integration.externalapi.service;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.integration.externalapi.persistence.ExternalApiConfigMapper;
import com.digiledger.backend.integration.externalapi.dto.ExternalApiConfigRequest;
import com.digiledger.backend.integration.externalapi.dto.ExternalApiConfigResponse;
import com.digiledger.backend.integration.externalapi.model.ExternalApiConfig;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.net.URI;
import java.util.List;

@Service
public class ExternalApiConfigServiceImpl implements ExternalApiConfigService {
    public static final String MT_PHOTOS = "MT_PHOTOS";
    public static final String JD_UNION_PRODUCT_SEARCH = "JD_UNION_PRODUCT_SEARCH";
    public static final String TAOBAO_UNION_PRODUCT_SEARCH = "TAOBAO_UNION_PRODUCT_SEARCH";
    public static final String ICECAT_PRODUCT_CATALOG = "ICECAT_PRODUCT_CATALOG";
    public static final String BRAVE_IMAGE_SEARCH = "BRAVE_IMAGE_SEARCH";
    public static final String SERPAPI_GOOGLE_IMAGES = "SERPAPI_GOOGLE_IMAGES";
    public static final String REMOVE_BG = "REMOVE_BG";

    private final ExternalApiConfigMapper mapper;

    public ExternalApiConfigServiceImpl(ExternalApiConfigMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public List<ExternalApiConfigResponse> list() {
        return mapper.findAll().stream().map(ExternalApiConfigResponse::from).toList();
    }

    @Override
    public ExternalApiConfigResponse get(String apiCode) {
        return ExternalApiConfigResponse.from(requireConfig(apiCode));
    }

    @Override
    @Transactional
    public ExternalApiConfigResponse save(String apiCode, ExternalApiConfigRequest request) {
        String code = normalizeCode(apiCode);
        validateBaseUrl(request.baseUrl());
        ExternalApiConfig current = mapper.findByCode(code);
        ExternalApiConfig target = current == null ? new ExternalApiConfig() : current;
        target.setApiCode(code);
        target.setDisplayName(request.displayName().trim());
        target.setBaseUrl(trimTrailingSlash(request.baseUrl()));
        target.setAuthType(StringUtils.hasText(request.authType()) ? request.authType().trim() : "API_KEY");
        target.setConfigJson(StringUtils.hasText(request.configJson()) ? request.configJson().trim() : null);
        target.setTimeoutMs(request.timeoutMs() == null ? 15000 : request.timeoutMs());
        target.setEnabled(request.enabled() == null || request.enabled());
        if (StringUtils.hasText(request.apiKey())) {
            target.setCredentialSecret(request.apiKey().trim());
        }
        if (current == null) {
            mapper.insert(target);
        } else {
            mapper.update(target);
        }
        return ExternalApiConfigResponse.from(target);
    }

    @Override
    public ExternalApiConfig requireEnabled(String apiCode) {
        ExternalApiConfig config = requireConfig(apiCode);
        if (!Boolean.TRUE.equals(config.getEnabled())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "该外接 API 尚未启用");
        }
        return config;
    }

    @Override
    public ExternalApiConfig requireConfiguredEnabled(String apiCode) {
        ExternalApiConfig config = requireEnabled(apiCode);
        if (!StringUtils.hasText(config.getCredentialSecret())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "请先配置 API Key");
        }
        return config;
    }

    private ExternalApiConfig requireConfig(String apiCode) {
        ExternalApiConfig config = mapper.findByCode(normalizeCode(apiCode));
        if (config == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "请先保存外接 API 配置");
        }
        return config;
    }

    private static String normalizeCode(String apiCode) {
        if (!StringUtils.hasText(apiCode)) throw new BizException(ErrorCode.VALIDATION_ERROR, "API 编码不能为空");
        return apiCode.trim().toUpperCase();
    }

    private static void validateBaseUrl(String baseUrl) {
        try {
            URI uri = URI.create(baseUrl.trim());
            if (uri.getScheme() == null || uri.getHost() == null || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                throw new IllegalArgumentException();
            }
        } catch (Exception ex) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "服务地址必须是有效的 HTTP(S) 地址");
        }
    }

    private static String trimTrailingSlash(String value) {
        String url = value.trim();
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
