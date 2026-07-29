package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.mapper.EquipUpgradeLinkMapper;
import com.digiledger.backend.mapper.EquipUpgradeNodeMapper;
import com.digiledger.backend.mapper.EquipUpgradeRouteMapper;
import com.digiledger.backend.mapper.PurchaseMapper;
import com.digiledger.backend.mapper.SaleMapper;
import com.digiledger.backend.mapper.WishlistMapper;
import com.digiledger.backend.model.dto.upgrade.*;
import com.digiledger.backend.model.entity.DeviceAsset;
import com.digiledger.backend.model.entity.EquipUpgradeLink;
import com.digiledger.backend.model.entity.EquipUpgradeNode;
import com.digiledger.backend.model.entity.EquipUpgradeRoute;
import com.digiledger.backend.model.entity.Purchase;
import com.digiledger.backend.model.entity.Sale;
import com.digiledger.backend.model.entity.WishlistItem;
import com.digiledger.backend.service.EquipUpgradeService;
import com.digiledger.backend.util.StoragePathHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
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
 * 升级路线领域服务。
 * <p>本服务只引用物品、购买和出售的实时数据，不复制交易数据；因此物品明细修正后，
 * 路线图、关系差额和总收支会在下次读取时同步刷新。</p>
 */
@Service
public class EquipUpgradeServiceImpl implements EquipUpgradeService {

    private static final String ROUTE_ARCHIVED = "ARCHIVED";
    private static final String NODE_ASSET = "ASSET";
    private static final String NODE_PLANNED = "PLANNED";
    private static final String NODE_WISHLIST = "WISHLIST";
    private static final String RELATION_SEQUENCE = "SEQUENCE";
    private static final String RELATION_ALTERNATIVE = "ALTERNATIVE";
    private static final String SALE_SCOPE_ASSET = "ASSET";

    private final EquipUpgradeRouteMapper routeMapper;
    private final EquipUpgradeNodeMapper nodeMapper;
    private final EquipUpgradeLinkMapper linkMapper;
    private final AssetMapper assetMapper;
    private final PurchaseMapper purchaseMapper;
    private final SaleMapper saleMapper;
    private final WishlistMapper wishlistMapper;
    private final StoragePathHelper storagePathHelper;

    public EquipUpgradeServiceImpl(EquipUpgradeRouteMapper routeMapper,
                                   EquipUpgradeNodeMapper nodeMapper,
                                   EquipUpgradeLinkMapper linkMapper,
                                   AssetMapper assetMapper,
                                   PurchaseMapper purchaseMapper,
                                   SaleMapper saleMapper,
                                   WishlistMapper wishlistMapper,
                                   StoragePathHelper storagePathHelper) {
        this.routeMapper = routeMapper;
        this.nodeMapper = nodeMapper;
        this.linkMapper = linkMapper;
        this.assetMapper = assetMapper;
        this.purchaseMapper = purchaseMapper;
        this.saleMapper = saleMapper;
        this.wishlistMapper = wishlistMapper;
        this.storagePathHelper = storagePathHelper;
    }

    /** 查询所有未删除路线，并附带真实与计划口径的独立汇总。 */
    @Override
    public List<EquipUpgradeRouteDTO> listRoutes() {
        return routeMapper.findAll().stream().map(this::toRouteDTO).toList();
    }

