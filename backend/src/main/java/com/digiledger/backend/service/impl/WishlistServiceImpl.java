package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.mapper.DictBrandMapper;
import com.digiledger.backend.mapper.DictCategoryMapper;
import com.digiledger.backend.mapper.DictTagMapper;
import com.digiledger.backend.mapper.WishlistMapper;
import com.digiledger.backend.mapper.EquipUpgradeNodeMapper;
import com.digiledger.backend.mapper.WishlistTagMapMapper;
import com.digiledger.backend.mapper.WishlistPriceHistoryMapper;
import com.digiledger.backend.model.dto.asset.AssetCreateRequest;
import com.digiledger.backend.model.dto.asset.TagDTO;
import com.digiledger.backend.model.dto.wishlist.WishlistAssetRefDTO;
import com.digiledger.backend.model.dto.wishlist.WishlistDTO;
import com.digiledger.backend.model.dto.wishlist.WishlistRequest;
import com.digiledger.backend.model.entity.DeviceAsset;
import com.digiledger.backend.model.entity.DictCategory;
import com.digiledger.backend.model.entity.DictTag;
import com.digiledger.backend.model.entity.WishlistItem;
import com.digiledger.backend.model.entity.WishlistTagMap;
import com.digiledger.backend.model.entity.WishlistPriceHistory;
import com.digiledger.backend.model.dto.wishlist.WishlistPriceRequest;
import com.digiledger.backend.model.dto.wishlist.WishlistPriceHistoryDTO;
import com.digiledger.backend.service.AssetService;
import com.digiledger.backend.service.WishlistService;
import com.digiledger.backend.util.StoragePathHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;

/**
 * 心愿单服务实现，可转化资产。
 */
