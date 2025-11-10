package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.mapper.PurchaseMapper;
import com.digiledger.backend.mapper.SaleMapper;
import com.digiledger.backend.model.dto.asset.*;
import com.digiledger.backend.model.entity.DeviceAsset;
import com.digiledger.backend.model.entity.Purchase;
import com.digiledger.backend.model.entity.Sale;
import com.digiledger.backend.service.AssetService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 资产服务实现，涵盖 CRUD、指标计算与售出向导。
 */
@Service
public class AssetServiceImpl implements AssetService {

    private static final List<String> VALID_STATUSES = List.of("使用中", "已闲置", "待出售", "已出售", "已丢弃");

    private final AssetMapper assetMapper;
    private final PurchaseMapper purchaseMapper;
    private final SaleMapper saleMapper;
    private final ObjectMapper objectMapper;

    public AssetServiceImpl(AssetMapper assetMapper,
                            PurchaseMapper purchaseMapper,
                            SaleMapper saleMapper,
                            ObjectMapper objectMapper) {
        this.assetMapper = assetMapper;
        this.purchaseMapper = purchaseMapper;
        this.saleMapper = saleMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AssetSummaryDTO> listAssets(String status, String keyword) {
        return assetMapper.findAll(status, keyword).stream()
                .map(asset -> {
                    List<Purchase> purchases = purchaseMapper.findByAssetId(asset.getId());
                    List<Sale> sales = saleMapper.findByAssetId(asset.getId());
                    return buildSummary(asset, purchases, sales);
                })
                .collect(Collectors.toList());
    }

    @Override
    public AssetDetailDTO getAssetDetail(Long id) {
        DeviceAsset asset = Optional.ofNullable(assetMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.ASSET_NOT_FOUND));
        List<Purchase> purchases = purchaseMapper.findByAssetId(id);
        List<Sale> sales = saleMapper.findByAssetId(id);

        AssetMetrics metrics = calculateMetrics(asset, purchases, sales);
        return new AssetDetailDTO(
                asset.getId(),
                asset.getName(),
                asset.getCategory(),
                asset.getBrand(),
                asset.getModel(),
                asset.getSerialNo(),
                asset.getStatus(),
                asset.getPurchaseDate(),
                asset.getEnabledDate(),
                asset.getRetiredDate(),
                asset.getCoverImageUrl(),
                asset.getNotes(),
                parseTags(asset.getTags()),
                metrics.totalInvest,
                metrics.useDays,
                metrics.avgCostPerDay,
                metrics.lastNetIncome,
                purchases.stream().map(this::toPurchaseDTO).toList(),
                sales.stream().map(this::toSaleDTO).toList()
        );
    }

    @Override
    @Transactional
    public Long createAsset(AssetCreateRequest request) {
        validateDates(request);
        DeviceAsset asset = buildDeviceAsset(request);
        assetMapper.insert(asset);
        persistPurchases(asset.getId(), request.getPurchases());
        refreshPrimaryPurchase(asset.getId());
        return asset.getId();
    }