    /**
     * 创建路线；传入 firstNode 时路线和首节点在同一事务中落库，避免产生空路线。
     * 旧客户端仍可不传 firstNode 创建路线，以保持已经发布接口的兼容性。
     */
    @Override
    @Transactional
    public Long createRoute(EquipUpgradeRouteRequest request) {
        EquipUpgradeNodeRequest firstNode = request.getFirstNode();
        Long rootAssetId = request.getRootAssetId();
        if (firstNode != null && NODE_ASSET.equals(resolveNodeType(firstNode)) && firstNode.getAssetId() != null) {
            if (rootAssetId != null && !Objects.equals(rootAssetId, firstNode.getAssetId())) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "起点物品与首节点物品必须一致");
            }
            rootAssetId = firstNode.getAssetId();
        }
        if (rootAssetId != null) {
            requireAsset(rootAssetId);
        }

        EquipUpgradeRoute route = new EquipUpgradeRoute();
        route.setName(request.getName());
        route.setRootAssetId(rootAssetId);
        route.setRemark(request.getRemark());
        route.setMainAssetId(request.getMainAssetId());
        route.setPlanYear(request.getPlanYear());
        route.setAnnualBudget(request.getAnnualBudget());
        route.setRouteType(defaultValue(request.getRouteType(), "ACTUAL"));
        route.setStatus(defaultValue(request.getStatus(), "ACTIVE"));
        route.setIsDeleted(0);
        routeMapper.insert(route);

        if (firstNode != null) {
            EquipUpgradeNode node = new EquipUpgradeNode();
            node.setRouteId(route.getId());
            applyNodeRequest(node, firstNode, null);
            node.setLevel(1);
            node.setSort(0);
            node.setIsDeleted(0);
            ensureAssetIsNotDuplicated(route.getId(), node.getAssetId(), null);
            nodeMapper.insert(node);
            rebuildMainline(route.getId());
        }
        return route.getId();
    }

    /** 修改路线基础信息；归档路线必须先恢复为 ACTIVE 才能编辑。 */
    @Override
    @Transactional
    public void updateRoute(Long id, EquipUpgradeRouteRequest request) {
        EquipUpgradeRoute route = requireRoute(id);
        if (request.getRootAssetId() != null) {
            requireAsset(request.getRootAssetId());
        }
        if (request.getMainAssetId() != null) {
            requireAssetInRoute(id, request.getMainAssetId());
        }
        route.setName(request.getName());
        // 编辑路线时前端通常只提交变更字段，未提交的主物品/起点必须保留。
        if (request.getRootAssetId() != null) {
            route.setRootAssetId(request.getRootAssetId());
        }
        if (request.getMainAssetId() != null) {
            route.setMainAssetId(request.getMainAssetId());
        }
        route.setRemark(request.getRemark());
        route.setPlanYear(request.getPlanYear());
        route.setAnnualBudget(request.getAnnualBudget());
        route.setRouteType(defaultValue(request.getRouteType(), route.getRouteType()));
        route.setStatus(defaultValue(request.getStatus(), route.getStatus()));
        routeMapper.update(route);
    }

    @Override
    @Transactional
    public void deleteRoute(Long id) {
        requireRoute(id);
        linkMapper.softDeleteByRoute(id);
        nodeMapper.softDeleteByRoute(id);
        routeMapper.softDelete(id);
    }

    /**
     * 创建节点。带锚点参数时会自动产生相应 SEQUENCE/ALTERNATIVE 关系；
     * 未带锚点时保留旧版独立节点创建语义。
     */
    @Override
    @Transactional
    public Long addNode(Long routeId, EquipUpgradeNodeRequest request) {
        EquipUpgradeRoute route = requireEditableRoute(routeId);
        EquipUpgradeNode anchor = request.getAnchorNodeId() == null
                ? null : requireNodeInRoute(request.getAnchorNodeId(), routeId);
        if (anchor != null && !StringUtils.hasText(request.getPosition())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "基于已有物品添加时必须选择上级、同级或下级位置");
        }

        EquipUpgradeNode node = new EquipUpgradeNode();
        node.setRouteId(route.getId());
        applyNodeRequest(node, request, anchor);
        node.setIsDeleted(0);
        ensureAssetIsNotDuplicated(routeId, node.getAssetId(), null);

        boolean insertBefore = anchor != null && "BEFORE".equals(request.getPosition());
        if (anchor != null && "BEFORE".equals(request.getPosition())) {
            if (insertBefore || Optional.ofNullable(anchor.getLevel()).orElse(1) <= 1) {
                nodeMapper.increaseLevelFrom(routeId, Optional.ofNullable(anchor.getLevel()).orElse(1));
                node.setLevel(Optional.ofNullable(anchor.getLevel()).orElse(1));
            } else {
                node.setLevel(Math.max(1, Optional.ofNullable(anchor.getLevel()).orElse(1) - 1));
            }
        }
        if (anchor != null && "AFTER".equals(request.getPosition())) {
            // 下级插入到锚点后方，已有的后续代际整体顺延，避免与新节点落在同一列。
            int insertedLevel = Optional.ofNullable(anchor.getLevel()).orElse(1) + 1;
            nodeMapper.increaseLevelFrom(routeId, insertedLevel);
            node.setLevel(insertedLevel);
        }
        nodeMapper.insert(node);

        if (anchor != null) {
            createAutomaticRelation(routeId, anchor, node, request.getPosition());
        }
        // 每次新增上、同、下级后按结构层级重建主线，并刷新相邻代际的价差。
        rebuildMainline(routeId);
        return node.getId();
    }

    @Override
    @Transactional
    public void updateNode(Long routeId, Long nodeId, EquipUpgradeNodeRequest request) {
        requireEditableRoute(routeId);
        EquipUpgradeNode node = requireNodeInRoute(nodeId, routeId);
        Integer originalLevel = Optional.ofNullable(node.getLevel()).orElse(1);
        Integer originalSort = Optional.ofNullable(node.getSort()).orElse(0);
        applyNodeRequest(node, request, null);
        // 节点编辑和主物品切换都不属于移动代际操作，无论客户端是否携带旧默认值都保留原布局。
        node.setLevel(originalLevel);
        node.setSort(originalSort);
        ensureAssetIsNotDuplicated(routeId, node.getAssetId(), nodeId);
        nodeMapper.update(node);
        if (Boolean.TRUE.equals(node.getMainline())) {
            nodeMapper.setMainlineAtLevelExcept(routeId, node.getLevel(), node.getId());
        }
        rebuildMainline(routeId);
    }

    private void applyNodeRequest(EquipUpgradeNode node, EquipUpgradeNodeRequest request, EquipUpgradeNode anchor) {
        String nodeType = resolveNodeType(request);
        if (NODE_ASSET.equals(nodeType)) {
            if (request.getAssetId() == null) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "真实物品节点必须选择已有物品");
            }
            requireAsset(request.getAssetId());
        } else if (NODE_WISHLIST.equals(nodeType)) {
            if (request.getWishlistId() == null || wishlistMapper.findById(request.getWishlistId()) == null) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "请选择有效的心愿物品");
            }
        } else if (!StringUtils.hasText(request.getTargetName())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "旧计划节点必须填写目标物品名称");
        }
        node.setAssetId(request.getAssetId());
        node.setWishlistId(request.getWishlistId());
        node.setNodeType(nodeType);
        // 独立创建请求可提供 level/sort；更新入口会在调用后再次锁定原布局。
        node.setLevel(anchor == null ? Optional.ofNullable(request.getLevel()).orElse(Optional.ofNullable(node.getLevel()).orElse(1)) : calculateLevel(anchor, request.getPosition()));
        node.setSort(anchor == null ? Optional.ofNullable(request.getSort()).orElse(Optional.ofNullable(node.getSort()).orElse(0))
                : "ALTERNATIVE".equals(request.getPosition()) ? Optional.ofNullable(anchor.getSort()).orElse(0) + 1 : 0);
        node.setMainline(request.getMainline() == null ? !"ALTERNATIVE".equals(request.getPosition()) : request.getMainline());
        node.setLabel(request.getLabel());
        node.setAlternativePurpose(request.getAlternativePurpose());
        node.setRemark(request.getRemark());
        node.setTitle(request.getTitle());
        node.setTargetName(request.getTargetName());
        node.setPeriodLabel(request.getPeriodLabel());
        node.setPlannedBudget(request.getPlannedBudget());
        node.setExpectedRecovery(request.getExpectedRecovery());
        node.setStatus(defaultValue(request.getStatus(), NODE_WISHLIST.equals(nodeType) ? "READY" : "PLANNED"));
    }

    /** 删除节点仅移除路线引用和关系，不会删除物品中心中的真实物品。 */
    @Override
    @Transactional
    public void deleteNode(Long routeId, Long nodeId) {
        requireEditableRoute(routeId);
        requireNodeInRoute(nodeId, routeId);
        linkMapper.softDeleteByNodeId(nodeId);
        nodeMapper.softDelete(nodeId);
    }

    /**
     * 兼容旧版独立连线入口，同时对 SEQUENCE 执行防环校验。
     */
    @Override
    @Transactional
    public Long addLink(Long routeId, EquipUpgradeLinkRequest request) {
        requireEditableRoute(routeId);
        EquipUpgradeNode fromNode = requireNodeInRoute(request.getFromNodeId(), routeId);
        EquipUpgradeNode toNode = requireNodeInRoute(request.getToNodeId(), routeId);
        if (Objects.equals(fromNode.getId(), toNode.getId())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "关联节点不能连接自身");
        }
        String relationType = defaultValue(request.getRelationType(), RELATION_SEQUENCE);
        List<EquipUpgradeLink> links = linkMapper.findByRouteId(routeId);
        boolean exists = links.stream().anyMatch(link -> Objects.equals(link.getFromNodeId(), fromNode.getId())
                && Objects.equals(link.getToNodeId(), toNode.getId())
                && Objects.equals(link.getRelationType(), relationType));
        if (exists) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "升级关系已存在");
        }
        if (RELATION_SEQUENCE.equals(relationType) && wouldCreateSequenceCycle(links, fromNode.getId(), toNode.getId())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "前后代关系不能形成环");
        }
        EquipUpgradeLink link = new EquipUpgradeLink();
        link.setRouteId(routeId);
        link.setFromNodeId(fromNode.getId());
        link.setToNodeId(toNode.getId());
        link.setRelationType(relationType);
        link.setRemark(request.getRemark());
        link.setIsDeleted(0);
        linkMapper.insert(link);
        return link.getId();
    }

    @Override
    @Transactional
    public void deleteLink(Long routeId, Long linkId) {
        requireEditableRoute(routeId);
        EquipUpgradeLink link = Optional.ofNullable(linkMapper.findById(linkId))
                .orElseThrow(() -> new BizException(ErrorCode.UPGRADE_LINK_NOT_FOUND));
        if (!Objects.equals(routeId, link.getRouteId())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "升级关系不属于当前路线");
        }
        linkMapper.softDelete(linkId);
    }

    /** 读取路线图并按需求文档输出节点、关系计算、真实汇总、计划汇总和数据告警。 */
    @Override
    @Transactional
    public UpgradeRouteGraphDTO getRouteGraph(Long routeId) {
        EquipUpgradeRoute route = requireRoute(routeId);
        List<EquipUpgradeNode> nodes = nodeMapper.findByRouteId(routeId);
        List<EquipUpgradeLink> links = linkMapper.findByRouteId(routeId);
        // 旧 DTO 的 level=1 缺省值可能在“设为同级主物品”时覆盖原代际；命中特征后自动复原。
        if (repairLegacyLevelOneOverwrite(routeId, nodes, links)) {
            nodes = nodeMapper.findByRouteId(routeId);
            links = linkMapper.findByRouteId(routeId);
        }
        // 兼容此前“同级被排成下一代”的存量数据：首次读取时即可恢复正确列布局，无需用户手工编辑。
        if (needsAlternativeLayoutRepair(nodes, links)) {
            rebuildMainline(routeId);
            nodes = nodeMapper.findByRouteId(routeId);
            links = linkMapper.findByRouteId(routeId);
        }
        Map<Long, DeviceAsset> assets = loadAssets(nodes.stream().map(EquipUpgradeNode::getAssetId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, WishlistItem> wishlists = new HashMap<>();
        for (EquipUpgradeNode node : nodes) {
            if (node.getWishlistId() != null) {
                WishlistItem item = wishlistMapper.findById(node.getWishlistId());
                if (item != null) {
                    wishlists.put(item.getId(), item);
                }
            }
        }
        Map<Long, AssetFinancials> financials = buildFinancials(assets.keySet());
        Map<Long, EquipUpgradeNode> nodeMap = nodes.stream()
                .collect(Collectors.toMap(EquipUpgradeNode::getId, item -> item));

        List<UpgradeGraphNodeDTO> nodeDTOs = nodes.stream()
                .map(node -> toNodeDTO(node, assets.get(node.getAssetId()), financials.get(node.getAssetId()), wishlists.get(node.getWishlistId())))
                .toList();
        List<UpgradeGraphLinkDTO> linkDTOs = links.stream()
                .map(link -> toLinkDTO(link, nodeMap, assets, financials))
                .toList();
        UpgradeActualSummaryDTO actualSummary = buildActualSummary(nodes, financials);
        UpgradePlanSummaryDTO planSummary = buildPlanSummary(nodes);
        List<String> warnings = nodeDTOs.stream().flatMap(node -> node.dataWarnings().stream())
                .distinct().toList();
        return new UpgradeRouteGraphDTO(route.getId(), route.getName(), route.getRemark(), route.getPlanYear(),
                route.getAnnualBudget(), nodeDTOs, linkDTOs, toRouteDTO(route), actualSummary, planSummary, warnings);
    }

    /** 根据点击的添加位置创建关系，前后代关系会再次执行防环检查。 */
    private void createAutomaticRelation(Long routeId, EquipUpgradeNode anchor, EquipUpgradeNode node, String position) {
        EquipUpgradeLinkRequest request = new EquipUpgradeLinkRequest();
        if ("BEFORE".equals(position)) {
            request.setFromNodeId(node.getId());
            request.setToNodeId(anchor.getId());
            request.setRelationType(RELATION_SEQUENCE);
        } else if ("AFTER".equals(position)) {
            request.setFromNodeId(anchor.getId());
            request.setToNodeId(node.getId());
            request.setRelationType(RELATION_SEQUENCE);
        } else {
            request.setFromNodeId(anchor.getId());
            request.setToNodeId(node.getId());
            request.setRelationType(RELATION_ALTERNATIVE);
        }
        addLink(routeId, request);
    }

    /** 判断新增 from -> to 是否会使 to 通过已有前后代关系回到 from。 */
    private boolean wouldCreateSequenceCycle(List<EquipUpgradeLink> links, Long fromNodeId, Long toNodeId) {
        Map<Long, List<Long>> outgoing = new HashMap<>();
        for (EquipUpgradeLink link : links) {
            if (RELATION_SEQUENCE.equals(link.getRelationType())) {
                outgoing.computeIfAbsent(link.getFromNodeId(), ignored -> new ArrayList<>()).add(link.getToNodeId());
            }
        }
        ArrayDeque<Long> queue = new ArrayDeque<>();
        Set<Long> visited = new HashSet<>();
        queue.add(toNodeId);
        while (!queue.isEmpty()) {
            Long current = queue.removeFirst();
            if (!visited.add(current)) {
                continue;
            }
            if (Objects.equals(current, fromNodeId)) {
                return true;
            }
            queue.addAll(outgoing.getOrDefault(current, Collections.emptyList()));
        }
        return false;
    }

    private UpgradeGraphNodeDTO toNodeDTO(EquipUpgradeNode node, DeviceAsset asset, AssetFinancials financials, WishlistItem wishlist) {
        String nodeType = defaultValue(node.getNodeType(), node.getAssetId() == null ? NODE_PLANNED : NODE_ASSET);
        List<String> warnings = new ArrayList<>();
        if (NODE_WISHLIST.equals(nodeType) && wishlist != null) {
            return new UpgradeGraphNodeDTO(node.getId(), null, wishlist.getName(), null, zero(), zero(), false,
                    null, storagePathHelper.toFullUrl(wishlist.getImageUrl()), node.getLevel(), node.getSort(), node.getLabel(), node.getRemark(),
                    null, wishlist.getName(), null, wishlist.getExpectedPrice(), null, wishlist.getStatus(), nodeType,
                    wishlist.getBrandId() == null ? null : "心愿物品", wishlist.getModel(), null, null, null, null, null, null, List.of(),
                    node.getAlternativePurpose(), node.getWishlistId(), Boolean.TRUE.equals(node.getMainline()));
        }
        if (NODE_PLANNED.equals(nodeType)) {
            return new UpgradeGraphNodeDTO(node.getId(), null, node.getTargetName(), null, zero(), zero(), false,
                    null, null, node.getLevel(), node.getSort(), node.getLabel(), node.getRemark(), node.getTitle(),
                    node.getTargetName(), node.getPeriodLabel(), node.getPlannedBudget(), node.getExpectedRecovery(),
                    node.getStatus(), nodeType, null, null, null, null, null, null, null, null, List.<String>of(), node.getAlternativePurpose(), node.getWishlistId(), Boolean.TRUE.equals(node.getMainline()));
        }
        if (asset == null || financials == null) {
            warnings.add("关联物品已不可用");
            return new UpgradeGraphNodeDTO(node.getId(), node.getAssetId(), node.getTargetName(), null, zero(), zero(), false,
                    null, null, node.getLevel(), node.getSort(), node.getLabel(), node.getRemark(), node.getTitle(),
                    node.getTargetName(), node.getPeriodLabel(), node.getPlannedBudget(), node.getExpectedRecovery(),
                    node.getStatus(), nodeType, null, null, null, null, null, null, null, null, warnings, node.getAlternativePurpose(), node.getWishlistId(), Boolean.TRUE.equals(node.getMainline()));
        }
        LocalDate purchaseDate = financials.primaryPurchaseDate();
        if (purchaseDate == null) {
            warnings.add("缺少主商品购买日期，无法计算使用时间和购买间隔");
        }
        if (financials.primarySpend().compareTo(BigDecimal.ZERO) == 0) {
            warnings.add("缺少主商品购买金额");
        }
        Long useDays = calculateUseDays(purchaseDate, financials.mainSale() == null ? null : financials.mainSale().getSaleDate(), warnings);
        Sale mainSale = financials.mainSale();
        boolean sold = mainSale != null;
        if ("已出售".equals(asset.getStatus()) && mainSale == null) {
            warnings.add("物品状态为已出售，但未找到主商品出售记录");
        }
        return new UpgradeGraphNodeDTO(node.getId(), asset.getId(), asset.getName(), asset.getStatus(),
                financials.totalSpend(), mainSale == null ? zero() : safe(mainSale.getSalePrice()), sold,
                purchaseDate, storagePathHelper.toFullUrl(asset.getCoverImageUrl()), node.getLevel(), node.getSort(),
                node.getLabel(), node.getRemark(), node.getTitle(), node.getTargetName(), node.getPeriodLabel(),
                node.getPlannedBudget(), node.getExpectedRecovery(), node.getStatus(), nodeType, asset.getBrand(), asset.getModel(),
                financials.primarySpend(), financials.totalSpend(), useDays, mainSale == null ? null : mainSale.getSaleDate(),
                mainSale == null ? null : safe(mainSale.getSalePrice()), mainSale == null ? null : netIncome(mainSale), warnings,
                node.getAlternativePurpose(), node.getWishlistId(), Boolean.TRUE.equals(node.getMainline()));
    }

    private UpgradeGraphLinkDTO toLinkDTO(EquipUpgradeLink link, Map<Long, EquipUpgradeNode> nodeMap,
                                           Map<Long, DeviceAsset> assets, Map<Long, AssetFinancials> financials) {
        String relationType = defaultValue(link.getRelationType(), RELATION_SEQUENCE);
        if (RELATION_ALTERNATIVE.equals(relationType)) {
            return new UpgradeGraphLinkDTO(link.getId(), link.getFromNodeId(), link.getToNodeId(), null, link.getRemark(),
                    relationType, null, null, null, "NOT_APPLICABLE");
        }
        EquipUpgradeNode from = nodeMap.get(link.getFromNodeId());
        EquipUpgradeNode to = nodeMap.get(link.getToNodeId());
        if (from == null || to == null || from.getAssetId() == null || to.getAssetId() == null) {
            return new UpgradeGraphLinkDTO(link.getId(), link.getFromNodeId(), link.getToNodeId(), null, link.getRemark(),
                    relationType, null, null, null, "PLAN_OR_DATA_INCOMPLETE");
        }
        AssetFinancials fromFinancials = financials.get(from.getAssetId());
        AssetFinancials toFinancials = financials.get(to.getAssetId());
        if (fromFinancials == null || toFinancials == null) {
            return new UpgradeGraphLinkDTO(link.getId(), link.getFromNodeId(), link.getToNodeId(), null, link.getRemark(),
                    relationType, null, null, null, "DATA_INCOMPLETE");
        }
        Long gap = null;
        String status = "READY";
        if (fromFinancials.primaryPurchaseDate() == null || toFinancials.primaryPurchaseDate() == null) {
            status = "DATA_INCOMPLETE";
        } else {
            gap = ChronoUnit.DAYS.between(fromFinancials.primaryPurchaseDate(), toFinancials.primaryPurchaseDate());
            if (gap < 0) {
                status = "DATE_ANOMALY";
            }
        }
        BigDecimal priceDelta = toFinancials.primarySpend().subtract(fromFinancials.primarySpend()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal replacement = fromFinancials.mainSale() == null ? null
                : toFinancials.primarySpend().subtract(netIncome(fromFinancials.mainSale())).setScale(2, RoundingMode.HALF_UP);
        if (replacement == null && "READY".equals(status)) {
            status = "WAITING_FOR_SALE";
        }
        return new UpgradeGraphLinkDTO(link.getId(), link.getFromNodeId(), link.getToNodeId(), replacement,
                link.getRemark(), relationType, gap, priceDelta, replacement, status);
    }

    private EquipUpgradeRouteDTO toRouteDTO(EquipUpgradeRoute route) {
        List<EquipUpgradeNode> nodes = nodeMapper.findByRouteId(route.getId());
        Set<Long> assetIds = nodes.stream().map(EquipUpgradeNode::getAssetId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        // 旧版路线可能只有 rootAssetId 而尚未创建节点，列表仍需能显示起点物品名称。
        if (route.getRootAssetId() != null) {
            assetIds.add(route.getRootAssetId());
        }
        Map<Long, DeviceAsset> assets = loadAssets(assetIds);
        Map<Long, AssetFinancials> financials = buildFinancials(assets.keySet());
        Map<String, Long> statuses = nodes.stream().collect(Collectors.groupingBy(
                node -> Optional.ofNullable(node.getStatus()).orElse("PLANNED"), Collectors.counting()));
        DeviceAsset root = route.getRootAssetId() == null ? null : assets.get(route.getRootAssetId());
        List<LocalDate> purchaseDates = financials.values().stream().map(AssetFinancials::primaryPurchaseDate)
                .filter(Objects::nonNull).sorted().toList();
        List<String> covers = nodes.stream().filter(node -> node.getAssetId() != null)
                .map(node -> assets.get(node.getAssetId())).filter(Objects::nonNull)
                .map(DeviceAsset::getCoverImageUrl).filter(StringUtils::hasText).map(storagePathHelper::toFullUrl)
                .distinct().limit(3).toList();
        return new EquipUpgradeRouteDTO(route.getId(), route.getName(), route.getRootAssetId(), route.getMainAssetId(),
                root == null ? null : root.getName(), route.getRemark(), route.getPlanYear(), route.getAnnualBudget(),
                buildPlanSummary(nodes).plannedBudget(), buildPlanSummary(nodes).expectedRecovery(), statuses, route.getUpdatedAt(),
                defaultValue(route.getRouteType(), "MIXED"), defaultValue(route.getStatus(), "ACTIVE"),
                buildActualSummary(nodes, financials), buildPlanSummary(nodes),
                purchaseDates.isEmpty() ? null : purchaseDates.get(0), purchaseDates.isEmpty() ? null : purchaseDates.get(purchaseDates.size() - 1), covers);
    }

    private UpgradeActualSummaryDTO buildActualSummary(List<EquipUpgradeNode> nodes, Map<Long, AssetFinancials> financials) {
        Set<Long> assetIds = nodes.stream().filter(node -> NODE_ASSET.equals(defaultValue(node.getNodeType(), node.getAssetId() == null ? NODE_PLANNED : NODE_ASSET)))
                .map(EquipUpgradeNode::getAssetId).filter(Objects::nonNull).collect(Collectors.toSet());
        BigDecimal total = zero();
        BigDecimal primary = zero();
        BigDecimal income = zero();
        LocalDate routeStart = null;
        LocalDate routeEnd = null;
        for (Long assetId : assetIds) {
            AssetFinancials item = financials.get(assetId);
            if (item == null) {
                continue;
            }
            total = total.add(item.totalSpend());
            primary = primary.add(item.primarySpend());
            income = income.add(item.allSalesNetIncome());
            if (item.primaryPurchaseDate() == null) {
                continue;
            }
            routeStart = routeStart == null || item.primaryPurchaseDate().isBefore(routeStart)
                    ? item.primaryPurchaseDate() : routeStart;
            // 仍在持有或缺少出售记录的物品，成本持续均摊到今天；全部出售时取最后一次出售日期。
            LocalDate itemEnd = item.mainSale() == null ? LocalDate.now() : item.mainSale().getSaleDate();
            if (itemEnd != null && (routeEnd == null || itemEnd.isAfter(routeEnd))) {
                routeEnd = itemEnd;
            }
        }
        BigDecimal net = total.subtract(income);
        // 日均成本以路线的连续实际履历为分母，避免并行持有多件物品时重复累计使用天数。
        long routeDays = routeStart == null || routeEnd == null ? 0
                : Math.max(1, ChronoUnit.DAYS.between(routeStart, routeEnd));
        BigDecimal daily = routeDays == 0 ? null : net.divide(BigDecimal.valueOf(routeDays), 2, RoundingMode.HALF_UP);
        return new UpgradeActualSummaryDTO(assetIds.size(), money(total), money(primary), money(total.subtract(primary)),
                money(income), money(net), daily);
    }

    private UpgradePlanSummaryDTO buildPlanSummary(List<EquipUpgradeNode> nodes) {
        BigDecimal budget = nodes.stream().filter(node -> NODE_PLANNED.equals(defaultValue(node.getNodeType(), node.getAssetId() == null ? NODE_PLANNED : NODE_ASSET)))
                .map(EquipUpgradeNode::getPlannedBudget).filter(Objects::nonNull).reduce(zero(), BigDecimal::add);
        BigDecimal recovery = nodes.stream().filter(node -> NODE_PLANNED.equals(defaultValue(node.getNodeType(), node.getAssetId() == null ? NODE_PLANNED : NODE_ASSET)))
                .map(EquipUpgradeNode::getExpectedRecovery).filter(Objects::nonNull).reduce(zero(), BigDecimal::add);
        return new UpgradePlanSummaryDTO(money(budget), money(recovery), money(budget.subtract(recovery)));
    }

    /**
     * 主线只保留每一层标记为主物品的节点。每次变更后按结构层级重新串联，
     * 让“中途补录上级/下级”无需手工调整连线和价差；同级备用节点仍保留在总收支中。
     */
    private void rebuildMainline(Long routeId) {
        List<EquipUpgradeNode> allNodes = nodeMapper.findByRouteId(routeId);
        List<EquipUpgradeLink> alternatives = linkMapper.findByRouteId(routeId).stream()
                .filter(link -> RELATION_ALTERNATIVE.equals(link.getRelationType())).toList();
        normalizeAlternativeMainline(allNodes, alternatives);
        collapseAlternativeLevels(allNodes, alternatives);
        List<EquipUpgradeNode> mainline = allNodes.stream().filter(node -> Boolean.TRUE.equals(node.getMainline()))
                // 代际由“添加上/下/同级”的结构关系决定；购买时间仅用于显示间隔，不能把同级推到下一代。
                .sorted(Comparator.comparing(EquipUpgradeNode::getLevel, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(EquipUpgradeNode::getSort, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(EquipUpgradeNode::getId))
                .toList();
        nodeMapper.softDeleteSequenceLinksByRoute(routeId);
        for (int index = 0; index < mainline.size(); index++) {
            EquipUpgradeNode node = mainline.get(index);
            nodeMapper.updateLayout(node.getId(), index + 1, 0);
            // 后续同级对齐直接读取实体对象，须同步内存状态，不能继续使用数据库更新前的旧层级。
            node.setLevel(index + 1);
            node.setSort(0);
            if (index > 0) {
                EquipUpgradeLink link = new EquipUpgradeLink();
                link.setRouteId(routeId);
                link.setFromNodeId(mainline.get(index - 1).getId());
                link.setToNodeId(node.getId());
                link.setRelationType(RELATION_SEQUENCE);
                link.setIsDeleted(0);
                linkMapper.insert(link);
            }
        }
        // 备用同级沿用其 ALTERNATIVE 锚点所在代际，不参与主线的顺序关系。
        Map<Long, EquipUpgradeNode> mainlineMap = mainline.stream().collect(Collectors.toMap(EquipUpgradeNode::getId, node -> node));
        alignAlternativeLayouts(allNodes, alternatives, mainlineMap);
    }

    /**
     * 同级关系中仅允许一个主物品参与前后代计算。
     * 历史数据可能因旧逻辑把同一组节点都标记为主物品；此处选择最近一次修改的节点作为主物品，
     * 使随后重建的 SEQUENCE 关系不会再把同级误拆成下一代。
     */
    private void normalizeAlternativeMainline(List<EquipUpgradeNode> nodes, List<EquipUpgradeLink> alternatives) {
        Map<Long, EquipUpgradeNode> nodeMap = nodes.stream().collect(Collectors.toMap(EquipUpgradeNode::getId, node -> node));
        Comparator<EquipUpgradeNode> mostRecent = Comparator
                .comparing(EquipUpgradeNode::getUpdatedAt, Comparator.nullsLast(Comparator.naturalOrder()))
                .thenComparing(EquipUpgradeNode::getId);
        for (List<EquipUpgradeNode> group : alternativeGroups(nodes, alternatives, nodeMap)) {
            List<EquipUpgradeNode> mains = group.stream().filter(node -> Boolean.TRUE.equals(node.getMainline())).toList();
            if (mains.size() <= 1) {
                continue;
            }
            EquipUpgradeNode retained = mains.stream().max(mostRecent).orElseThrow();
            for (EquipUpgradeNode candidate : mains) {
                if (!Objects.equals(candidate.getId(), retained.getId())) {
                    nodeMapper.updateMainline(candidate.getId(), false);
                    candidate.setMainline(false);
                }
            }
        }
    }

    /**
     * 将一整组同级节点对齐到主物品所在代际。支持“从备用物品继续添加同级”的链式关系，
     * 不依赖关系的 from/to 方向，避免同级节点被排到新的一列。
     */
    private void alignAlternativeLayouts(List<EquipUpgradeNode> nodes, List<EquipUpgradeLink> alternatives,
                                         Map<Long, EquipUpgradeNode> mainlineMap) {
        Map<Long, EquipUpgradeNode> nodeMap = nodes.stream().collect(Collectors.toMap(EquipUpgradeNode::getId, node -> node));
        for (List<EquipUpgradeNode> group : alternativeGroups(nodes, alternatives, nodeMap)) {
            EquipUpgradeNode primary = group.stream()
                    .filter(node -> mainlineMap.containsKey(node.getId()))
                    .findFirst().orElse(null);
            if (primary == null) {
                continue;
            }
            int level = Optional.ofNullable(primary.getLevel()).orElse(1);
            int sort = 0;
            // 主物品始终放在同级列顶部，备用物品按稳定 ID 顺序纵向排列。
            for (EquipUpgradeNode node : group.stream().sorted(Comparator.comparing(EquipUpgradeNode::getId)).toList()) {
                int nodeSort = Objects.equals(node.getId(), primary.getId()) ? 0 : ++sort;
                nodeMapper.updateLayout(node.getId(), level, nodeSort);
                node.setLevel(level);
                node.setSort(nodeSort);
            }
        }
    }

    /**
     * 同级组以最早已有层级作为锚点。这样旧版本已把新增同级错误放到下一代时，
     * 在下一次重建便会回到原列；新写入的同级节点也天然继承被点击物品的层级。
     */
    private void collapseAlternativeLevels(List<EquipUpgradeNode> nodes, List<EquipUpgradeLink> alternatives) {
        Map<Long, EquipUpgradeNode> nodeMap = nodes.stream().collect(Collectors.toMap(EquipUpgradeNode::getId, node -> node));
        for (List<EquipUpgradeNode> group : alternativeGroups(nodes, alternatives, nodeMap)) {
            int anchorLevel = group.stream().map(EquipUpgradeNode::getLevel).filter(Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(1);
            int sort = 0;
            for (EquipUpgradeNode node : group.stream().sorted(Comparator.comparing(EquipUpgradeNode::getId)).toList()) {
                int nodeSort = Boolean.TRUE.equals(node.getMainline()) ? 0 : ++sort;
                nodeMapper.updateLayout(node.getId(), anchorLevel, nodeSort);
                node.setLevel(anchorLevel);
                node.setSort(nodeSort);
            }
        }
    }

    /** 构造 ALTERNATIVE 的无向连通分组，供主物品冲突修复和纵向排布共用。 */
    private List<List<EquipUpgradeNode>> alternativeGroups(List<EquipUpgradeNode> nodes, List<EquipUpgradeLink> alternatives,
                                                            Map<Long, EquipUpgradeNode> nodeMap) {
        Map<Long, List<Long>> adjacent = new HashMap<>();
        for (EquipUpgradeLink link : alternatives) {
            adjacent.computeIfAbsent(link.getFromNodeId(), ignored -> new ArrayList<>()).add(link.getToNodeId());
            adjacent.computeIfAbsent(link.getToNodeId(), ignored -> new ArrayList<>()).add(link.getFromNodeId());
        }
        List<List<EquipUpgradeNode>> groups = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        for (EquipUpgradeNode node : nodes) {
            if (!adjacent.containsKey(node.getId()) || !visited.add(node.getId())) {
                continue;
            }
            List<EquipUpgradeNode> group = new ArrayList<>();
            ArrayDeque<Long> queue = new ArrayDeque<>();
            queue.add(node.getId());
            while (!queue.isEmpty()) {
                Long currentId = queue.removeFirst();
                EquipUpgradeNode current = nodeMap.get(currentId);
                if (current != null) {
                    group.add(current);
                }
                for (Long nextId : adjacent.getOrDefault(currentId, Collections.emptyList())) {
                    if (visited.add(nextId)) {
                        queue.add(nextId);
                    }
                }
            }
            groups.add(group);
        }
        return groups;
    }

    /** 判断同级组是否发生跨代或双主物品错位，仅命中历史异常数据时才触发读取期修复。 */
    private boolean needsAlternativeLayoutRepair(List<EquipUpgradeNode> nodes, List<EquipUpgradeLink> links) {
        List<EquipUpgradeLink> alternatives = links.stream()
                .filter(link -> RELATION_ALTERNATIVE.equals(link.getRelationType())).toList();
        if (alternatives.isEmpty()) {
            return false;
        }
        Map<Long, EquipUpgradeNode> nodeMap = nodes.stream().collect(Collectors.toMap(EquipUpgradeNode::getId, node -> node));
        for (EquipUpgradeLink link : alternatives) {
            EquipUpgradeNode from = nodeMap.get(link.getFromNodeId());
            EquipUpgradeNode to = nodeMap.get(link.getToNodeId());
            if (from != null && to != null && !Objects.equals(from.getLevel(), to.getLevel())) {
                return true;
            }
        }
        return alternativeGroups(nodes, alternatives, nodeMap).stream()
                .anyMatch(group -> group.stream().filter(node -> Boolean.TRUE.equals(node.getMainline())).count() > 1);
    }

    /**
     * 修复旧请求模型把被编辑节点写入第一代的特定数据形态。
     * <p>安全条件必须同时满足：第一代存在一个无同级关系且被误标为备用的原节点、
     * 一个主物品及其可选同级组，并且该主物品按购买时间应位于后续代际。
     * 命中后恢复原第一代，并把整个错位同级组插回目标位置。</p>
     */
    private boolean repairLegacyLevelOneOverwrite(Long routeId, List<EquipUpgradeNode> nodes,
                                                   List<EquipUpgradeLink> links) {
        List<EquipUpgradeNode> levelOne = nodes.stream()
                .filter(node -> Optional.ofNullable(node.getLevel()).orElse(1) == 1)
                .toList();
        if (levelOne.size() < 2) {
            return false;
        }
        EquipUpgradeNode displaced = levelOne.stream()
                .filter(node -> Boolean.TRUE.equals(node.getMainline())).findFirst().orElse(null);
        if (displaced == null) {
            return false;
        }
        List<EquipUpgradeLink> alternatives = links.stream()
                .filter(link -> RELATION_ALTERNATIVE.equals(link.getRelationType())).toList();
        Map<Long, EquipUpgradeNode> nodeMap = nodes.stream().collect(Collectors.toMap(EquipUpgradeNode::getId, node -> node));
        List<EquipUpgradeNode> displacedGroup = alternativeGroups(nodes, alternatives, nodeMap).stream()
                .filter(group -> group.stream().anyMatch(node -> Objects.equals(node.getId(), displaced.getId())))
                .findFirst().orElse(List.of(displaced));
        Set<Long> displacedGroupIds = displacedGroup.stream().map(EquipUpgradeNode::getId).collect(Collectors.toSet());
        EquipUpgradeNode original = levelOne.stream()
                .filter(node -> !Boolean.TRUE.equals(node.getMainline()))
                .filter(node -> !displacedGroupIds.contains(node.getId()))
                .findFirst().orElse(null);
        if (original == null || levelOne.stream().anyMatch(node -> !Objects.equals(node.getId(), original.getId())
                && !displacedGroupIds.contains(node.getId()))) {
            return false;
        }

        Map<Long, AssetFinancials> financials = buildFinancials(nodes.stream()
                .map(EquipUpgradeNode::getAssetId).filter(Objects::nonNull).collect(Collectors.toSet()));
        List<EquipUpgradeNode> chronologicalMainline = nodes.stream()
                .filter(node -> Boolean.TRUE.equals(node.getMainline()) || Objects.equals(node.getId(), original.getId()))
                .filter(node -> Optional.ofNullable(financials.get(node.getAssetId()))
                        .map(AssetFinancials::primaryPurchaseDate).isPresent())
                .sorted(Comparator
                        .comparing((EquipUpgradeNode node) -> financials.get(node.getAssetId()).primaryPurchaseDate())
                        .thenComparing(EquipUpgradeNode::getId))
                .toList();
        if (chronologicalMainline.isEmpty()
                || !Objects.equals(chronologicalMainline.get(0).getId(), original.getId())) {
            return false;
        }
        int targetLevel = chronologicalMainline.indexOf(displaced) + 1;
        if (targetLevel <= 1) {
            return false;
        }

        // 为目标代际腾出位置，再恢复被误伤的第一代主物品；随后统一重建 SEQUENCE 关系。
        nodeMapper.increaseLevelFrom(routeId, targetLevel);
        for (EquipUpgradeNode groupNode : displacedGroup) {
            nodeMapper.updateLayout(groupNode.getId(), targetLevel,
                    Boolean.TRUE.equals(groupNode.getMainline()) ? 0 : Optional.ofNullable(groupNode.getSort()).orElse(1));
        }
        nodeMapper.updateLayout(original.getId(), 1, 0);
        nodeMapper.updateMainline(original.getId(), true);
        rebuildMainline(routeId);
        return true;
    }

    /** 汇总购买与出售数据；主商品出售仅选择 sale_scope=ASSET，配件出售只进入路线总收入。 */
    private Map<Long, AssetFinancials> buildFinancials(Collection<Long> assetIds) {
        Map<Long, AssetFinancials> result = new HashMap<>();
        for (Long assetId : assetIds) {
            List<Purchase> purchases = purchaseMapper.findByAssetId(assetId);
            List<Sale> sales = saleMapper.findByAssetId(assetId);
            BigDecimal totalSpend = purchases.stream().map(this::purchaseAmount).reduce(zero(), BigDecimal::add);
            List<Purchase> primaryPurchases = purchases.stream().filter(p -> "PRIMARY".equals(p.getType())).toList();
            BigDecimal primarySpend = primaryPurchases.stream().map(this::purchaseAmount).reduce(zero(), BigDecimal::add);
            LocalDate primaryDate = primaryPurchases.stream().map(Purchase::getPurchaseDate).filter(Objects::nonNull)
                    .min(Comparator.naturalOrder()).orElse(null);
            Sale mainSale = sales.stream().filter(s -> SALE_SCOPE_ASSET.equals(s.getSaleScope()))
                    .max(Comparator.comparing(Sale::getSaleDate, Comparator.nullsLast(Comparator.naturalOrder()))).orElse(null);
            BigDecimal allSalesIncome = sales.stream().map(this::netIncome).reduce(zero(), BigDecimal::add);
            result.put(assetId, new AssetFinancials(money(totalSpend), money(primarySpend), primaryDate, mainSale, money(allSalesIncome)));
        }
        return result;
    }

    private BigDecimal purchaseAmount(Purchase purchase) {
        return safe(purchase.getPrice()).add(safe(purchase.getShippingCost()));
    }

    private BigDecimal netIncome(Sale sale) {
        if (sale.getNetIncome() != null) {
            return sale.getNetIncome();
        }
        return safe(sale.getSalePrice()).subtract(safe(sale.getFee())).subtract(safe(sale.getShippingCost())).subtract(safe(sale.getOtherCost()));
    }

    private Long calculateUseDays(LocalDate purchaseDate, LocalDate saleDate, List<String> warnings) {
        if (purchaseDate == null) {
            return null;
        }
        LocalDate endDate = saleDate == null ? LocalDate.now() : saleDate;
        long days = ChronoUnit.DAYS.between(purchaseDate, endDate);
        if (days < 0) {
            warnings.add("出售日期早于购买日期");
        }
        return Math.max(0, days);
    }

    private void ensureAssetIsNotDuplicated(Long routeId, Long assetId, Long currentNodeId) {
        if (assetId == null) {
            return;
        }
        EquipUpgradeNode existing = nodeMapper.findActiveByRouteAndAsset(routeId, assetId);
        if (existing != null && !Objects.equals(existing.getId(), currentNodeId)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "同一物品不能在同一路线中重复添加");
        }
    }

    private EquipUpgradeRoute requireRoute(Long id) {
        return Optional.ofNullable(routeMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.UPGRADE_ROUTE_NOT_FOUND));
    }

    private EquipUpgradeRoute requireEditableRoute(Long id) {
        EquipUpgradeRoute route = requireRoute(id);
        if (ROUTE_ARCHIVED.equals(route.getStatus())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "已归档路线只读，请先恢复为进行中");
        }
        return route;
    }

    private DeviceAsset requireAsset(Long assetId) {
        return Optional.ofNullable(assetMapper.findById(assetId))
                .orElseThrow(() -> new BizException(ErrorCode.ASSET_NOT_FOUND, "关联物品不存在"));
    }

    /** 主物品是路线展示状态，不创建新节点，故必须引用已存在的真实物品节点。 */
    private void requireAssetInRoute(Long routeId, Long assetId) {
        if (nodeMapper.findActiveByRouteAndAsset(routeId, assetId) == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "当前主物品必须先添加到这条路线");
        }
    }

    private EquipUpgradeNode requireNodeInRoute(Long nodeId, Long routeId) {
        EquipUpgradeNode node = Optional.ofNullable(nodeMapper.findById(nodeId))
                .orElseThrow(() -> new BizException(ErrorCode.UPGRADE_NODE_NOT_FOUND));
        if (!Objects.equals(routeId, node.getRouteId())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "节点不属于当前路线");
        }
        return node;
    }

    private Map<Long, DeviceAsset> loadAssets(Set<Long> assetIds) {
        if (assetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return assetMapper.findByIds(new ArrayList<>(assetIds)).stream()
                .collect(Collectors.toMap(DeviceAsset::getId, item -> item));
    }

    private int calculateLevel(EquipUpgradeNode anchor, String position) {
        int anchorLevel = Optional.ofNullable(anchor.getLevel()).orElse(1);
        return switch (position) {
            case "BEFORE" -> Math.max(1, anchorLevel - 1);
            case "AFTER" -> anchorLevel + 1;
            default -> anchorLevel;
        };
    }

    private String resolveNodeType(EquipUpgradeNodeRequest request) {
        if (StringUtils.hasText(request.getNodeType())) {
            return request.getNodeType();
        }
        return request.getAssetId() != null ? NODE_ASSET : request.getWishlistId() != null ? NODE_WISHLIST : NODE_PLANNED;
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private BigDecimal safe(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal zero() {
        return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return safe(value).setScale(2, RoundingMode.HALF_UP);
    }

    /** 单个真实物品的路线计算缓存，避免节点与连线使用不同的金额口径。 */
    private record AssetFinancials(BigDecimal totalSpend, BigDecimal primarySpend, LocalDate primaryPurchaseDate,
                                   Sale mainSale, BigDecimal allSalesNetIncome) {
    }
}
