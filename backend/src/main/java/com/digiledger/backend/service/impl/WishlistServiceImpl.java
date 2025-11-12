package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.DictBrandMapper;
import com.digiledger.backend.mapper.DictCategoryMapper;
import com.digiledger.backend.mapper.WishlistMapper;
import com.digiledger.backend.model.dto.asset.AssetCreateRequest;
import com.digiledger.backend.model.dto.wishlist.WishlistDTO;
import com.digiledger.backend.model.dto.wishlist.WishlistRequest;
import com.digiledger.backend.model.entity.WishlistItem;
import com.digiledger.backend.service.AssetService;
import com.digiledger.backend.service.WishlistService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 心愿单服务实现，可转化资产。
 */
@Service
public class WishlistServiceImpl implements WishlistService {

    private final WishlistMapper wishlistMapper;
    private final DictCategoryMapper dictCategoryMapper;
    private final DictBrandMapper dictBrandMapper;
    private final AssetService assetService;

    public WishlistServiceImpl(WishlistMapper wishlistMapper,
                               DictCategoryMapper dictCategoryMapper,
                               DictBrandMapper dictBrandMapper,
                               AssetService assetService) {
        this.wishlistMapper = wishlistMapper;
        this.dictCategoryMapper = dictCategoryMapper;
        this.dictBrandMapper = dictBrandMapper;
        this.assetService = assetService;
    }

    @Override
    public List<WishlistDTO> listAll() {
        return wishlistMapper.findAll().stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public WishlistDTO getById(Long id) {
        return toDto(Optional.ofNullable(wishlistMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.WISHLIST_NOT_FOUND)));
    }

    @Override
    @Transactional
    public Long create(WishlistRequest request) {
        validateReferences(request);
        WishlistItem item = buildEntity(request);
        wishlistMapper.insert(item);
        return item.getId();
    }

    @Override
    @Transactional
    public void update(Long id, WishlistRequest request) {
        WishlistItem exist = Optional.ofNullable(wishlistMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.WISHLIST_NOT_FOUND));
        validateReferences(request);
        WishlistItem update = buildEntity(request);
        update.setId(id);
        update.setConvertedAssetId(exist.getConvertedAssetId());
        wishlistMapper.update(update);
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
            request.setCoverImageUrl(item.getImageUrl());
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

    private WishlistItem buildEntity(WishlistRequest request) {
        WishlistItem item = new WishlistItem();
        item.setName(request.getName());
        item.setCategoryId(request.getCategoryId());
        item.setBrandId(request.getBrandId());
        item.setImageUrl(request.getImageUrl());
        item.setStatus(Optional.ofNullable(request.getStatus()).orElse("未购买"));
        item.setLink(request.getLink());
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
        if (request.getStatus() == null) {
            request.setStatus("未购买");
        }
    }

    private WishlistDTO toDto(WishlistItem item) {
        return new WishlistDTO(
                item.getId(),
                item.getName(),
                item.getCategoryId(),
                item.getBrandId(),
                item.getImageUrl(),
                item.getStatus(),
                item.getLink(),
                item.getNotes(),
                item.getPriority(),
                item.getConvertedAssetId(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
