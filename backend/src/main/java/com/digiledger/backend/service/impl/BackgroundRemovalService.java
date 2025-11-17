package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.config.RemoveBgProperties;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.boot.web.client.RestTemplateBuilder;

import java.time.Duration;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 调用 remove.bg 抠图
 */
@Service
public class BackgroundRemovalService {

    private static final int MAX_ATTEMPTS = 3;
    private static final long WINDOW_MS = 5 * 60 * 1000L;

    private final RemoveBgProperties properties;
    private final RestTemplate restTemplate;
    private final Map<Long, Deque<Long>> rateLimitMap = new ConcurrentHashMap<>();

    public BackgroundRemovalService(RemoveBgProperties properties) {
        this.properties = properties;
        this.restTemplate = new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .setReadTimeout(Duration.ofMillis(properties.getTimeoutMs()))
                .build();
    }

    public byte[] removeBackground(Long assetId, byte[] sourceBytes, String fileName) {
        ensureConfigured();
        enforceRateLimit(assetId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-Key", properties.getApiKey());
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        ByteArrayResource resource = new ByteArrayResource(sourceBytes) {
            @Override
            public String getFilename() {
                return fileName;
            }
        };
        body.add("image_file", resource);

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<byte[]> response;
        try {
            response = restTemplate.postForEntity(properties.getEndpoint(), request, byte[].class);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "抠图服务暂时不可用，请稍后再试");
        }
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "抠图服务暂时不可用，请稍后再试");
        }
        return response.getBody();
    }

    private void ensureConfigured() {
        if (properties.getApiKey() == null || properties.getApiKey().isBlank()) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "抠图服务未配置 API Key");
        }
    }

    private void enforceRateLimit(Long assetId) {
        long now = System.currentTimeMillis();
        Deque<Long> deque = rateLimitMap.computeIfAbsent(assetId, k -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && now - deque.peekFirst() > WINDOW_MS) {
                deque.pollFirst();
            }
            if (deque.size() >= MAX_ATTEMPTS) {
                throw new BizException(ErrorCode.RATE_LIMIT_EXCEEDED);
            }
            deque.addLast(now);
        }
    }
}
