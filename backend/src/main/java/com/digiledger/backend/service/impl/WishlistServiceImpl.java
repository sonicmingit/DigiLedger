package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
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
    private final AssetService assetService;

    public WishlistServiceImpl(WishlistMapper wishlistMapper, AssetService assetService) {
        this.wishlistMapper = wishlistMapper;
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
        WishlistItem item = buildEntity(request);
        wishlistMapper.insert(item);
        return item.getId();
    }

    @Override
    @Transactional
    public void update(Long id, WishlistRequest request) {
        WishlistItem exist = Optional.ofNullable(wishlistMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.WISHLIST_NOT_FOUND));
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
        if (request.getBrand() == null || request.getBrand().isBlank()) {
            request.setBrand(item.getBrand());
        }
        if (request.getModel() == null || request.getModel().isBlank()) {
            request.setModel(item.getModel());
        }
        if (request.getNotes() == null || request.getNotes().isBlank()) {
            request.setNotes(item.getNotes());
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
        item.setCategory(request.getCategory());
        item.setBrand(request.getBrand());
        item.setModel(request.getModel());
        item.setExpectedPrice(request.getExpectedPrice());
        item.setPlannedPlatform(request.getPlannedPlatform());
        item.setLink(request.getLink());
        item.setNotes(request.getNotes());
        item.setPriority(request.getPriority());
        return item;
    }

    private WishlistDTO toDto(WishlistItem item) {
        return new WishlistDTO(
                item.getId(),
                item.getName(),
                item.getCategory(),
                item.getBrand(),
                item.getModel(),
                item.getExpectedPrice(),
                item.getPlannedPlatform(),
                item.getLink(),
                item.getNotes(),
                item.getPriority(),
                item.getConvertedAssetId(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }
}
