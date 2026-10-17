package com.digiledger.backend.openapi;

import com.digiledger.backend.model.dto.asset.AssetCreateRequest;
import com.digiledger.backend.service.AssetService;
import com.digiledger.backend.service.DictService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.Validator;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** MCP and the portable Skill share the same service layer and API schemas. */
@Service
public class McpToolService {
    private final AssetService assets;
    private final DictService dictionaries;
    private final OpenAssetService edits;
    private final ObjectMapper json;
    private final Validator validator;
    private final Map<String, ObjectNode> tools = new LinkedHashMap<>();

    public McpToolService(AssetService assets, DictService dictionaries, OpenAssetService edits,
                          ObjectMapper json, Validator validator) throws IOException {
        this.assets = assets; this.dictionaries = dictionaries; this.edits = edits;
        this.json = json; this.validator = validator;
        JsonNode spec;
        try (var input = new ClassPathResource("agent-skill/digiledger/references/openapi.json").getInputStream()) {
            spec = json.readTree(input);
        }
        ObjectNode filters = objectSchema();
        for (JsonNode parameter : spec.at("/paths/~1assets/get/parameters")) {
            filters.withObject("/properties").set(parameter.path("name").asText(), resolve(parameter.path("schema"), spec));
        }
        filters.withObject("/properties").set("tag_ids", json.readTree("{\"type\":\"array\",\"items\":{\"type\":\"integer\",\"minimum\":1}}"));
        add("list_assets", "分页查询物品。支持关键词、状态、分类、品牌、平台和标签筛选；按 total 继续分页。", filters, false);
        ObjectNode id = objectSchema();
        id.withObject("/properties").set("id", json.readTree("{\"type\":\"integer\",\"minimum\":1,\"format\":\"int64\"}"));
        id.putArray("required").add("id");
        add("get_asset", "查询物品详情，包含购买、出售、标签和成本。", id, false);
        ObjectNode create = (ObjectNode) resolve(spec.at("/components/schemas/AssetCreate"), spec);
        add("create_asset", "新增物品，可同时录入 purchases 购买记录。先查询真实分类 ID；不要编造价格或日期。写入超时后先查询，勿盲目重试。", create, true);
        ObjectNode patch = (ObjectNode) resolve(spec.at("/components/schemas/AssetPatch"), spec);
        ObjectNode update = id.deepCopy();
        update.withObject("/properties").set("patch", patch);
        update.withArray("required").add("patch");
        add("update_asset", "局部编辑物品，只修改 patch 中提交的字段，保留购买记录。null 清空可空字段，[] 清空集合；已丢弃状态会清理出售记录。编辑后查询核验。", update, true);
        add("delete_asset", "仅在用户明确要求删除时调用。沿用系统关联记录限制，删除后无法恢复。", id, true);
        add("list_categories", "查询分类树，录入物品前用它取得真实分类 ID。", objectSchema(), false);
        add("list_brands", "查询品牌字典。", objectSchema(), false);
        add("list_platforms", "查询购买和出售平台字典。", objectSchema(), false);
        add("list_tags", "查询标签树。", objectSchema(), false);
    }

    private ObjectNode objectSchema() {
        ObjectNode schema = json.createObjectNode().put("type", "object").put("additionalProperties", false);
        schema.putObject("properties");
        return schema;
    }

    /** Inline local OpenAPI references and translate nullable to JSON Schema unions. */
    private JsonNode resolve(JsonNode source, JsonNode spec) {
        if (source.has("$ref")) return resolve(spec.at(source.path("$ref").asText().substring(1)), spec);
        if (source.isArray()) {
            var result = json.createArrayNode(); source.forEach(item -> result.add(resolve(item, spec))); return result;
        }
        if (!source.isObject()) return source.deepCopy();
        ObjectNode result = json.createObjectNode();
        source.fields().forEachRemaining(entry -> {
            if (!"nullable".equals(entry.getKey())) result.set(entry.getKey(), resolve(entry.getValue(), spec));
        });
        if (source.path("nullable").asBoolean()) result.putArray("type").add(source.path("type").asText()).add("null");
        if ("object".equals(source.path("type").asText())) result.put("additionalProperties", false);
        return result;
    }

    private void add(String name, String description, ObjectNode schema, boolean write) {
        ObjectNode tool = json.createObjectNode().put("name", name).put("description", description);
        tool.set("inputSchema", schema);
        tool.putObject("annotations").put("readOnlyHint", !write).put("destructiveHint", write)
                .put("idempotentHint", !write).put("openWorldHint", false);
        tools.put(name, tool);
    }

    public List<ObjectNode> list(boolean allowWrite) {
        return tools.values().stream().filter(tool -> allowWrite || !isWrite(tool.path("name").asText()))
                .map(ObjectNode::deepCopy).toList();
    }

