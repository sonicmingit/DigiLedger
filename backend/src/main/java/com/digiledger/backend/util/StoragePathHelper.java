package com.digiledger.backend.util;

import com.digiledger.backend.config.StorageProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 辅助进行对象存储访问路径与 objectKey 的转换。
 */
@Component
public class StoragePathHelper {

    private final StorageProperties storageProperties;

    public StoragePathHelper(StorageProperties storageProperties) {
        this.storageProperties = storageProperties;
    }

    /**
     * 将任意形式（objectKey、相对/绝对路径）的输入解析为对象存储 objectKey。
     */
    public String toObjectKey(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            return null;
        }
        normalized = stripProtocol(normalized);
        normalized = stripApiBase(normalized);
        String ossPrefix = "/oss/" + storageProperties.getBucket() + "/";
        int ossIndex = normalized.indexOf(ossPrefix);
        if (ossIndex >= 0) {
            return safeSubstring(normalized, ossIndex + ossPrefix.length());
        }
        String bucketPrefix = storageProperties.getBucket() + "/";
        if (normalized.startsWith(bucketPrefix)) {
            return safeSubstring(normalized, bucketPrefix.length());
        }
        String bucketSlashPrefix = "/" + bucketPrefix;
        if (normalized.startsWith(bucketSlashPrefix)) {
            return safeSubstring(normalized, bucketSlashPrefix.length());
        }
        if (normalized.startsWith("/")) {
            return normalized.substring(1);
        }
        return normalized;
    }

    /**
     * 根据 objectKey 生成以 /oss/{bucket}/ 为前缀的相对访问路径。
     * 若传入为绝对地址或相对路径，会先解析为 objectKey 再构建标准路径。
     */
    public String toRelativeUrl(String value) {
        String objectKey = toObjectKey(value);
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        return buildRelativePath(objectKey);
    }

    /**
     * 根据 objectKey 构造完整访问 URL。
     */
    /**
     * 根据 objectKey 构造完整访问 URL。
     */
    public String toFullUrl(String value) {
        String objectKey = toObjectKey(value);
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        String baseUrl = storageProperties.getBaseUrl();
        String bucket = storageProperties.getBucket();

        // 构建完整的URL: baseUrl + bucket + objectKey
        StringBuilder urlBuilder = new StringBuilder();

        // 处理基础URL
        if (baseUrl != null && !baseUrl.isBlank()) {
            String normalizedBase = baseUrl.trim();
            // 确保基础URL以斜杠结尾
            if (!normalizedBase.endsWith("/")) {
                normalizedBase = normalizedBase + "/";
            }
            urlBuilder.append(normalizedBase);
        }

        // 添加bucket名称
        if (bucket != null && !bucket.isBlank()) {
            urlBuilder.append(bucket);
            // 确保bucket后有斜杠
            if (!bucket.endsWith("/")) {
                urlBuilder.append("/");
            }
        }

        // 添加对象键，确保开头没有多余的斜杠
        String normalizedKey = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        urlBuilder.append(normalizedKey);

        return urlBuilder.toString();
    }


    /**
     * 将字符串列表批量转换为 objectKey 列表。
     */
    public List<String> toObjectKeys(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::toObjectKey)
                .filter(Objects::nonNull)
                .filter(key -> !key.isBlank())
                .collect(Collectors.toList());
    }

    /**
     * 将 objectKey 列表转换为相对访问路径列表。
     */
    public List<String> toRelativeUrls(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::toRelativeUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 将 objectKey 列表批量转换为完整访问 URL 列表。
     */
    public List<String> toFullUrls(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::toFullUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    private String buildRelativePath(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        String normalizedKey = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        return "/oss/" + storageProperties.getBucket() + "/" + normalizedKey;
    }

    private String stripProtocol(String value) {
        if (value.startsWith("http://") || value.startsWith("https://")) {
            int slashIndex = value.indexOf('/', value.indexOf("//") + 2);
            if (slashIndex >= 0) {
                return value.substring(slashIndex);
            }
            return "";
        }
        if (value.startsWith("//")) {
            int slashIndex = value.indexOf('/', 2);
            if (slashIndex >= 0) {
                return value.substring(slashIndex);
            }
            return "";
        }
        return value;
    }

    private String stripApiBase(String value) {
        String baseUrl = storageProperties.getBaseUrl();
        if (baseUrl != null && !baseUrl.isBlank()) {
            String normalizedBase = stripProtocol(baseUrl.trim());
            if (!normalizedBase.isBlank() && value.startsWith(normalizedBase)) {
                value = value.substring(normalizedBase.length());
            }
        }
        if (value.startsWith("/api/")) {
            return value.substring(4);
        }
        if (value.startsWith("/api")) {
            return value.substring(4);
        }
        return value;
    }

    private String safeSubstring(String value, int beginIndex) {
        if (beginIndex >= value.length()) {
            return "";
        }
        return value.substring(beginIndex);
    }
}