@Service
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("未购买", "已购买");

    private final WishlistMapper wishlistMapper;
    private final EquipUpgradeNodeMapper upgradeNodeMapper;
    private final DictCategoryMapper dictCategoryMapper;
    private final DictBrandMapper dictBrandMapper;
    private final DictTagMapper dictTagMapper;
    private final AssetMapper assetMapper;
    private final WishlistTagMapMapper wishlistTagMapMapper;
    private final AssetService assetService;
    private final StoragePathHelper storagePathHelper;
    private final WishlistPriceHistoryMapper priceHistoryMapper;

    public WishlistServiceImpl(WishlistMapper wishlistMapper,
                               DictCategoryMapper dictCategoryMapper,
                               DictBrandMapper dictBrandMapper,
                               DictTagMapper dictTagMapper,
                               WishlistTagMapMapper wishlistTagMapMapper,
                               AssetService assetService,
                               StoragePathHelper storagePathHelper,
                               AssetMapper assetMapper,
                               WishlistPriceHistoryMapper priceHistoryMapper,
                               EquipUpgradeNodeMapper upgradeNodeMapper) {
        this.wishlistMapper = wishlistMapper;
        this.upgradeNodeMapper = upgradeNodeMapper;
        this.dictCategoryMapper = dictCategoryMapper;
        this.dictBrandMapper = dictBrandMapper;
        this.dictTagMapper = dictTagMapper;
        this.wishlistTagMapMapper = wishlistTagMapMapper;
        this.assetService = assetService;
        this.storagePathHelper = storagePathHelper;
        this.assetMapper = assetMapper;
        this.priceHistoryMapper = priceHistoryMapper;
    }

    @Override
    public List<WishlistDTO> listAll(String status) {
        String normalizedStatus = normalizeStatus(status);
        List<WishlistItem> items = wishlistMapper.findAll(normalizedStatus);
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, DictCategory> categoryMap = loadCategoryMap();
        Map<Long, DeviceAsset> assetCache = loadAssetMap(items.stream()
                .map(WishlistItem::getConvertedAssetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toList()));
        Map<Long, String> brandNameCache = new HashMap<>();
        Map<Long, List<Long>> tagIdMap = loadWishlistTagIds(items.stream()
                .map(WishlistItem::getId)
                .collect(Collectors.toList()));
        Map<Long, DictTag> tagCache = loadTagCache(tagIdMap.values());
        return items.stream()
                .map(item -> toDto(
                        item,
                        resolveBrandName(item.getBrandId(), brandNameCache),
                        resolveCategoryName(item.getCategoryId(), categoryMap),
                        toTagDTOs(tagIdMap.get(item.getId()), tagCache),
                        resolveAssetRefs(item, assetCache)))
                .collect(Collectors.toList());
    }

    @Override
    public WishlistDTO getById(Long id) {
        WishlistItem item = Optional.ofNullable(wishlistMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.WISHLIST_NOT_FOUND));
        Map<Long, DictCategory> categoryMap = loadCategoryMap();
        Map<Long, List<Long>> tagIdMap = loadWishlistTagIds(List.of(item.getId()));
        Map<Long, DictTag> tagCache = loadTagCache(tagIdMap.values());
        Map<Long, DeviceAsset> assetCache = loadAssetMap(item.getConvertedAssetId() == null
                ? List.of()
                : List.of(item.getConvertedAssetId()));
        return toDto(
                item,
                resolveBrandName(item.getBrandId(), new HashMap<>()),
                resolveCategoryName(item.getCategoryId(), categoryMap),
                toTagDTOs(tagIdMap.get(item.getId()), tagCache),
                resolveAssetRefs(item, assetCache));
    }

    @Override
    @Transactional
    public Long create(WishlistRequest request) {
        validateReferences(request);
        List<Long> tagIds = validateTagIds(request.getTagIds());
        WishlistItem item = buildEntity(request, "未购买");
        wishlistMapper.insert(item);
        // 新建时的首次关注价也作为价格曲线的起点，避免历史列表从第二次更新才开始。
        if (item.getCurrentPrice() != null) {
            WishlistPriceHistory history = new WishlistPriceHistory();
            history.setWishlistId(item.getId());
            history.setPrice(item.getCurrentPrice());
            history.setCapturedAt(item.getLastPriceAt());
            priceHistoryMapper.insert(history);
        }
        persistTags(item.getId(), tagIds);
        return item.getId();
    }

    @Override
    @Transactional
    public void update(Long id, WishlistRequest request) {
        WishlistItem exist = Optional.ofNullable(wishlistMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.WISHLIST_NOT_FOUND));
        validateReferences(request);
        List<Long> tagIds = validateTagIds(request.getTagIds());
        String status = Optional.ofNullable(exist.getStatus()).orElse("未购买");
        validateStatus(status);
        WishlistItem update = buildEntity(request, status);
        update.setId(id);
        update.setConvertedAssetId(exist.getConvertedAssetId());
        wishlistMapper.update(update);
        persistTags(id, tagIds);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        getById(id);
        wishlistMapper.delete(id);
    }

    @Override
    @Transactional
    public Long convertToAsset(Long id, AssetCreateRequest request) {
        WishlistItem item = Optional.ofNullable(wishlistMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.WISHLIST_NOT_FOUND));
        if (item.getConvertedAssetId() != null) {
            return item.getConvertedAssetId();
        }
        if (request.getName() == null || request.getName().isBlank()) {
            request.setName(item.getName());
        }
        if (request.getCategoryId() == null) {
            request.setCategoryId(item.getCategoryId());
        }
        if (request.getBrandId() == null) {
            request.setBrandId(item.getBrandId());
        }
        if (request.getModel() == null || request.getModel().isBlank()) {
            request.setModel(item.getModel());
        }
        if (request.getNotes() == null || request.getNotes().isBlank()) {
            request.setNotes(item.getNotes());
        }
        if (request.getCategoryId() == null) {
            request.setCategoryId(item.getCategoryId());
        }
        if (request.getBrandId() == null) {
            request.setBrandId(item.getBrandId());
        }
        if (request.getCoverImageUrl() == null) {
            request.setCoverImageUrl(storagePathHelper.toStoredReference(item.getImageUrl()));
        }
        if (request.getStatus() == null || request.getStatus().isBlank()) {
            request.setStatus("使用中");
        }
        Long assetId = assetService.createAsset(request);
        wishlistMapper.markConverted(id, assetId);
        // 路线节点保留原有 ID 与关系，心愿购买完成后仅替换其数据来源。
        upgradeNodeMapper.bindWishlistNodesToAsset(id, assetId);
        return assetId;
    }

    @Override
    @Transactional
    public void updatePrice(Long id, WishlistPriceRequest request) {
        Optional.ofNullable(wishlistMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.WISHLIST_NOT_FOUND));
        LocalDateTime capturedAt = request.capturedAt() == null
                ? LocalDateTime.now()
                : request.capturedAt().toLocalDateTime();
        WishlistPriceHistory history = new WishlistPriceHistory();
        history.setWishlistId(id);
        history.setPrice(request.currentPrice());
        history.setCapturedAt(capturedAt);
        // 历史记录先落库，再同步当前值；事务确保两者不会出现半成功状态。
        priceHistoryMapper.insert(history);
        wishlistMapper.updateCurrentPrice(id, request.currentPrice(), capturedAt);
    }

    @Override
    public List<WishlistPriceHistoryDTO> getPriceHistory(Long id) {
        WishlistItem item = Optional.ofNullable(wishlistMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.WISHLIST_NOT_FOUND));
        List<WishlistPriceHistoryDTO> history = priceHistoryMapper.findByWishlistId(id).stream()
                .map(point -> new WishlistPriceHistoryDTO(point.getPrice(), point.getCapturedAt()))
                .toList();
        // 兼容旧数据：曾只保存“当前价”而未落价格历史时，也能看到首次价格。
        if (history.isEmpty() && item.getCurrentPrice() != null) {
            return List.of(new WishlistPriceHistoryDTO(item.getCurrentPrice(),
                    Optional.ofNullable(item.getLastPriceAt()).orElse(item.getCreatedAt())));
        }
        return history;
    }

    @Override
    @Transactional
    public Long markPurchased(Long id, AssetCreateRequest request) {
        // 购买确认复用转物品逻辑，主购买记录由前端仅填写的时间、价格和平台组成。
        WishlistItem item = Optional.ofNullable(wishlistMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.WISHLIST_NOT_FOUND));
        Long assetId = convertToAsset(id, request);
        BigDecimal purchasedPrice = request.getPurchases() == null || request.getPurchases().isEmpty()
                ? null : request.getPurchases().get(0).getPrice();
        LocalDate purchasedAt = request.getPurchaseDate();
        BigDecimal difference = purchasedPrice == null || item.getExpectedPrice() == null
                ? null : purchasedPrice.subtract(item.getExpectedPrice());
        // 该快照不随之后编辑物品购买记录而变化，确保“心愿 → 购买”的复盘口径稳定。
        wishlistMapper.updatePurchaseSummary(id, purchasedAt, purchasedPrice, difference);
        return assetId;
    }

    private WishlistItem buildEntity(WishlistRequest request, String status) {
        WishlistItem item = new WishlistItem();
        item.setName(request.getName());
        item.setCategoryId(request.getCategoryId());
        item.setBrandId(request.getBrandId());
        item.setModel(request.getModel());
        item.setExpectedPrice(request.getExpectedPrice());
        item.setCurrentPrice(request.getCurrentPrice());
        item.setLastPriceAt(request.getCurrentPrice() == null ? null : LocalDateTime.now());
        item.setImageUrl(storagePathHelper.toStoredReference(request.getImageUrl()));
        item.setLink(request.getLink());
        item.setSource(request.getSource());
        item.setStatus(status);
        item.setNotes(request.getNotes());
        item.setPriority(Optional.ofNullable(request.getPriority()).orElse(3));
        return item;
    }

    private void validateReferences(WishlistRequest request) {
        if (request.getCategoryId() != null) {
            Optional.ofNullable(dictCategoryMapper.findById(request.getCategoryId()))
                    .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "类别不存在"));
        }
        if (request.getBrandId() != null) {
            Optional.ofNullable(dictBrandMapper.findById(request.getBrandId()))
                    .orElseThrow(() -> new BizException(ErrorCode.VALIDATION_ERROR, "品牌不存在"));
        }
    }

    private WishlistDTO toDto(WishlistItem item, String brandName, String categoryName,
                              List<TagDTO> tags, List<WishlistAssetRefDTO> relatedAssets) {
        List<WishlistPriceHistory> priceHistory = priceHistoryMapper.findByWishlistId(item.getId());
        BigDecimal changeRate = calculatePriceChangeRate(item.getCurrentPrice(), priceHistory);
        return new WishlistDTO(
                item.getId(),
                item.getName(),
                item.getCategoryId(),
                categoryName,
                item.getBrandId(),
                brandName,
                item.getModel(),
                item.getExpectedPrice(),
                item.getCurrentPrice(),
                changeRate,
                item.getLastPriceAt(),
                storagePathHelper.toBrowserUrl(item.getImageUrl()),
                item.getStatus(),
                item.getLink(),
                item.getSource(),
                item.getNotes(),
                item.getPriority(),
                tags,
                item.getConvertedAssetId(),
                item.getPurchasedAt(),
                item.getPurchasedPrice(),
                item.getPurchasePriceDiff(),
                relatedAssets,
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    private BigDecimal calculatePriceChangeRate(BigDecimal current, List<WishlistPriceHistory> history) {
        if (current == null || history == null || history.size() < 2) {
            return null;
        }
        if (history.get(1).getPrice() == null || history.get(1).getPrice().signum() == 0) {
            return null;
        }
        BigDecimal previous = history.get(1).getPrice();
        return current.subtract(previous).multiply(BigDecimal.valueOf(100))
                .divide(previous, 2, RoundingMode.HALF_UP);
    }

    private Map<Long, DeviceAsset> loadAssetMap(List<Long> assetIds) {
        if (assetIds == null || assetIds.isEmpty()) {
            return Map.of();
        }
        List<DeviceAsset> assets = assetMapper.findByIds(assetIds);
        if (assets == null || assets.isEmpty()) {
            return Map.of();
        }
        return assets.stream().collect(Collectors.toMap(DeviceAsset::getId, asset -> asset));
    }

    private List<WishlistAssetRefDTO> resolveAssetRefs(WishlistItem item, Map<Long, DeviceAsset> assetCache) {
        Long assetId = item.getConvertedAssetId();
        if (assetId == null) {
            return List.of();
        }
        DeviceAsset asset = assetCache.get(assetId);
        if (asset == null) {
            return List.of(new WishlistAssetRefDTO(assetId, null, false));
        }
        return List.of(new WishlistAssetRefDTO(asset.getId(), asset.getName(), true));
    }

    private String resolveBrandName(Long brandId, Map<Long, String> cache) {
        if (brandId == null) {
            return null;
        }
        if (cache.containsKey(brandId)) {
            return cache.get(brandId);
        }
        return Optional.ofNullable(dictBrandMapper.findById(brandId))
                .map(brand -> {
                    if (StringUtils.hasText(brand.getAlias())) {
                        return brand.getAlias().trim();
                    }
                    return brand.getName();
                })
                .map(name -> {
                    cache.put(brandId, name);
                    return name;
                })
                .orElseGet(() -> {
                    cache.put(brandId, null);
                    return null;
                });
    }

    private Map<Long, DictCategory> loadCategoryMap() {
        return dictCategoryMapper.findAll().stream()
                .collect(Collectors.toMap(DictCategory::getId, category -> category));
    }

    private String resolveCategoryName(Long categoryId, Map<Long, DictCategory> categoryCache) {
        if (categoryId == null) {
            return null;
        }
        List<String> names = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        DictCategory current = categoryCache.get(categoryId);
        while (current != null) {
            if (!visited.add(current.getId())) {
                break;
            }
            if (StringUtils.hasText(current.getName())) {
                names.add(current.getName().trim());
            }
            Long parentId = current.getParentId();
            if (parentId == null) {
                break;
            }
            current = categoryCache.get(parentId);
            if (current == null) {
                current = Optional.ofNullable(dictCategoryMapper.findById(parentId)).orElse(null);
                if (current != null) {
                    categoryCache.put(current.getId(), current);
                }
            }
        }
        if (names.isEmpty()) {
            return null;
        }
        Collections.reverse(names);
        return String.join(" / ", names);
    }

    private Map<Long, List<Long>> loadWishlistTagIds(List<Long> wishlistIds) {
        if (wishlistIds == null || wishlistIds.isEmpty()) {
            return Map.of();
        }
        //List<WishlistTagMap> relations = wishlistTagMapMapper.findByWishlistIds(wishlistIds);
        List<WishlistTagMap> relations = wishlistTagMapMapper.findByWishlistIds(wishlistIds);
        log.debug("Query returned {} relations", relations.size());
        if (!relations.isEmpty()) {
            log.debug("First relation: wishlistId={}, tagId={}, createdAt={}",
                    relations.get(0).getWishlistId(),
                    relations.get(0).getTagId(),
                    relations.get(0).getCreatedAt());
        }

        if (relations.isEmpty()) {
            return Map.of();
        }
        return relations.stream()
                .collect(Collectors.groupingBy(WishlistTagMap::getWishlistId,
                        Collectors.mapping(WishlistTagMap::getTagId, Collectors.toList())));
    }

    private Map<Long, DictTag> loadTagCache(Collection<List<Long>> tagIdLists) {
        if (tagIdLists == null) {
            return Map.of();
        }
        Set<Long> allTagIds = tagIdLists.stream()
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toSet());
        if (allTagIds.isEmpty()) {
            return Map.of();
        }
        return dictTagMapper.findByIds(new ArrayList<>(allTagIds)).stream()
                .collect(Collectors.toMap(DictTag::getId, tag -> tag));
    }

    private List<TagDTO> toTagDTOs(List<Long> tagIds, Map<Long, DictTag> tagCache) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        return tagIds.stream()
                .map(tagCache::get)
                .filter(Objects::nonNull)
                .map(tag -> new TagDTO(tag.getId(), tag.getName(), tag.getColor(), tag.getIcon()))
                .collect(Collectors.toList());
    }

    private List<Long> validateTagIds(List<Long> tagIds) {
        if (tagIds == null || tagIds.isEmpty()) {
            return List.of();
        }
        List<Long> distinct = tagIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (distinct.isEmpty()) {
            return List.of();
        }
        List<DictTag> tags = dictTagMapper.findByIds(distinct);
        if (tags.size() != distinct.size()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "存在无效的标签ID");
        }
        return distinct;
    }

    private void persistTags(Long wishlistId, List<Long> tagIds) {
        wishlistTagMapMapper.deleteByWishlistId(wishlistId);
        if (tagIds == null || tagIds.isEmpty()) {
            return;
        }
        List<WishlistTagMap> mappings = tagIds.stream()
                .map(tagId -> {
                    WishlistTagMap map = new WishlistTagMap();
                    map.setWishlistId(wishlistId);
                    map.setTagId(tagId);
                    return map;
                })
                .collect(Collectors.toList());
        if (!mappings.isEmpty()) {
            wishlistTagMapMapper.batchInsert(mappings);
        }
    }

    private void validateStatus(String status) {
        if (!ALLOWED_STATUSES.contains(status)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "心愿状态非法");
        }
    }

    private String normalizeStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        String trimmed = status.trim();
        validateStatus(trimmed);
        return trimmed;
    }
}
