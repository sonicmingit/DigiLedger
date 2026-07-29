package com.digiledger.backend.integration.mtphotos;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.integration.externalapi.model.ExternalApiConfig;
import com.digiledger.backend.integration.externalapi.service.ExternalApiConfigServiceImpl;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * MT Photos 图库连接器。搜索分页、文件信息、授权码和缩略图全部由后端处理，
 * 使浏览器不会获得 MT Photos API Key 或 auth_code。
 */
@Service
public class MtPhotosServiceImpl implements MtPhotosService {
    private static final int PAGE_SIZE = 10;
    private static final int AUTH_CODE_TTL_SECONDS = 23 * 60 * 60 + 30 * 60;

    private final ExternalApiConfigServiceImpl configService;
    private final ObjectMapper objectMapper;
    private final Map<Long, FileInfo> fileInfoCache = new ConcurrentHashMap<>();
    private final Object authLock = new Object();
    private volatile AuthCodeCache authCodeCache;

    public MtPhotosServiceImpl(ExternalApiConfigServiceImpl configService, ObjectMapper objectMapper) {
        this.configService = configService;
        this.objectMapper = objectMapper;
    }

    @Override
    public MtPhotosSearchResponse testSearch(MtPhotosSearchRequest request) {
        ExternalApiConfig config = configService.requireConfiguredEnabled(ExternalApiConfigServiceImpl.MT_PHOTOS);
        String mode = "CLIP".equalsIgnoreCase(request.mode()) ? "CLIP" : "KEYWORD";
        int page = request.page() == null ? 1 : Math.max(1, request.page());
        Map<String, Object> body = new java.util.LinkedHashMap<>();
        body.put("key", request.query().trim());
        if ("CLIP".equals(mode)) body.put("count", 100);
        JsonNode root = requestJson(config, "POST", "CLIP".equals(mode) ? "/gateway/searchCLIP" : "/gateway/search", body);
        List<JsonNode> allMatches = extractFileNodes(root);
        // MT Photos 的 totalCount 可能包含同一文件在不同匹配字段中的重复命中。
        // 页面实际以文件 ID 展示，因此计数和分页也必须基于可展示的唯一文件 ID。
        List<Long> fileIds = allMatches.stream()
                .map(node -> readLong(node, "id", "ID"))
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        int totalCount = fileIds.size();
        int totalPages = Math.max(1, (int) Math.ceil((double) totalCount / PAGE_SIZE));
        if (page > totalPages) page = totalPages;

        int start = Math.min((page - 1) * PAGE_SIZE, totalCount);
        int end = Math.min(start + PAGE_SIZE, totalCount);
        List<MtPhotosSearchItem> items = new ArrayList<>();
        for (Long id : fileIds.subList(start, end)) {
            FileInfo info = getFileInfo(config, id);
            items.add(new MtPhotosSearchItem(id, info.fileName(), info.capturedAt(), info.fileType(), thumbnailUrl(id)));
        }
        return new MtPhotosSearchResponse(mode, totalCount, page, PAGE_SIZE, totalPages, items);
    }

