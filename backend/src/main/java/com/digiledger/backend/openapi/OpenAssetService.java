package com.digiledger.backend.openapi;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.model.dto.asset.AssetCreateRequest;
import com.digiledger.backend.service.AssetService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Set;

@Service
public class OpenAssetService {
    static final Set<String> FIELDS = Set.of("name", "categoryId", "brandId", "brand", "model", "serialNo",
            "specifications", "predecessorAssetId", "status", "purchaseDate", "retiredDate", "coverImageUrl", "relatedLinks",
            "manualUseMonths", "notes", "tagIds");
    private final AssetService assets;
    private final AssetMapper mapper;
    private final ObjectMapper json;
    private final Validator validator;
    public OpenAssetService(AssetService assets, AssetMapper mapper, ObjectMapper json, Validator validator) {
        this.assets = assets; this.mapper = mapper; this.json = json; this.validator = validator;
    }

    @Transactional
    public void patch(Long id, ObjectNode patch) {
        if (patch == null || patch.isEmpty()) throw new BizException(ErrorCode.VALIDATION_ERROR, "请提供需要修改的字段");
        patch.fieldNames().forEachRemaining(field -> {
            if (!FIELDS.contains(field)) throw new BizException(ErrorCode.VALIDATION_ERROR, "不支持修改字段：" + field);
        });
        var entity = mapper.findByIdForUpdate(id);
        if (entity == null) throw new BizException(ErrorCode.ASSET_NOT_FOUND);
        var detail = assets.getAssetDetail(id);
        ObjectNode source = json.valueToTree(entity);
        ObjectNode merged = json.createObjectNode();
        for (String field : FIELDS) if (source.has(field)) merged.set(field, source.get(field));
        // Stored related links are JSON text; API requests need the actual array.
        merged.set("relatedLinks", json.valueToTree(detail.relatedLinks()));
        merged.set("tagIds", json.valueToTree(detail.tags().stream().map(tag -> tag.id()).toList()));
        patch.fields().forEachRemaining(entry -> merged.set(entry.getKey(), entry.getValue()));
        AssetCreateRequest request = json.convertValue(merged, AssetCreateRequest.class);
        var violations = validator.validate(request);
        if (!violations.isEmpty()) throw new ConstraintViolationException(violations);
        // null purchases means preserve all existing financial records in AssetService.
        assets.updateAsset(id, request);
    }
}
