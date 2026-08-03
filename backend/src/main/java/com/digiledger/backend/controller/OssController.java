package com.digiledger.backend.controller;

import com.digiledger.backend.config.StorageProperties;
import com.digiledger.backend.service.FileService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

/**
 * 对象存储浏览代理。
 *
 * <p>这里只暴露当前配置 bucket 下的单个对象读取，不提供列桶、列对象或任意 bucket
 * 访问能力。MinIO 只由后端容器访问，浏览器始终访问同源的 /oss 路径。</p>
 */
@RestController
@RequestMapping("/oss")
public class OssController {

    private static final CacheControl CACHE_CONTROL = CacheControl
            .maxAge(Duration.ofHours(1))
            .cachePublic()
            .noTransform();

    private final FileService fileService;
    private final StorageProperties storageProperties;

    public OssController(FileService fileService, StorageProperties storageProperties) {
        this.fileService = fileService;
        this.storageProperties = storageProperties;
    }

    /**
     * 读取当前配置 bucket 中的对象。
     * { *objectKey } 由 Spring 的 PathPattern 支持，可覆盖多级对象键路径。
     */
    @GetMapping("/{bucket}/{*objectKey}")
    public ResponseEntity<byte[]> getObject(@PathVariable("bucket") String bucket,
                                            @PathVariable("objectKey") String objectKey) {
        // 对错误 bucket 统一返回 404，避免泄露当前配置或提供 bucket 探测信号。
        if (!Objects.equals(bucket, storageProperties.getBucket())) {
            return ResponseEntity.notFound().build();
        }

        String normalizedKey = normalizeObjectKey(objectKey);
        if (normalizedKey == null) {
            return ResponseEntity.notFound().build();
        }

        byte[] content = fileService.download(normalizedKey);
        MediaType mediaType = resolveMediaType(normalizedKey, content);
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(content.length)
                .cacheControl(CACHE_CONTROL)
                .body(content);
    }

    private String normalizeObjectKey(String objectKey) {
        if (objectKey == null) {
            return null;
        }
        String normalized = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        if (normalized.isBlank() || normalized.startsWith("/") || normalized.indexOf('\0') >= 0
                || normalized.indexOf('\\') >= 0) {
            return null;
        }
        for (String segment : normalized.split("/", -1)) {
            if (segment.isEmpty() || ".".equals(segment) || "..".equals(segment)) {
                return null;
            }
        }
        return normalized;
    }

    private MediaType resolveMediaType(String objectKey, byte[] content) {
        Optional<MediaType> byName = MediaTypeFactory.getMediaType(objectKey);
        if (byName.isPresent()) {
            return byName.get();
        }
        return detectImageMediaType(content).orElse(MediaType.APPLICATION_OCTET_STREAM);
    }

    private Optional<MediaType> detectImageMediaType(byte[] content) {
        if (content == null) {
            return Optional.empty();
        }
        if (content.length >= 8
                && (content[0] & 0xFF) == 0x89 && content[1] == 'P' && content[2] == 'N'
                && content[3] == 'G' && (content[4] & 0xFF) == 0x0D && (content[5] & 0xFF) == 0x0A
                && (content[6] & 0xFF) == 0x1A && (content[7] & 0xFF) == 0x0A) {
            return Optional.of(MediaType.IMAGE_PNG);
        }
        if (content.length >= 3
                && (content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8
                && (content[2] & 0xFF) == 0xFF) {
            return Optional.of(MediaType.IMAGE_JPEG);
        }
        if (content.length >= 6
                && content[0] == 'G' && content[1] == 'I' && content[2] == 'F'
                && content[3] == '8' && (content[4] == '7' || content[4] == '9') && content[5] == 'a') {
            return Optional.of(MediaType.parseMediaType("image/gif"));
        }
        if (content.length >= 12
                && content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P') {
            return Optional.of(MediaType.parseMediaType("image/webp"));
        }
        return Optional.empty();
    }
}