    @Override
    @Transactional
    public void updateAsset(Long id, AssetCreateRequest request) {
        DeviceAsset existing = Optional.ofNullable(assetMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.ASSET_NOT_FOUND));
        validateDates(request);
        DeviceAsset asset = buildDeviceAsset(request);
        asset.setId(id);
        assetMapper.update(asset);
        purchaseMapper.deleteByAsset(id);
        persistPurchases(id, request.getPurchases());
        refreshPrimaryPurchase(id);
        // 状态变化若为已丢弃则重置售出记录
        if ("已丢弃".equals(asset.getStatus())) {
            saleMapper.deleteByAsset(id);
        }
    }

    @Override
    @Transactional
    public void deleteAsset(Long id) {
        DeviceAsset asset = Optional.ofNullable(assetMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.ASSET_NOT_FOUND));
        if (purchaseMapper.countByAsset(id) > 0 || saleMapper.countByAsset(id) > 0) {
            throw new BizException(ErrorCode.ASSET_DELETE_CONFLICT);
        }
        assetMapper.delete(asset.getId());
    }

    @Override
    @Transactional
    public SaleDTO sellAsset(Long id, AssetSellRequest request) {
        DeviceAsset asset = Optional.ofNullable(assetMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.ASSET_NOT_FOUND));
        LocalDate enabledDate = Optional.ofNullable(asset.getEnabledDate())
                .orElse(Optional.ofNullable(asset.getPurchaseDate()).orElse(request.getSaleDate()));
        if (request.getSaleDate().isBefore(enabledDate)) {
            throw new BizException(ErrorCode.DATE_RANGE_CONFLICT, "售出日期不能早于启用日期");
        }
        if (!List.of("待出售", "已闲置", "使用中").contains(asset.getStatus())) {
            throw new BizException(ErrorCode.SALE_STATUS_CONFLICT, "资产状态不允许售出");
        }
        Sale sale = new Sale();
        sale.setAssetId(id);
        sale.setPlatform(request.getPlatform());
        sale.setBuyer(request.getBuyer());
        sale.setSalePrice(request.getSalePrice());
        sale.setFee(defaultZero(request.getFee()));
        sale.setShippingCost(defaultZero(request.getShippingCost()));
        sale.setOtherCost(defaultZero(request.getOtherCost()));
        sale.setNetIncome(request.getSalePrice()
                .subtract(defaultZero(request.getFee()))
                .subtract(defaultZero(request.getShippingCost()))
                .subtract(defaultZero(request.getOtherCost())));
        sale.setSaleDate(request.getSaleDate());
        sale.setAttachments(toJson(request.getAttachments()));
        sale.setNotes(request.getNotes());
        saleMapper.insert(sale);
        asset.setStatus("已出售");
        asset.setRetiredDate(request.getSaleDate());
        assetMapper.update(asset);
        return toSaleDTO(saleMapper.findLatestByAsset(id));
    }

    private void validateDates(AssetCreateRequest request) {
        if (request.getPurchaseDate() != null && request.getEnabledDate() != null
                && request.getEnabledDate().isBefore(request.getPurchaseDate())) {
            throw new BizException(ErrorCode.DATE_RANGE_CONFLICT, "启用日期不能早于购买日期");
        }
        if (request.getRetiredDate() != null && request.getEnabledDate() != null
                && request.getRetiredDate().isBefore(request.getEnabledDate())) {
            throw new BizException(ErrorCode.DATE_RANGE_CONFLICT, "报废日期不能早于启用日期");
        }
        if (request.getPurchases() != null) {
            request.getPurchases().forEach(purchase -> {
                if (request.getEnabledDate() != null && purchase.getPurchaseDate() != null
                        && request.getEnabledDate().isBefore(purchase.getPurchaseDate())) {
                    throw new BizException(ErrorCode.DATE_RANGE_CONFLICT, "启用日期需晚于购买日期");
                }
            });
        }
    }

    private DeviceAsset buildDeviceAsset(AssetCreateRequest request) {
        if (!VALID_STATUSES.contains(request.getStatus())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "资产状态非法");
        }
        DeviceAsset asset = new DeviceAsset();
        asset.setName(request.getName());
        asset.setCategory(request.getCategory());
        asset.setBrand(request.getBrand());
        asset.setModel(request.getModel());
        asset.setSerialNo(request.getSerialNo());
        asset.setStatus(request.getStatus());
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setEnabledDate(request.getEnabledDate());
        asset.setRetiredDate(request.getRetiredDate());
        asset.setCoverImageUrl(request.getCoverImageUrl());
        asset.setNotes(request.getNotes());
        asset.setTags(toJson(request.getTags()));
        return asset;
    }

    private void persistPurchases(Long assetId, List<PurchaseRequest> purchaseRequests) {
        if (purchaseRequests == null || purchaseRequests.isEmpty()) {
            return;
        }
        for (PurchaseRequest request : purchaseRequests) {
            Purchase purchase = new Purchase();
            purchase.setAssetId(assetId);
            purchase.setType(request.getType());
            purchase.setPlatform(request.getPlatform());
            purchase.setSeller(request.getSeller());
            purchase.setPrice(request.getPrice());
            purchase.setCurrency(request.getCurrency());
            purchase.setQuantity(request.getQuantity());
            purchase.setShippingCost(defaultZero(request.getShippingCost()));
            purchase.setPurchaseDate(request.getPurchaseDate());
            purchase.setInvoiceNo(request.getInvoiceNo());
            purchase.setWarrantyMonths(request.getWarrantyMonths());
            purchase.setWarrantyExpireDate(request.getWarrantyExpireDate());
            purchase.setAttachments(toJson(request.getAttachments()));
            purchase.setNotes(request.getNotes());
            purchaseMapper.insert(purchase);
        }
    }

    private void refreshPrimaryPurchase(Long assetId) {
        List<Purchase> purchases = purchaseMapper.findByAssetId(assetId);
        if (purchases.isEmpty()) {
            assetMapper.updatePrimaryInfo(assetId, null, null);
            return;
        }
        Purchase primary = purchases.stream()
                .filter(p -> Objects.equals("PRIMARY", p.getType()))
                .min(Comparator.comparing(Purchase::getPurchaseDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(purchases.get(0));
        assetMapper.updatePrimaryInfo(assetId, primary.getId(), primary.getPurchaseDate());
    }

    private AssetSummaryDTO buildSummary(DeviceAsset asset, List<Purchase> purchases, List<Sale> sales) {
        AssetMetrics metrics = calculateMetrics(asset, purchases, sales);
        return new AssetSummaryDTO(
                asset.getId(),
                asset.getName(),
                asset.getCategory(),
                asset.getStatus(),
                asset.getCoverImageUrl(),
                metrics.totalInvest,
                metrics.avgCostPerDay,
                metrics.useDays,
                metrics.lastNetIncome,
                asset.getEnabledDate(),
                asset.getPurchaseDate(),
                parseTags(asset.getTags())
        );
    }

    private AssetMetrics calculateMetrics(DeviceAsset asset, List<Purchase> purchases, List<Sale> sales) {
        BigDecimal totalInvest = purchases.stream()
                .map(p -> p.getPrice().add(Optional.ofNullable(p.getShippingCost()).orElse(BigDecimal.ZERO)))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long useDays = calculateUseDays(asset, sales);
        BigDecimal avgCost = useDays == 0 ? BigDecimal.ZERO : totalInvest
                .divide(BigDecimal.valueOf(useDays), 2, RoundingMode.HALF_UP);
        Sale latestSale = sales.stream().max(Comparator.comparing(Sale::getSaleDate)).orElse(null);
        BigDecimal netIncome = latestSale != null ? Optional.ofNullable(latestSale.getNetIncome()).orElse(BigDecimal.ZERO) : BigDecimal.ZERO;
        return new AssetMetrics(totalInvest.setScale(2, RoundingMode.HALF_UP), useDays, avgCost, netIncome);
    }

    private long calculateUseDays(DeviceAsset asset, List<Sale> sales) {
        LocalDate start = Optional.ofNullable(asset.getEnabledDate())
                .orElse(Optional.ofNullable(asset.getPurchaseDate()).orElse(LocalDate.now()));
        LocalDate end = switch (asset.getStatus()) {
            case "已出售" -> sales.stream().max(Comparator.comparing(Sale::getSaleDate))
                    .map(Sale::getSaleDate).orElse(LocalDate.now());
            case "已丢弃" -> Optional.ofNullable(asset.getRetiredDate()).orElse(LocalDate.now());
            default -> LocalDate.now();
        };
        long days = ChronoUnit.DAYS.between(start, end);
        return Math.max(days, 1);
    }

    private List<String> parseTags(String tagsJson) {
        if (tagsJson == null || tagsJson.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(tagsJson, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
        } catch (JsonProcessingException e) {
            return Collections.emptyList();
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

    private PurchaseDTO toPurchaseDTO(Purchase purchase) {
        return new PurchaseDTO(
                purchase.getId(),
                purchase.getType(),
                purchase.getPlatform(),
                purchase.getSeller(),
                purchase.getPrice(),
                purchase.getShippingCost(),
                purchase.getCurrency(),
                purchase.getQuantity(),
                purchase.getPurchaseDate(),
                purchase.getInvoiceNo(),
                purchase.getWarrantyMonths(),
                purchase.getWarrantyExpireDate(),
                parseTags(purchase.getAttachments()),
                purchase.getNotes()
        );
    }

    private SaleDTO toSaleDTO(Sale sale) {
        if (sale == null) {
            return null;
        }
        return new SaleDTO(
                sale.getId(),
                sale.getPlatform(),
                sale.getBuyer(),
                sale.getSalePrice(),
                sale.getFee(),
                sale.getShippingCost(),
                sale.getOtherCost(),
                sale.getNetIncome(),
                sale.getSaleDate(),
                parseTags(sale.getAttachments()),
                sale.getNotes()
        );
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 内部指标封装 */
    private record AssetMetrics(BigDecimal totalInvest, long useDays, BigDecimal avgCostPerDay, BigDecimal lastNetIncome) {
    }
}
