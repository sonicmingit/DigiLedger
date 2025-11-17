package com.digiledger.backend.service.impl;

import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.mapper.DictCategoryMapper;
import com.digiledger.backend.mapper.PurchaseMapper;
import com.digiledger.backend.model.cover.CoverCandidate;
import com.digiledger.backend.model.cover.ProductInfo;
import com.digiledger.backend.model.dto.asset.CoverSuggestionDTO;
import com.digiledger.backend.model.entity.DeviceAsset;
import com.digiledger.backend.model.entity.DictCategory;
import com.digiledger.backend.model.entity.Purchase;
import com.digiledger.backend.service.cover.ImageSearchProvider;
import com.digiledger.backend.service.cover.ProductLinkResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 封面图智能推荐：优先解析购买链接，其次调用图片搜索。
 */
@Service
public class CoverSuggestionService {

    private static final Logger log = LoggerFactory.getLogger(CoverSuggestionService.class);
    private static final int MAX_RESULTS = 18;

    private final AssetMapper assetMapper;
    private final PurchaseMapper purchaseMapper;
    private final DictCategoryMapper dictCategoryMapper;
    private final List<ProductLinkResolver> productLinkResolvers;
    private final List<ImageSearchProvider> imageSearchProviders;

    public CoverSuggestionService(AssetMapper assetMapper,
                                  PurchaseMapper purchaseMapper,
                                  DictCategoryMapper dictCategoryMapper,
                                  List<ProductLinkResolver> productLinkResolvers,
                                  List<ImageSearchProvider> imageSearchProviders) {
        this.assetMapper = assetMapper;
        this.purchaseMapper = purchaseMapper;
        this.dictCategoryMapper = dictCategoryMapper;
        this.productLinkResolvers = productLinkResolvers == null ? List.of() : productLinkResolvers;
        this.imageSearchProviders = imageSearchProviders == null ? List.of() : imageSearchProviders;
    }

    public List<CoverCandidate> getCoverCandidatesForAsset(Long assetId, String manualQuery) {
        if (assetId == null) {
            return List.of();
        }
        DeviceAsset asset = Optional.ofNullable(assetMapper.findById(assetId)).orElse(null);
        if (asset == null) {
            return List.of();
        }
        List<Purchase> purchases = purchaseMapper.findByAssetId(assetId);
        List<CoverCandidate> candidates = new ArrayList<>();
        Set<String> unique = new LinkedHashSet<>();

        resolveFromPurchase(purchases).ifPresent(candidate -> addCandidate(candidates, unique, candidate));

        String keyword = buildKeyword(asset, manualQuery);
        if (StringUtils.hasText(keyword) && !CollectionUtils.isEmpty(imageSearchProviders)) {
            for (ImageSearchProvider provider : imageSearchProviders) {
                try {
                    List<CoverCandidate> results = provider.search(keyword, MAX_RESULTS);
                    for (CoverCandidate candidate : results) {
                        addCandidate(candidates, unique, candidate);
                        if (candidates.size() >= MAX_RESULTS) {
                            break;
                        }
                    }
                } catch (Exception ex) {
                    log.warn("图片搜索提供方 {} 查询失败：{}", provider.getName(), ex.getMessage());
                }
                if (candidates.size() >= MAX_RESULTS) {
                    break;
                }
            }
        }
        return candidates;
    }

    public List<CoverSuggestionDTO> suggest(Long assetId, String manualQuery) {
        return getCoverCandidatesForAsset(assetId, manualQuery).stream()
                .map(CoverSuggestionDTO::fromCandidate)
                .toList();
    }

    private Optional<CoverCandidate> resolveFromPurchase(List<Purchase> purchases) {
        if (CollectionUtils.isEmpty(purchases) || CollectionUtils.isEmpty(productLinkResolvers)) {
            return Optional.empty();
        }
        return purchases.stream()
                .sorted(Comparator
                        .comparing(Purchase::getPurchaseDate, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(Purchase::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder())))
                .filter(purchase -> StringUtils.hasText(purchase.getProductLink()))
                .map(this::resolvePurchaseLink)
                .flatMap(Optional::stream)
                .findFirst();
    }

    private Optional<CoverCandidate> resolvePurchaseLink(Purchase purchase) {
        String link = purchase.getProductLink();
        for (ProductLinkResolver resolver : productLinkResolvers) {
            if (!resolver.supports(link)) {
                continue;
            }
            try {
                Optional<ProductInfo> infoOptional = resolver.resolve(link);
                if (infoOptional.isEmpty()) {
                    continue;
                }
                ProductInfo info = infoOptional.get();
                if (!StringUtils.hasText(info.mainImageUrl())) {
                    continue;
                }
                Map<String, Object> extra = new LinkedHashMap<>();
                extra.put("sourceLabel", resolver.getDisplayName());
                extra.put("productTitle", info.title());
                return Optional.of(new CoverCandidate(
                        info.mainImageUrl(),
                        info.mainImageUrl(),
                        resolver.getSource(),
                        info.title(),
                        extra
                ));
            } catch (Exception ex) {
                log.warn("解析购买链接失败（{}）：{}", resolver.getSource(), ex.getMessage());
            }
        }
        return Optional.empty();
    }

    private void addCandidate(List<CoverCandidate> results, Set<String> unique, CoverCandidate candidate) {
        if (candidate == null || !StringUtils.hasText(candidate.originalUrl())) {
            return;
        }
        if (unique.add(candidate.originalUrl())) {
            results.add(candidate);
        }
    }

    private String buildKeyword(DeviceAsset asset, String manualQuery) {
        if (StringUtils.hasText(manualQuery)) {
            return manualQuery.trim();
        }
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(asset.getName())) {
            parts.add(asset.getName().trim());
        }
        if (StringUtils.hasText(asset.getBrand())) {
            parts.add(asset.getBrand().trim());
        }
        if (StringUtils.hasText(asset.getModel())) {
            parts.add(asset.getModel().trim());
        }
        String categoryName = resolveCategoryName(asset.getCategoryId());
        if (StringUtils.hasText(categoryName)) {
            parts.add(categoryName);
        }
        if (parts.isEmpty()) {
            parts.add("物品封面");
        }
        return String.join(" ", parts);
    }

    private String resolveCategoryName(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return Optional.ofNullable(dictCategoryMapper.findById(categoryId))
                .map(DictCategory::getName)
                .orElse(null);
    }
}
