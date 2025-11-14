package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.DictBrandMapper;
import com.digiledger.backend.mapper.DictCategoryMapper;
import com.digiledger.backend.mapper.DictTagMapper;
import com.digiledger.backend.mapper.WishlistMapper;
import com.digiledger.backend.mapper.WishlistTagMapMapper;
import com.digiledger.backend.model.dto.asset.AssetCreateRequest;
import com.digiledger.backend.model.dto.asset.TagDTO;
import com.digiledger.backend.model.dto.wishlist.WishlistDTO;
import com.digiledger.backend.model.dto.wishlist.WishlistRequest;
import com.digiledger.backend.model.entity.DictCategory;
import com.digiledger.backend.model.entity.DictTag;
import com.digiledger.backend.model.entity.WishlistItem;
import com.digiledger.backend.model.entity.WishlistTagMap;
import com.digiledger.backend.service.AssetService;
import com.digiledger.backend.service.WishlistService;
import com.digiledger.backend.util.StoragePathHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
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

/**
 * 心愿单服务实现，可转化资产。
 */
@Service
@Slf4j
public class WishlistServiceImpl implements WishlistService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("未购买", "已购买");

    private final WishlistMapper wishlistMapper;
    private final DictCategoryMapper dictCategoryMapper;
    private final DictBrandMapper dictBrandMapper;
    private final DictTagMapper dictTagMapper;
    private final WishlistTagMapMapper wishlistTagMapMapper;
    private final AssetService assetService;
    private final StoragePathHelper storagePathHelper;

    public WishlistServiceImpl(WishlistMapper wishlistMapper,
                               DictCategoryMapper dictCategoryMapper,
                               DictBrandMapper dictBrandMapper,
                               DictTagMapper dictTagMapper,
                               WishlistTagMapMapper wishlistTagMapMapper,
                               AssetService assetService,
                               StoragePathHelper storagePathHelper) {
        this.wishlistMapper = wishlistMapper;
        this.dictCategoryMapper = dictCategoryMapper;
        this.dictBrandMapper = dictBrandMapper;
        this.dictTagMapper = dictTagMapper;
        this.wishlistTagMapMapper = wishlistTagMapMapper;
        this.assetService = assetService;
        this.storagePathHelper = storagePathHelper;
    }

    @Override
    public List<WishlistDTO> listAll(String status) {
        String normalizedStatus = normalizeStatus(status);
        List<WishlistItem> items = wishlistMapper.findAll(normalizedStatus);
        if (items.isEmpty()) {
            return List.of();
        }
        Map<Long, DictCategory> categoryMap = loadCategoryMap();
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
                        toTagDTOs(tagIdMap.get(item.getId()), tagCache)))
                .collect(Collectors.toList());
    }

    @Override
    public WishlistDTO getById(Long id) {
        WishlistItem item = Optional.ofNullable(wishlistMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.WISHLIST_NOT_FOUND));
        Map<Long, DictCategory> categoryMap = loadCategoryMap();
        Map<Long, List<Long>> tagIdMap = loadWishlistTagIds(List.of(item.getId()));
        Map<Long, DictTag> tagCache = loadTagCache(tagIdMap.values());
        return toDto(
                item,
                resolveBrandName(item.getBrandId(), new HashMap<>()),
                resolveCategoryName(item.getCategoryId(), categoryMap),
                toTagDTOs(tagIdMap.get(item.getId()), tagCache));
    }

    @Override
    @Transactional
    public Long create(WishlistRequest request) {
        validateReferences(request);
        List<Long> tagIds = validateTagIds(request.getTagIds());
        WishlistItem item = buildEntity(request, "未购买");
        wishlistMapper.insert(item);
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
            request.setCoverImageUrl(storagePathHelper.toObjectKey(item.getImageUrl()));
        }
        if (request.getStatus() == null || request.getStatus().isBlank()) {
            request.setStatus("使用中");
        }
        if (request.getEnabledDate() == null) {
            request.setEnabledDate(LocalDate.now());
        }
        Long assetId = assetService.createAsset(request);
        wishlistMapper.markConverted(id, assetId);
        return assetId;
    }

    private WishlistItem buildEntity(WishlistRequest request, String status) {
        WishlistItem item = new WishlistItem();
        item.setName(request.getName());
        item.setCategoryId(request.getCategoryId());
        item.setBrandId(request.getBrandId());
        item.setModel(request.getModel());
        item.setExpectedPrice(request.getExpectedPrice());
        item.setImageUrl(storagePathHelper.toObjectKey(request.getImageUrl()));
        item.setLink(request.getLink());
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

    private WishlistDTO toDto(WishlistItem item, String brandName, String categoryName, List<TagDTO> tags) {
        return new WishlistDTO(
                item.getId(),
                item.getName(),
                item.getCategoryId(),
                categoryName,
                item.getBrandId(),
                brandName,
                item.getModel(),
                item.getExpectedPrice(),
                storagePathHelper.toFullUrl(item.getImageUrl()),
                item.getStatus(),
                item.getLink(),
                item.getNotes(),
                item.getPriority(),
                tags,
                item.getConvertedAssetId(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
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
