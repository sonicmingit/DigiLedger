package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.mapper.DictPlatformMapper;
import com.digiledger.backend.mapper.PurchaseMapper;
import com.digiledger.backend.model.dto.asset.PurchaseRequest;
import com.digiledger.backend.model.dto.asset.PurchaseUpsertRequest;
import com.digiledger.backend.model.entity.DeviceAsset;
import com.digiledger.backend.model.entity.DictPlatform;
import com.digiledger.backend.model.entity.Purchase;
import com.digiledger.backend.service.PurchaseService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class PurchaseServiceImpl implements PurchaseService {

    private final PurchaseMapper purchaseMapper;
    private final AssetMapper assetMapper;
    private final DictPlatformMapper dictPlatformMapper;
    private final ObjectMapper objectMapper;

    public PurchaseServiceImpl(PurchaseMapper purchaseMapper,
                               AssetMapper assetMapper,
                               DictPlatformMapper dictPlatformMapper,
                               ObjectMapper objectMapper) {
        this.purchaseMapper = purchaseMapper;
        this.assetMapper = assetMapper;
        this.dictPlatformMapper = dictPlatformMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public Long createPurchase(PurchaseUpsertRequest request) {
        DeviceAsset asset = Optional.ofNullable(assetMapper.findById(request.getAssetId()))
                .orElseThrow(() -> new BizException(ErrorCode.ASSET_NOT_FOUND));
        Purchase purchase = buildPurchase(request.getAssetId(), request, new HashMap<>());
        purchaseMapper.insert(purchase);
        refreshPrimaryPurchase(asset.getId());
        return purchase.getId();
    }

    @Override
    @Transactional
    public void updatePurchase(Long id, PurchaseUpsertRequest request) {
        Purchase existing = Optional.ofNullable(purchaseMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.PURCHASE_NOT_FOUND));
        if (!Objects.equals(existing.getAssetId(), request.getAssetId())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "不支持变更所属资产");
        }
        Purchase purchase = buildPurchase(existing.getAssetId(), request, new HashMap<>());
        purchase.setId(id);
        purchaseMapper.update(purchase);
        refreshPrimaryPurchase(existing.getAssetId());
    }

    @Override
    @Transactional
    public void deletePurchase(Long id) {
        Purchase existing = Optional.ofNullable(purchaseMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.PURCHASE_NOT_FOUND));
        purchaseMapper.delete(existing.getId());
        refreshPrimaryPurchase(existing.getAssetId());
    }

    private Purchase buildPurchase(Long assetId, PurchaseRequest request, Map<Long, DictPlatform> platformCache) {
        validatePurchase(request);
        Purchase purchase = new Purchase();
        purchase.setAssetId(assetId);
        purchase.setType(request.getType());
        purchase.setName(request.getName());
        DictPlatform platform = resolvePlatform(request.getPlatformId(), platformCache);
        if (platform != null) {
            purchase.setPlatformId(platform.getId());
            purchase.setPlatformName(platform.getName());
        }
        purchase.setSeller(request.getSeller());
        purchase.setPrice(request.getPrice());
        purchase.setCurrency("CNY");
        purchase.setQuantity(Optional.ofNullable(request.getQuantity()).orElse(1));
        purchase.setShippingCost(defaultZero(request.getShippingCost()));
        purchase.setPurchaseDate(request.getPurchaseDate());
        purchase.setInvoiceNo("");
        purchase.setWarrantyMonths(request.getWarrantyMonths());
        purchase.setWarrantyExpireDate(request.getWarrantyExpireDate());
        purchase.setProductLink(request.getProductLink());
        purchase.setAttachments(toJson(request.getAttachments()));
        purchase.setNotes(request.getNotes());
        return purchase;
    }

    private void refreshPrimaryPurchase(Long assetId) {
        List<Purchase> purchases = purchaseMapper.findByAssetId(assetId);
        if (purchases.isEmpty()) {
            assetMapper.updatePrimaryInfo(assetId, null, null);
            return;
        }
        Purchase primary = purchases.stream()
                .filter(p -> Objects.equals("PRIMARY", p.getType()))
                .min((a, b) -> {
                    if (a.getPurchaseDate() == null && b.getPurchaseDate() == null) {
                        return Long.compare(a.getId(), b.getId());
                    }
                    if (a.getPurchaseDate() == null) {
                        return 1;
                    }
                    if (b.getPurchaseDate() == null) {
                        return -1;
                    }
                    return a.getPurchaseDate().compareTo(b.getPurchaseDate());
                })
                .orElse(purchases.get(0));
        assetMapper.updatePrimaryInfo(assetId, primary.getId(), primary.getPurchaseDate());
    }

    private DictPlatform resolvePlatform(Long platformId, Map<Long, DictPlatform> cache) {
        if (platformId == null) {
            return null;
        }
        return cache.computeIfAbsent(platformId, id -> Optional.ofNullable(dictPlatformMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "平台不存在")));
    }

    private void validatePurchase(PurchaseRequest request) {
        if (request == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "购买信息不能为空");
        }
        String type = request.getType();
        if (type == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "购买类型不能为空");
        }
        if (List.of("ACCESSORY", "SERVICE").contains(type)
                && (request.getName() == null || request.getName().isBlank())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "配件/服务名称不能为空");
        }
    }

    private String toJson(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "JSON 序列化失败");
        }
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
