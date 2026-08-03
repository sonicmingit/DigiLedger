package com.digiledger.backend.util;

import com.digiledger.backend.config.StorageProperties;
import org.springframework.stereotype.Component;

import java.net.URI;
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
     * 将存储引用转换为浏览器访问路径。
     *
     * <p>本地对象统一通过应用代理访问，因而只返回同源的
     * {@code /oss/{bucket}/{objectKey}}。外部网站 URL 和外部 API 路径保持原值，
     * 避免把业务上的远程图片误判为 MinIO 对象。</p>
     */
    public String toBrowserUrl(String value) {
        String normalized = normalizeInput(value);
        if (normalized == null) {
            return null;
        }
        if (shouldPreserveReference(normalized)) {
            return normalized;
        }
        String objectKey = toObjectKey(normalized);
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }
        return buildRelativePath(objectKey);
    }

    /**
     * 保留旧调用方使用的相对路径方法名。
     */
    public String toRelativeUrl(String value) {
        return toBrowserUrl(value);
    }

    /**
     * 将输入规范化为可落库的存储引用。
     * 本地对象仍沿用 {@link #toObjectKey(String)} 的兼容解析；外部 URL/路径则原样保存。
     */
    public String toStoredReference(String value) {
        String normalized = normalizeInput(value);
        if (normalized == null) {
            return null;
        }
        if (shouldPreserveReference(normalized)) {
            return normalized;
        }
        return toObjectKey(normalized);
    }

    /**
     * 兼容旧调用方：本地对象现在统一返回应用代理路径，不再返回 MinIO 绝对地址。
     */
    @Deprecated
    public String toFullUrl(String value) {
        return toBrowserUrl(value);
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
        return toBrowserUrls(values);
    }

    /**
     * 将存储引用列表转换为浏览器访问路径列表；外部 URL 保持原值。
     */
    public List<String> toBrowserUrls(List<String> values) {
        if (values == null || values.isEmpty()) {
            return List.of();
        }
        return values.stream()
                .map(this::toBrowserUrl)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * 兼容旧调用方：本地对象列表现在统一返回应用代理路径列表。
     */
    @Deprecated
    public List<String> toFullUrls(List<String> values) {
        return toBrowserUrls(values);
    }

    private String buildRelativePath(String objectKey) {
        String bucket = storageProperties.getBucket();
        if (objectKey == null || objectKey.isBlank() || bucket == null || bucket.isBlank()) {
            return null;
        }
        String normalizedKey = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        return "/oss/" + bucket + "/" + normalizedKey;
    }

    private String normalizeInput(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private boolean shouldPreserveReference(String value) {
        // 这些是应用自身提供的外部图片代理/其他业务路径，不是对象存储引用。
        if (value.equals("/api") || (value.startsWith("/api/") && !value.startsWith("/api/oss/"))) {
            return true;
        }
        if (isHttpUrl(value)) {
            return !isConfiguredStorageUrl(value);
        }
        // data:、blob:、ftp: 等带 scheme 的业务 URL 也不能被改写成 objectKey。
        return hasUriScheme(value);
    }

    private boolean isHttpUrl(String value) {
        return value.regionMatches(true, 0, "http://", 0, "http://".length())
                || value.regionMatches(true, 0, "https://", 0, "https://".length())
                || value.startsWith("//");
    }

    private boolean hasUriScheme(String value) {
        int colonIndex = value.indexOf(':');
        if (colonIndex <= 0) {
            return false;
        }
        for (int index = 0; index < colonIndex; index++) {
            char character = value.charAt(index);
            if (!(Character.isLetterOrDigit(character) || character == '+' || character == '-'
                    || character == '.')) {
                return false;
            }
        }
        return true;
    }

    private boolean isConfiguredStorageUrl(String value) {
        return matchesConfiguredBase(value, storageProperties.getBaseUrl())
                || matchesConfiguredBase(value, storageProperties.getEndpoint());
    }

    private boolean matchesConfiguredBase(String candidateValue, String configuredValue) {
        if (configuredValue == null || configuredValue.isBlank()) {
            return false;
        }
        URI candidate = parseHttpUri(candidateValue);
        URI configured = parseHttpUri(configuredValue.trim());
        if (candidate == null || configured == null
                || candidate.getHost() == null || configured.getHost() == null
                || !candidate.getHost().equalsIgnoreCase(configured.getHost())) {
            return false;
        }
        if (!samePort(candidate, configured)) {
            return false;
        }
        if (!configured.getScheme().equalsIgnoreCase(candidate.getScheme())) {
            return false;
        }

        String configuredPath = normalizePath(configured.getPath());
        String candidatePath = normalizePath(candidate.getPath());
        return configuredPath.isEmpty()
                || "/".equals(configuredPath)
                || candidatePath.equals(configuredPath)
                || candidatePath.startsWith(configuredPath + "/");
    }

    private URI parseHttpUri(String value) {
        try {
            String candidate = value.startsWith("//") ? "http:" + value : value;
            URI uri = URI.create(candidate);
            if (uri.getScheme() == null
                    || !("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))) {
                return null;
            }
            return uri;
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private boolean samePort(URI first, URI second) {
        return effectivePort(first) == effectivePort(second);
    }

    private int effectivePort(URI uri) {
        if (uri.getPort() >= 0) {
            return uri.getPort();
        }
        return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
    }

    private String normalizePath(String path) {
        if (path == null || path.isBlank() || "/".equals(path)) {
            return "";
        }
        String normalized = path;
        while (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
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