    public boolean contains(String name) { return tools.containsKey(name); }
    public boolean isWrite(String name) { return List.of("create_asset", "update_asset", "delete_asset").contains(name); }

    public Object call(String name, ObjectNode arguments) {
        validate(arguments, tools.get(name).path("inputSchema"), "arguments");
        return switch (name) {
            case "list_assets" -> assets.pageAssets(string(arguments, "status"), string(arguments, "keyword"),
                    optionalId(arguments, "category_id"), optionalId(arguments, "brand_id"), optionalId(arguments, "platform_id"),
                    ids(arguments.get("tag_ids")), arguments.path("page").asInt(1), arguments.path("page_size").asInt(20), "purchaseDate", "desc");
            case "get_asset" -> assets.getAssetDetail(arguments.path("id").longValue());
            case "create_asset" -> {
                AssetCreateRequest request = json.convertValue(arguments, AssetCreateRequest.class);
                var violations = validator.validate(request);
                if (!violations.isEmpty()) throw new IllegalArgumentException(violations.iterator().next().getMessage());
                yield Map.of("id", assets.createAsset(request));
            }
            case "update_asset" -> {
                edits.patch(arguments.path("id").longValue(), (ObjectNode) arguments.get("patch"));
                yield Map.of("success", true);
            }
            case "delete_asset" -> { assets.deleteAsset(arguments.path("id").longValue()); yield Map.of("success", true); }
            case "list_categories" -> dictionaries.getCategoryTree();
            case "list_brands" -> dictionaries.listBrands();
            case "list_platforms" -> dictionaries.listPlatforms();
            case "list_tags" -> dictionaries.getTagTree();
            default -> throw new IllegalArgumentException("未知工具");
        };
    }

    private String string(ObjectNode args, String key) { return args.has(key) ? args.get(key).textValue() : null; }
    private Long optionalId(ObjectNode args, String key) { return args.has(key) ? args.get(key).longValue() : null; }
    private List<Long> ids(JsonNode node) {
        if (node == null) return null;
        List<Long> result = new ArrayList<>(); node.forEach(id -> result.add(id.longValue())); return result;
    }

    /** Validate advertised types before Jackson can coerce strings, booleans or overflowing numbers. */
    private void validate(JsonNode value, JsonNode schema, String path) {
        JsonNode type = schema.path("type");
        List<String> types = new ArrayList<>();
        if (type.isArray()) type.forEach(t -> types.add(t.asText())); else types.add(type.asText());
        boolean valid = types.stream().anyMatch(t -> switch (t) {
            case "null" -> value.isNull();
            case "object" -> value.isObject();
            case "array" -> value.isArray();
            case "string" -> value.isTextual();
            case "integer" -> value.isIntegralNumber() && value.canConvertToLong()
                    && (!"int32".equals(schema.path("format").asText()) || value.canConvertToInt());
            case "number" -> value.isNumber();
            case "boolean" -> value.isBoolean();
            default -> false;
        });
        if (!valid) throw new IllegalArgumentException(path + " 类型无效");
        if (value.isNull()) return;
        if (schema.has("enum")) {
            boolean allowed = false;
            for (JsonNode option : schema.get("enum")) if (option.equals(value)) allowed = true;
            if (!allowed) throw new IllegalArgumentException(path + " 值不在允许范围内");
        }
        if (value.isObject()) {
            for (JsonNode key : schema.path("required")) if (!value.has(key.asText())) throw new IllegalArgumentException(path + "." + key.asText() + " 必填");
            if (value.size() < schema.path("minProperties").asInt(0)) throw new IllegalArgumentException(path + " 不能为空");
            value.fields().forEachRemaining(entry -> {
                JsonNode property = schema.path("properties").get(entry.getKey());
                if (property == null) throw new IllegalArgumentException(path + " 不支持字段：" + entry.getKey());
                validate(entry.getValue(), property, path + "." + entry.getKey());
            });
        }
        if (value.isArray()) {
            if (value.size() > schema.path("maxItems").asInt(Integer.MAX_VALUE)) throw new IllegalArgumentException(path + " 项数过多");
            value.forEach(item -> validate(item, schema.path("items"), path + "[]"));
        }
        if (value.isNumber() && ((schema.has("minimum") && value.decimalValue().compareTo(schema.get("minimum").decimalValue()) < 0)
                || (schema.has("maximum") && value.decimalValue().compareTo(schema.get("maximum").decimalValue()) > 0))) {
            throw new IllegalArgumentException(path + " 超出允许范围");
        }
        if (value.isTextual() && (value.textValue().length() < schema.path("minLength").asInt(0)
                || value.textValue().length() > schema.path("maxLength").asInt(Integer.MAX_VALUE))) {
            throw new IllegalArgumentException(path + " 长度无效");
        }
    }
}