    @Override
    public MtPhotosThumbnail getThumbnail(Long fileId) {
        if (fileId == null || fileId <= 0) throw new BizException(ErrorCode.VALIDATION_ERROR, "文件 ID 不合法");
        ExternalApiConfig config = configService.requireConfiguredEnabled(ExternalApiConfigServiceImpl.MT_PHOTOS);
        FileInfo info = getFileInfo(config, fileId);
        String authCode = getAuthCode(config);
        String path = "/gateway/h220/" + encodePath(info.md5()) + "?id=" + fileId + "&auth_code=" + encodeQuery(authCode);
        HttpURLConnection connection = null;
        try {
            connection = openConnection(config, "GET", path);
            connection.setRequestProperty("Accept", "image/*");
            int status = connection.getResponseCode();
            try (InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream()) {
                if (status < 200 || status >= 300) {
                    String error = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                    throw new BizException(ErrorCode.VALIDATION_ERROR, "读取 MT Photos 缩略图失败（HTTP " + status + "）：" + abbreviate(error));
                }
                String contentType = connection.getContentType();
                return new MtPhotosThumbnail(stream.readAllBytes(), StringUtils.hasText(contentType) ? contentType : "image/jpeg");
            }
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "获取 MT Photos 缩略图失败：" + ex.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private FileInfo getFileInfo(ExternalApiConfig config, Long fileId) {
        FileInfo cached = fileInfoCache.get(fileId);
        if (cached != null) return cached;
        JsonNode node = requestJson(config, "GET", "/gateway/fileInfoById/" + fileId, null);
        String md5 = readText(node, "MD5", "md5");
        if (!StringUtils.hasText(md5)) throw new BizException(ErrorCode.VALIDATION_ERROR, "MT Photos 未返回文件 MD5，无法生成缩略图");
        FileInfo info = new FileInfo(md5, readText(node, "fileName", "name"), readText(node, "tokenAt", "createTime", "date"), readText(node, "fileType"));
        // 搜索页只会预热当前 10 条；加上上限避免长时间运行时缓存无限增长。
        if (fileInfoCache.size() >= 500) fileInfoCache.clear();
        fileInfoCache.put(fileId, info);
        return info;
    }

    private String getAuthCode(ExternalApiConfig config) {
        AuthCodeCache cached = authCodeCache;
        if (cached != null && cached.apiKey().equals(config.getCredentialSecret()) && Instant.now().isBefore(cached.expiresAt())) return cached.value();
        synchronized (authLock) {
            cached = authCodeCache;
            if (cached != null && cached.apiKey().equals(config.getCredentialSecret()) && Instant.now().isBefore(cached.expiresAt())) return cached.value();
            JsonNode response = requestJson(config, "POST", "/auth/auth_code", Map.of("api_key", config.getCredentialSecret()));
            String authCode = readText(response, "auth_code", "authCode");
            if (!StringUtils.hasText(authCode)) throw new BizException(ErrorCode.VALIDATION_ERROR, "MT Photos 未返回 auth_code");
            authCodeCache = new AuthCodeCache(config.getCredentialSecret(), authCode, Instant.now().plusSeconds(AUTH_CODE_TTL_SECONDS));
            return authCode;
        }
    }

    private JsonNode requestJson(ExternalApiConfig config, String method, String path, Map<String, Object> body) {
        HttpURLConnection connection = null;
        try {
            connection = openConnection(config, method, path);
            connection.setRequestProperty("Accept", "application/json");
            if (body != null) {
                connection.setDoOutput(true);
                connection.setRequestProperty("Content-Type", "application/json");
                try (OutputStream output = connection.getOutputStream()) {
                    output.write(objectMapper.writeValueAsBytes(body));
                }
            }
            int status = connection.getResponseCode();
            try (InputStream stream = status >= 200 && status < 300 ? connection.getInputStream() : connection.getErrorStream()) {
                String content = stream == null ? "" : new String(stream.readAllBytes(), StandardCharsets.UTF_8);
                if (status < 200 || status >= 300) {
                    throw new BizException(ErrorCode.VALIDATION_ERROR, "MT Photos 请求失败（HTTP " + status + "）：" + abbreviate(content));
                }
                return objectMapper.readTree(content);
            }
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "无法连接 MT Photos：" + ex.getMessage());
        } finally {
            if (connection != null) connection.disconnect();
        }
    }

    private HttpURLConnection openConnection(ExternalApiConfig config, String method, String path) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) URI.create(config.getBaseUrl() + path).toURL().openConnection();
        connection.setRequestMethod(method);
        connection.setConnectTimeout(config.getTimeoutMs());
        connection.setReadTimeout(config.getTimeoutMs());
        connection.setRequestProperty("x-api-key", config.getCredentialSecret());
        return connection;
    }

    /** 兼容 MT Photos 不同版本的按日期分组和扁平列表响应。 */
    private List<JsonNode> extractFileNodes(JsonNode root) {
        List<JsonNode> files = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();
        collectFileNodes(root.path("list"), files, unique);
        if (files.isEmpty()) collectFileNodes(root.path("result"), files, unique);
        if (files.isEmpty()) collectFileNodes(root.path("data"), files, unique);
        return files;
    }

    private void collectFileNodes(JsonNode node, List<JsonNode> output, Set<String> unique) {
        if (node == null || node.isMissingNode() || node.isNull()) return;
        if (node.isArray()) {
            for (JsonNode child : node) collectFileNodes(child, output, unique);
            return;
        }
        if (!node.isObject()) return;
        if (isFileNode(node)) {
            String key = node.hasNonNull("id") ? "id:" + node.get("id").asText() : node.toString();
            if (unique.add(key)) output.add(node);
            return;
        }
        for (String wrapper : List.of("files", "list", "items", "data", "records", "result")) {
            JsonNode child = node.path(wrapper);
            if (!child.isMissingNode() && !child.isNull()) collectFileNodes(child, output, unique);
        }
    }

    private boolean isFileNode(JsonNode node) {
        return node.has("md5") || node.has("MD5") || node.has("fileName") || node.has("originalFileName")
                || (node.has("id") && (node.has("filePath") || node.has("tokenAt") || node.has("fileType")));
    }

    private static Long readLong(JsonNode node, String... names) {
        for (String name : names) {
            if (!node.hasNonNull(name)) continue;
            JsonNode value = node.get(name);
            if (value.canConvertToLong()) return value.asLong();
            if (value.isTextual()) {
                try {
                    return Long.parseLong(value.asText().trim());
                } catch (NumberFormatException ignored) {
                    // 继续尝试其他兼容字段。
                }
            }
        }
        return null;
    }

    private static String readText(JsonNode node, String... names) {
        for (String name : names) if (node.hasNonNull(name)) return node.get(name).asText();
        return null;
    }

    private static String thumbnailUrl(Long fileId) {
        return "/api/external-api-configs/MT_PHOTOS/thumbnail/" + fileId;
    }

    private static String encodePath(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20"); }
    private static String encodeQuery(String value) { return URLEncoder.encode(value, StandardCharsets.UTF_8); }
    private static String abbreviate(String content) {
        String normalized = StringUtils.hasText(content) ? content.replaceAll("\\s+", " ") : "服务未返回错误详情";
        return normalized.length() > 180 ? normalized.substring(0, 180) + "…" : normalized;
    }

    private record FileInfo(String md5, String fileName, String capturedAt, String fileType) { }
    private record AuthCodeCache(String apiKey, String value, Instant expiresAt) { }
}
