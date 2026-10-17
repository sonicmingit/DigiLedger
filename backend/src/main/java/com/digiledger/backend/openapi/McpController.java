package com.digiledger.backend.openapi;

import com.digiledger.backend.common.BizException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

/** Stateless Streamable HTTP: one JSON-RPC request and JSON response per POST. */
@RestController
@RequestMapping(McpController.PATH)
public class McpController {
    public static final String PATH = "/api/open/mcp";
    private static final String VERSION = "2025-11-25";
    private static final Set<String> VERSIONS = Set.of("2025-03-26", "2025-06-18", VERSION);
    private static final Logger log = LoggerFactory.getLogger(McpController.class);
    private final ObjectMapper json;
    private final OpenApiAccessService access;
    private final McpToolService tools;

    public McpController(ObjectMapper json, OpenApiAccessService access, McpToolService tools) {
        this.json = json; this.access = access; this.tools = tools;
    }

    @PostMapping(produces = "application/json")
    public ResponseEntity<?> message(@RequestBody String body, HttpServletRequest request) {
        String protocol = request.getHeader("MCP-Protocol-Version");
        if (protocol != null && !VERSIONS.contains(protocol)) return reply(400, error(null, -32600, "不支持的 MCP 协议版本"));
        JsonNode message;
        try { message = json.readTree(body); }
        catch (JsonProcessingException ex) { return reply(400, error(null, -32700, "JSON 解析失败")); }
        if (message == null || !message.isObject() || !"2.0".equals(message.path("jsonrpc").asText())
                || !message.path("method").isTextual()
                || (message.has("id") && !(message.get("id").isTextual() || message.get("id").isIntegralNumber()))
                || (message.has("params") && !message.get("params").isObject())) {
            return reply(400, error(null, -32600, "无效的 JSON-RPC 请求；不支持批量请求"));
        }
        JsonNode id = message.get("id");
        String method = message.path("method").asText();
        // Never execute a tool from a notification, even if its method is tools/call.
        if (id == null) {
            return method.startsWith("notifications/") ? reply(202, null)
                    : reply(400, error(null, -32600, "此方法需要请求 ID"));
        }
        JsonNode params = message.path("params");
        try {
            return switch (method) {
                case "initialize" -> {
                    if (!params.path("protocolVersion").isTextual() || !params.path("capabilities").isObject()
                            || !params.path("clientInfo").path("name").isTextual()
                            || !params.path("clientInfo").path("version").isTextual()) {
                        yield reply(200, error(id, -32602, "initialize 需要 protocolVersion、capabilities 和 clientInfo"));
                    }
                    String wanted = params.path("protocolVersion").asText();
                    yield reply(200, result(id, Map.of("protocolVersion", VERSIONS.contains(wanted) ? wanted : VERSION,
                            "serverInfo", Map.of("name", "digiledger", "version", "1.0.0"),
                            "capabilities", Map.of("tools", Map.of()),
                            "instructions", "管理 DigiLedger 共享账本。录入前查询真实字典 ID；不编造购买信息；删除须用户明确要求。写入超时后先查询核验，避免重复操作。")));
                }
                case "ping" -> reply(200, result(id, Map.of()));
                case "tools/list" -> {
                    if (params.has("cursor")) yield reply(200, error(id, -32602, "工具列表不支持 cursor"));
                    boolean write = access.authorize(request.getHeader("Authorization"), true) == 0;
                    yield reply(200, result(id, Map.of("tools", tools.list(write))));
                }
                case "tools/call" -> call(id, params, request.getHeader("Authorization"));
                default -> reply(200, error(id, -32601, "不支持的方法：" + method));
            };
        } catch (Exception ex) {
            log.error("MCP request failed: {}", method, ex);
            return reply(200, error(id, -32603, "服务内部错误"));
        }
    }

    private ResponseEntity<?> call(JsonNode id, JsonNode params, String authorization) {
        String name = params.path("name").asText("");
        if (!tools.contains(name)) return reply(200, error(id, -32602, "未知工具：" + name));
        if (params.has("arguments") && !params.get("arguments").isObject()) return reply(200, error(id, -32602, "arguments 必须是对象"));
        // POST is also used for reads; writes must be authorized by tool, on every call.
        if (tools.isWrite(name) && access.authorize(authorization, true) != 0) {
            return reply(200, result(id, toolResult("Token 没有写入权限或开放接口已关闭", true)));
        }
        try {
            ObjectNode args = params.has("arguments") ? (ObjectNode) params.get("arguments") : json.createObjectNode();
            return reply(200, result(id, toolResult(json.writeValueAsString(tools.call(name, args)), false)));
        } catch (BizException | ConstraintViolationException | IllegalArgumentException ex) {
            return reply(200, result(id, toolResult(ex.getMessage(), true)));
        } catch (Exception ex) {
            log.error("MCP tool failed: {}", name, ex);
            return reply(200, result(id, toolResult("操作失败，请查询当前物品状态后再决定是否重试", true)));
        }
    }

    private Map<String, Object> toolResult(String text, boolean error) {
        return Map.of("content", List.of(Map.of("type", "text", "text", text == null ? "操作失败" : text)), "isError", error);
    }
    private ObjectNode result(JsonNode id, Object value) {
        ObjectNode message = json.createObjectNode().put("jsonrpc", "2.0");
        message.set("id", id); message.set("result", json.valueToTree(value)); return message;
    }
    private ObjectNode error(JsonNode id, int code, String text) {
        ObjectNode message = json.createObjectNode().put("jsonrpc", "2.0");
        message.set("id", id); message.putObject("error").put("code", code).put("message", text); return message;
    }
    private ResponseEntity<?> reply(int status, Object value) {
        return ResponseEntity.status(status).header(HttpHeaders.CACHE_CONTROL, "no-store").body(value);
    }

    @RequestMapping(method = {RequestMethod.GET, RequestMethod.HEAD, RequestMethod.DELETE, RequestMethod.PUT, RequestMethod.PATCH})
    public ResponseEntity<?> noStream() {
        return ResponseEntity.status(405).header(HttpHeaders.ALLOW, "POST, OPTIONS")
                .header(HttpHeaders.CACHE_CONTROL, "no-store").build();
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<?> unreadable() { return reply(400, error(null, -32700, "JSON 请求体不能为空")); }
}
