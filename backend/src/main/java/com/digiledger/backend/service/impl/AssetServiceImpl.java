package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.mapper.AssetTagMapMapper;
import com.digiledger.backend.mapper.DictBrandMapper;
import com.digiledger.backend.mapper.DictCategoryMapper;
import com.digiledger.backend.mapper.DictPlatformMapper;
import com.digiledger.backend.mapper.DictTagMapper;
import com.digiledger.backend.mapper.PurchaseMapper;
import com.digiledger.backend.mapper.SaleMapper;
import com.digiledger.backend.model.dto.asset.*;
import com.digiledger.backend.model.dto.dict.BrandDTO;
import com.digiledger.backend.model.entity.AssetTagMap;
import com.digiledger.backend.model.entity.DeviceAsset;
import com.digiledger.backend.model.entity.DictBrand;
import com.digiledger.backend.model.entity.DictCategory;
import com.digiledger.backend.model.entity.DictBrand;
import com.digiledger.backend.model.entity.DictPlatform;
import com.digiledger.backend.model.entity.DictTag;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
    private final DictCategoryMapper dictCategoryMapper;
    private final DictBrandMapper dictBrandMapper;
    private final DictPlatformMapper dictPlatformMapper;
    private final DictTagMapper dictTagMapper;
    private final AssetTagMapMapper assetTagMapMapper;
    private final ObjectMapper objectMapper;

    public AssetServiceImpl(AssetMapper assetMapper,
                            PurchaseMapper purchaseMapper,
                            SaleMapper saleMapper,
                            DictCategoryMapper dictCategoryMapper,
                            DictBrandMapper dictBrandMapper,
                            DictPlatformMapper dictPlatformMapper,
                            DictTagMapper dictTagMapper,
                            AssetTagMapMapper assetTagMapMapper,
                            ObjectMapper objectMapper) {
        this.assetMapper = assetMapper;
        this.purchaseMapper = purchaseMapper;
        this.saleMapper = saleMapper;
        this.dictCategoryMapper = dictCategoryMapper;
        this.dictBrandMapper = dictBrandMapper;
        this.dictPlatformMapper = dictPlatformMapper;
        this.dictTagMapper = dictTagMapper;
        this.assetTagMapMapper = assetTagMapMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<AssetSummaryDTO> listAssets(String status, String keyword, Long categoryId, Long platformId, List<Long> tagIds) {
        if (categoryId != null) {
            Optional.ofNullable(dictCategoryMapper.findById(categoryId))
                    .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "类别不存在"));
        }
        if (platformId != null) {
            Optional.ofNullable(dictPlatformMapper.findById(platformId))
                    .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "平台不存在"));
        }
        List<Long> normalizedTagIds = tagIds == null ? null : validateTagIds(tagIds);
        String categoryLike = buildCategoryLikePattern(categoryId, true);
        String categorySuffix = buildCategoryLikePattern(categoryId, false);
        List<DeviceAsset> assets = assetMapper.findAll(status, keyword, categoryId, categoryLike, categorySuffix,
                platformId, normalizedTagIds, normalizedTagIds == null ? null : normalizedTagIds.size());
        return assets.stream()
                .map(asset -> {
                    List<Purchase> purchases = purchaseMapper.findByAssetId(asset.getId());
                    List<Sale> sales = saleMapper.findByAssetId(asset.getId());
                    List<TagDTO> tags = toTagDTOs(dictTagMapper.findByAssetId(asset.getId()));
                    return buildSummary(asset, purchases, sales, tags);
                })
                .collect(Collectors.toList());
    }

    @Override
    public AssetDetailDTO getAssetDetail(Long id) {
        DeviceAsset asset = Optional.ofNullable(assetMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.ASSET_NOT_FOUND));
        List<Purchase> purchases = purchaseMapper.findByAssetId(id);
        List<Sale> sales = saleMapper.findByAssetId(id);
        List<TagDTO> tags = toTagDTOs(dictTagMapper.findByAssetId(id));
        DictBrand brand = asset.getBrandId() == null ? null : dictBrandMapper.findById(asset.getBrandId());

        AssetMetrics metrics = calculateMetrics(asset, purchases, sales);
        return new AssetDetailDTO(
                asset.getId(),
                asset.getName(),
                asset.getCategoryId(),
                asset.getCategoryPath(),
                toBrandDTO(brand, asset.getBrand()),
                asset.getModel(),
                asset.getSerialNo(),
                asset.getStatus(),
                asset.getPurchaseDate(),
                asset.getEnabledDate(),
                asset.getRetiredDate(),
                asset.getCoverImageUrl(),
                asset.getNotes(),
                tags,
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
        Map<Long, DictCategory> categoryMap = loadCategoryMap();
        DictCategory category = resolveCategory(request.getCategoryId(), categoryMap);
        String categoryPath = buildCategoryPath(category.getId(), categoryMap);
        DictBrand brand = resolveBrand(request.getBrandId());
        List<Long> tagIds = validateTagIds(request.getTagIds());
        DictBrand brand = resolveBrand(request.getBrandId());
        DeviceAsset asset = buildDeviceAsset(request, category, categoryPath, brand);
        assetMapper.insert(asset);
        persistTags(asset.getId(), tagIds);
        persistPurchases(asset.getId(), request.getPurchases(), new HashMap<>());
        refreshPrimaryPurchase(asset.getId());
        return asset.getId();
    }

    @Override
    @Transactional
    public void updateAsset(Long id, AssetCreateRequest request) {
        Optional.ofNullable(assetMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.ASSET_NOT_FOUND));
        validateDates(request);
        Map<Long, DictCategory> categoryMap = loadCategoryMap();
        DictCategory category = resolveCategory(request.getCategoryId(), categoryMap);
        String categoryPath = buildCategoryPath(category.getId(), categoryMap);
        DictBrand brand = resolveBrand(request.getBrandId());
        List<Long> tagIds = validateTagIds(request.getTagIds());
        DictBrand brand = resolveBrand(request.getBrandId());
        DeviceAsset asset = buildDeviceAsset(request, category, categoryPath, brand);
        asset.setId(id);
        assetMapper.update(asset);
        persistTags(id, tagIds);
        purchaseMapper.deleteByAsset(id);
        persistPurchases(id, request.getPurchases(), new HashMap<>());
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
        Map<Long, DictPlatform> platformCache = new HashMap<>();
        DictPlatform salePlatform = resolvePlatform(request.getPlatformId(), platformCache);
        Sale sale = new Sale();
        sale.setAssetId(id);
        if (salePlatform != null) {
            sale.setPlatformId(salePlatform.getId());
            sale.setPlatformName(salePlatform.getName());
        }
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

    @Override
    @Transactional
    public void updateAssetStatus(Long id, String status) {
        if (!VALID_STATUSES.contains(status)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "资产状态非法");
        }
        Optional.ofNullable(assetMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.ASSET_NOT_FOUND));
        assetMapper.updateStatus(id, status);
    }

    private void validateDates(AssetCreateRequest request) {
        LocalDate purchaseDate = request.getPurchaseDate();
        LocalDate enabledDate = request.getEnabledDate();
        if (enabledDate == null) {
            enabledDate = purchaseDate;
            request.setEnabledDate(enabledDate);
        }
        if (enabledDate == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "启用日期不能为空");
        }
        if (purchaseDate != null && enabledDate.isBefore(purchaseDate)) {
            throw new BizException(ErrorCode.DATE_RANGE_CONFLICT, "启用日期不能早于购买日期");
        }
        if (request.getRetiredDate() != null && request.getRetiredDate().isBefore(enabledDate)) {
            throw new BizException(ErrorCode.DATE_RANGE_CONFLICT, "报废日期不能早于启用日期");
        }
        if (request.getPurchases() != null) {
            request.getPurchases().forEach(purchase -> {
                if (enabledDate != null && purchase.getPurchaseDate() != null
                        && enabledDate.isBefore(purchase.getPurchaseDate())) {
                    throw new BizException(ErrorCode.DATE_RANGE_CONFLICT, "启用日期需晚于购买日期");
                }
            });
        }
    }

    private DeviceAsset buildDeviceAsset(AssetCreateRequest request, DictCategory category, String categoryPath, DictBrand brandDict) {
    private DeviceAsset buildDeviceAsset(AssetCreateRequest request, DictCategory category, String categoryPath, DictBrand brand) {
        if (!VALID_STATUSES.contains(request.getStatus())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "资产状态非法");
        }
        DeviceAsset asset = new DeviceAsset();
        asset.setName(request.getName());
        asset.setCategoryId(category != null ? category.getId() : null);
        asset.setCategoryPath(categoryPath);
        if (brandDict != null) {
            asset.setBrandId(brandDict.getId());
            asset.setBrand(brandDict.getName());
        } else {
            asset.setBrandId(request.getBrandId());
            asset.setBrand(request.getBrand());
        }
        asset.setBrandId(brand != null ? brand.getId() : null);
        asset.setBrand(determineBrandName(request.getBrand(), brand));
        asset.setModel(request.getModel());
        asset.setSerialNo(request.getSerialNo());
        asset.setStatus(request.getStatus());
        asset.setPurchaseDate(request.getPurchaseDate());
        asset.setEnabledDate(request.getEnabledDate());
        asset.setRetiredDate(request.getRetiredDate());
        asset.setCoverImageUrl(request.getCoverImageUrl());
        asset.setNotes(request.getNotes());
        return asset;
    }

    private void persistPurchases(Long assetId, List<PurchaseRequest> purchaseRequests, Map<Long, DictPlatform> platformCache) {
        if (purchaseRequests == null || purchaseRequests.isEmpty()) {
            return;
        }
        for (PurchaseRequest request : purchaseRequests) {
            validatePurchaseName(request);
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
            purchase.setCurrency(Optional.ofNullable(request.getCurrency()).filter(s -> !s.isBlank()).orElse("CNY"));
            purchase.setQuantity(Optional.ofNullable(request.getQuantity()).orElse(1));
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

    private AssetSummaryDTO buildSummary(DeviceAsset asset, List<Purchase> purchases, List<Sale> sales, List<TagDTO> tags) {
        AssetMetrics metrics = calculateMetrics(asset, purchases, sales);
        return new AssetSummaryDTO(
                asset.getId(),
                asset.getName(),
                asset.getCategoryId(),
                asset.getCategoryPath(),
                asset.getStatus(),
                asset.getCoverImageUrl(),
                metrics.totalInvest,
                metrics.avgCostPerDay,
                metrics.useDays,
                metrics.lastNetIncome,
                asset.getEnabledDate(),
                asset.getPurchaseDate(),
                tags
        );
    }

    private BrandDTO toBrandDTO(DictBrand brand, String fallbackName) {
        if (brand == null) {
            if (fallbackName == null || fallbackName.isBlank()) {
                return null;
            }
            return new BrandDTO(null, fallbackName, null, null, null);
        }
        return new BrandDTO(brand.getId(), brand.getName(), brand.getAlias(), brand.getInitial(), brand.getSort());
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

    private List<String> parseStringList(String jsonArray) {
        if (jsonArray == null || jsonArray.isBlank()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(jsonArray, objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
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
                purchase.getName(),
                purchase.getPlatformId(),
                purchase.getPlatformName(),
                purchase.getSeller(),
                purchase.getPrice(),
                purchase.getShippingCost(),
                purchase.getCurrency(),
                purchase.getQuantity(),
                purchase.getPurchaseDate(),
                purchase.getInvoiceNo(),
                purchase.getWarrantyMonths(),
                purchase.getWarrantyExpireDate(),
                parseStringList(purchase.getAttachments()),
                purchase.getNotes()
        );
    }

    private SaleDTO toSaleDTO(Sale sale) {
        if (sale == null) {
            return null;
        }
        return new SaleDTO(
                sale.getId(),
                sale.getPlatformId(),
                sale.getPlatformName(),
                sale.getBuyer(),
                sale.getSalePrice(),
                sale.getFee(),
                sale.getShippingCost(),
                sale.getOtherCost(),
                sale.getNetIncome(),
                sale.getSaleDate(),
                parseStringList(sale.getAttachments()),
                sale.getNotes()
        );
    }

    private Map<Long, DictCategory> loadCategoryMap() {
        return dictCategoryMapper.findAll().stream()
                .collect(Collectors.toMap(DictCategory::getId, category -> category));
    }

    private DictCategory resolveCategory(Long categoryId, Map<Long, DictCategory> categoryMap) {
        if (categoryId == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "类别不能为空");
        }
        DictCategory category = categoryMap.get(categoryId);
        if (category == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "类别不存在");
        }
        return category;
    }

    private DictBrand resolveBrand(Long brandId) {
        if (brandId == null) {
            return null;
        }
        return Optional.ofNullable(dictBrandMapper.findById(brandId))
                .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "品牌不存在"));
    }

    private String determineBrandName(String customName, DictBrand brand) {
        if (customName != null && !customName.isBlank()) {
            return customName;
        }
        return brand != null ? brand.getName() : null;
    }

    private String buildCategoryPath(Long categoryId, Map<Long, DictCategory> categoryMap) {
        List<Long> path = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        DictCategory current = categoryMap.get(categoryId);
        while (current != null) {
            if (!visited.add(current.getId())) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "类别层级存在循环");
            }
            path.add(current.getId());
            Long parentId = current.getParentId();
            if (parentId == null) {
                break;
            }
            current = categoryMap.get(parentId);
            if (current == null) {
                current = Optional.ofNullable(dictCategoryMapper.findById(parentId))
                        .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "类别层级不完整"));
                categoryMap.put(current.getId(), current);
            }
        }
        Collections.reverse(path);
        return path.stream().map(String::valueOf).collect(Collectors.joining("/", "/", ""));
    }

    private List<Long> validateTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinct = tagIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (distinct.isEmpty()) {
            return List.of();
        }
        List<DictTag> tags = dictTagMapper.findByIds(distinct);
        if (tags.size() != distinct.size()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "存在无效的标签ID");
        }
        return distinct;
    }

    private void persistTags(Long assetId, List<Long> tagIds) {
        assetTagMapMapper.deleteByAssetId(assetId);
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<AssetTagMap> mappings = tagIds.stream()
                .map(tagId -> {
                    AssetTagMap map = new AssetTagMap();
                    map.setAssetId(assetId);
                    map.setTagId(tagId);
                    return map;
                })
                .toList();
        assetTagMapMapper.batchInsert(mappings);
    }

    private void validatePurchase(PurchaseRequest request) {
        if (request == null) {
            return;
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

    private DictPlatform resolvePlatform(Long platformId, Map<Long, DictPlatform> platformCache) {
        if (platformId == null) {
            return null;
        }
        return platformCache.computeIfAbsent(platformId, id -> Optional.ofNullable(dictPlatformMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "平台不存在")));
    }

    private DictBrand resolveBrand(Long brandId) {
        if (brandId == null) {
            return null;
        }
        return Optional.ofNullable(dictBrandMapper.findById(brandId))
                .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "品牌不存在"));
    }

    private void validatePurchaseName(PurchaseRequest request) {
        if (request == null) {
            return;
        }
        String type = request.getType();
        if (type == null) {
            return;
        }
        if (List.of("ACCESSORY", "SERVICE").contains(type) && (request.getName() == null || request.getName().isBlank())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "配件/服务名称必填");
        }
    }

    private String buildCategoryLikePattern(Long categoryId, boolean includeDescendants) {
        if (categoryId == null) {
            return null;
        }
        if (includeDescendants) {
            return "%/" + categoryId + "/%";
        }
        return "%/" + categoryId;
    }

    private List<TagDTO> toTagDTOs(List<DictTag> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        return tags.stream()
                .map(tag -> new TagDTO(tag.getId(), tag.getName(), tag.getColor(), tag.getIcon()))
                .toList();
    }

    private BrandDTO toBrandDTO(DictBrand brand, String fallbackName) {
        if (brand != null) {
            return new BrandDTO(brand.getId(), brand.getName(), brand.getAlias(), brand.getInitial(), brand.getSort());
        }
        if (fallbackName != null && !fallbackName.isBlank()) {
            return new BrandDTO(null, fallbackName, null, null, null);
        }
        return null;
    }

    private BigDecimal defaultZero(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    /** 内部指标封装 */
    private record AssetMetrics(BigDecimal totalInvest, long useDays, BigDecimal avgCostPerDay, BigDecimal lastNetIncome) {
    }
}
