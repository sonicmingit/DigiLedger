package com.digiledger.backend.integration.removebg;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.integration.externalapi.model.ExternalApiConfig;
import com.digiledger.backend.integration.externalapi.service.ExternalApiConfigService;
import com.digiledger.backend.integration.externalapi.service.ExternalApiConfigServiceImpl;
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

    private final ExternalApiConfigService configService;
    private final RestTemplateBuilder restTemplateBuilder;
    private final Map<Long, Deque<Long>> rateLimitMap = new ConcurrentHashMap<>();

    public BackgroundRemovalService(ExternalApiConfigService configService, RestTemplateBuilder restTemplateBuilder) {
        this.configService = configService;
        this.restTemplateBuilder = restTemplateBuilder;
    }

    public byte[] removeBackground(Long assetId, byte[] sourceBytes, String fileName) {
        ExternalApiConfig config = configService.requireConfiguredEnabled(ExternalApiConfigServiceImpl.REMOVE_BG);
        enforceRateLimit(assetId);
        HttpHeaders headers = new HttpHeaders();
        headers.add("X-API-Key", config.getCredentialSecret());
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
            response = restTemplate(config).postForEntity(config.getBaseUrl(), request, byte[].class);
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "抠图服务暂时不可用，请稍后再试");
        }
        if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "抠图服务暂时不可用，请稍后再试");
        }
        return response.getBody();
    }

    private RestTemplate restTemplate(ExternalApiConfig config) {
        int timeout = config.getTimeoutMs() == null ? 120000 : config.getTimeoutMs();
        return restTemplateBuilder
                .setConnectTimeout(Duration.ofMillis(timeout))
                .setReadTimeout(Duration.ofMillis(timeout))
                .build();
    }

    private void enforceRateLimit(Long assetId) {
        long now = System.currentTimeMillis();
        // 新增物品尚未持有资产 ID，统一使用独立桶限制其预览请求。
        long limitKey = assetId == null ? 0L : assetId;
        Deque<Long> deque = rateLimitMap.computeIfAbsent(limitKey, k -> new ArrayDeque<>());
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
