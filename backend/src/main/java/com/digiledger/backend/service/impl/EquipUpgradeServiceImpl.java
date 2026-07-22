package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.mapper.EquipUpgradeLinkMapper;
import com.digiledger.backend.mapper.EquipUpgradeNodeMapper;
import com.digiledger.backend.mapper.EquipUpgradeRouteMapper;
import com.digiledger.backend.mapper.PurchaseMapper;
import com.digiledger.backend.mapper.SaleMapper;
import com.digiledger.backend.model.dto.upgrade.EquipUpgradeLinkRequest;
import com.digiledger.backend.model.dto.upgrade.EquipUpgradeNodeRequest;
import com.digiledger.backend.model.dto.upgrade.EquipUpgradeRouteDTO;
import com.digiledger.backend.model.dto.upgrade.EquipUpgradeRouteRequest;
import com.digiledger.backend.model.dto.upgrade.UpgradeGraphLinkDTO;
import com.digiledger.backend.model.dto.upgrade.UpgradeGraphNodeDTO;
import com.digiledger.backend.model.dto.upgrade.UpgradeRouteGraphDTO;
import com.digiledger.backend.model.entity.DeviceAsset;
import com.digiledger.backend.model.entity.EquipUpgradeLink;
import com.digiledger.backend.model.entity.EquipUpgradeNode;
import com.digiledger.backend.model.entity.EquipUpgradeRoute;
import com.digiledger.backend.model.entity.Sale;
import com.digiledger.backend.service.EquipUpgradeService;
import com.digiledger.backend.util.StoragePathHelper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 装备升级路线图服务实现。
 */
@Service
public class EquipUpgradeServiceImpl implements EquipUpgradeService {

    private final EquipUpgradeRouteMapper routeMapper;
    private final EquipUpgradeNodeMapper nodeMapper;
    private final EquipUpgradeLinkMapper linkMapper;
    private final AssetMapper assetMapper;
    private final PurchaseMapper purchaseMapper;
    private final SaleMapper saleMapper;
    private final StoragePathHelper storagePathHelper;

    public EquipUpgradeServiceImpl(EquipUpgradeRouteMapper routeMapper,
                                   EquipUpgradeNodeMapper nodeMapper,
                                   EquipUpgradeLinkMapper linkMapper,
                                   AssetMapper assetMapper,
                                   PurchaseMapper purchaseMapper,
                                   SaleMapper saleMapper,
                                   StoragePathHelper storagePathHelper) {
        this.routeMapper = routeMapper;
        this.nodeMapper = nodeMapper;
        this.linkMapper = linkMapper;
        this.assetMapper = assetMapper;
        this.purchaseMapper = purchaseMapper;
        this.saleMapper = saleMapper;
        this.storagePathHelper = storagePathHelper;
    }

    @Override
    public List<EquipUpgradeRouteDTO> listRoutes() {
        List<EquipUpgradeRoute> routes = routeMapper.findAll();
        Map<Long, DeviceAsset> assetMap = loadAssets(routes.stream()
                .map(EquipUpgradeRoute::getRootAssetId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet()));
        return routes.stream()
                .map(route -> {
                    List<EquipUpgradeNode> nodes = nodeMapper.findByRouteId(route.getId());
                    BigDecimal planned = nodes.stream().map(EquipUpgradeNode::getPlannedBudget)
                            .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    BigDecimal recovery = nodes.stream().map(EquipUpgradeNode::getExpectedRecovery)
                            .filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
                    Map<String, Long> statuses = nodes.stream().collect(Collectors.groupingBy(
                            node -> Optional.ofNullable(node.getStatus()).orElse("PLANNED"), Collectors.counting()));
                    return new EquipUpgradeRouteDTO(
                        route.getId(),
                        route.getName(),
                        route.getRootAssetId(),
                        resolveAssetName(assetMap.get(route.getRootAssetId())),
                        route.getRemark(),
                        route.getPlanYear(),
                        route.getAnnualBudget(),
                        planned,
                        recovery,
                        statuses,
                        route.getUpdatedAt()
                    );
                })
                .toList();
    }

    @Override
    @Transactional
    public Long createRoute(EquipUpgradeRouteRequest request) {
        DeviceAsset root = null;
        if (request.getRootAssetId() != null) {
            root = requireAsset(request.getRootAssetId());
        }
        EquipUpgradeRoute route = new EquipUpgradeRoute();
        route.setName(request.getName());
        route.setRootAssetId(root != null ? root.getId() : null);
        route.setRemark(request.getRemark());
        route.setPlanYear(request.getPlanYear());
        route.setAnnualBudget(request.getAnnualBudget());
        route.setIsDeleted(0);
        routeMapper.insert(route);
        return route.getId();
    }

    @Override
    @Transactional
    public void updateRoute(Long id, EquipUpgradeRouteRequest request) {
        EquipUpgradeRoute route = requireRoute(id);
        if (request.getRootAssetId() != null) {
            requireAsset(request.getRootAssetId());
        }
        route.setName(request.getName());
        route.setRootAssetId(request.getRootAssetId());
        route.setRemark(request.getRemark());
        route.setPlanYear(request.getPlanYear());
        route.setAnnualBudget(request.getAnnualBudget());
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

    @Override
    @Transactional
    public Long addNode(Long routeId, EquipUpgradeNodeRequest request) {
        requireRoute(routeId);
        if (request.getAssetId() != null) {
            requireAsset(request.getAssetId());
        }
        EquipUpgradeNode node = new EquipUpgradeNode();
        node.setRouteId(routeId);
        applyNodeRequest(node, request);
        node.setIsDeleted(0);
        nodeMapper.insert(node);
        return node.getId();
    }

    @Override
    @Transactional
    public void updateNode(Long routeId, Long nodeId, EquipUpgradeNodeRequest request) {
        requireRoute(routeId);
        EquipUpgradeNode node = requireNodeInRoute(nodeId, routeId);
        if (request.getAssetId() != null) {
            requireAsset(request.getAssetId());
        }
        applyNodeRequest(node, request);
        nodeMapper.update(node);
    }

    private void applyNodeRequest(EquipUpgradeNode node, EquipUpgradeNodeRequest request) {
        if (request.getAssetId() == null && !StringUtils.hasText(request.getTargetName())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "关联资产与目标物品名称至少填写一项");
        }
        node.setAssetId(request.getAssetId());
        node.setLevel(Optional.ofNullable(request.getLevel()).orElse(1));
        node.setSort(Optional.ofNullable(request.getSort()).orElse(0));
        node.setLabel(request.getLabel());
        node.setRemark(request.getRemark());
        node.setTitle(request.getTitle());
        node.setTargetName(request.getTargetName());
        node.setPeriodLabel(request.getPeriodLabel());
        node.setPlannedBudget(request.getPlannedBudget());
        node.setExpectedRecovery(request.getExpectedRecovery());
        node.setStatus(Optional.ofNullable(request.getStatus()).orElse("PLANNED"));
    }

    @Override
    @Transactional
    public void deleteNode(Long routeId, Long nodeId) {
        requireRoute(routeId);
        EquipUpgradeNode node = Optional.ofNullable(nodeMapper.findById(nodeId))
                .orElseThrow(() -> new BizException(ErrorCode.UPGRADE_NODE_NOT_FOUND));
        if (!Objects.equals(routeId, node.getRouteId())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "节点不属于当前路线");
        }
        linkMapper.softDeleteByNodeId(nodeId);
        nodeMapper.softDelete(nodeId);
    }

    @Override
    @Transactional
    public Long addLink(Long routeId, EquipUpgradeLinkRequest request) {
        requireRoute(routeId);
        EquipUpgradeNode fromNode = requireNodeInRoute(request.getFromNodeId(), routeId);
        EquipUpgradeNode toNode = requireNodeInRoute(request.getToNodeId(), routeId);
        if (Objects.equals(fromNode.getId(), toNode.getId())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "前后代节点不能相同");
        }
        boolean exists = linkMapper.findByRouteId(routeId).stream()
                .anyMatch(link -> Objects.equals(link.getFromNodeId(), fromNode.getId())
                        && Objects.equals(link.getToNodeId(), toNode.getId())
                        && Objects.equals(link.getIsDeleted(), 0));
        if (exists) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "升级关系已存在");
        }
        EquipUpgradeLink link = new EquipUpgradeLink();
        link.setRouteId(routeId);
        link.setFromNodeId(fromNode.getId());
        link.setToNodeId(toNode.getId());
        link.setRemark(request.getRemark());
        link.setIsDeleted(0);
        linkMapper.insert(link);
        return link.getId();
    }

    @Override
    @Transactional
    public void deleteLink(Long routeId, Long linkId) {
        requireRoute(routeId);
        EquipUpgradeLink link = Optional.ofNullable(linkMapper.findById(linkId))
                .orElseThrow(() -> new BizException(ErrorCode.UPGRADE_LINK_NOT_FOUND));
        if (!Objects.equals(routeId, link.getRouteId())) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "升级关系不属于当前路线");
        }
        linkMapper.softDelete(linkId);
    }

    @Override
    public UpgradeRouteGraphDTO getRouteGraph(Long routeId) {
        EquipUpgradeRoute route = requireRoute(routeId);
        List<EquipUpgradeNode> nodes = nodeMapper.findByRouteId(routeId);
        List<EquipUpgradeLink> links = linkMapper.findByRouteId(routeId);
        if (CollectionUtils.isEmpty(nodes)) {
            return new UpgradeRouteGraphDTO(route.getId(), route.getName(), route.getRemark(),
                    route.getPlanYear(), route.getAnnualBudget(), Collections.emptyList(), Collections.emptyList());
        }
        Map<Long, DeviceAsset> assetMap = loadAssets(nodes.stream().map(EquipUpgradeNode::getAssetId)
                .filter(Objects::nonNull).collect(Collectors.toSet()));
        Map<Long, BigDecimal> investMap = buildInvestMap(assetMap.keySet());
        Map<Long, Sale> saleMap = buildSaleMap(assetMap.keySet());
        Map<Long, EquipUpgradeNode> nodeMap = nodes.stream().collect(Collectors.toMap(EquipUpgradeNode::getId, n -> n));

        List<UpgradeGraphNodeDTO> nodeDTOs = nodes.stream()
                .map(node -> toNodeDTO(node, assetMap, investMap, saleMap))
                .toList();

        List<UpgradeGraphLinkDTO> linkDTOs = links.stream()
                .filter(link -> Objects.equals(link.getIsDeleted(), 0))
                .map(link -> new UpgradeGraphLinkDTO(
                        link.getId(),
                        link.getFromNodeId(),
                        link.getToNodeId(),
                        calculateStepCost(link, nodeMap, investMap, saleMap, assetMap),
                        link.getRemark()
                ))
                .toList();
        return new UpgradeRouteGraphDTO(route.getId(), route.getName(), route.getRemark(),
                route.getPlanYear(), route.getAnnualBudget(), nodeDTOs, linkDTOs);
    }

    private EquipUpgradeRoute requireRoute(Long id) {
        return Optional.ofNullable(routeMapper.findById(id))
                .orElseThrow(() -> new BizException(ErrorCode.UPGRADE_ROUTE_NOT_FOUND));
    }

    private DeviceAsset requireAsset(Long assetId) {
        return Optional.ofNullable(assetMapper.findById(assetId))
                .orElseThrow(() -> new BizException(ErrorCode.ASSET_NOT_FOUND, "关联资产不存在"));
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
        if (assetIds == null || assetIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return assetMapper.findByIds(assetIds.stream().toList()).stream()
                .collect(Collectors.toMap(DeviceAsset::getId, a -> a));
    }

    private Map<Long, BigDecimal> buildInvestMap(Collection<Long> assetIds) {
        Map<Long, BigDecimal> result = new HashMap<>();
        for (Long assetId : assetIds) {
            BigDecimal sum = Optional.ofNullable(purchaseMapper.sumInvestByAsset(assetId))
                    .orElse(BigDecimal.ZERO);
            result.put(assetId, sum.setScale(2, RoundingMode.HALF_UP));
        }
        return result;
    }

    private Map<Long, Sale> buildSaleMap(Collection<Long> assetIds) {
        Map<Long, Sale> result = new HashMap<>();
        for (Long assetId : assetIds) {
            Sale sale = saleMapper.findLatestByAsset(assetId);
            if (sale != null) {
                result.put(assetId, sale);
            }
        }
        return result;
    }

    private UpgradeGraphNodeDTO toNodeDTO(EquipUpgradeNode node,
                                          Map<Long, DeviceAsset> assetMap,
                                          Map<Long, BigDecimal> investMap,
                                          Map<Long, Sale> saleMap) {
        DeviceAsset asset = node.getAssetId() == null ? null : assetMap.get(node.getAssetId());
        if (node.getAssetId() != null && asset == null) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND, "节点关联资产不存在");
        }
        if (asset == null) {
            return new UpgradeGraphNodeDTO(node.getId(), null, node.getTargetName(), null,
                    BigDecimal.ZERO, BigDecimal.ZERO, false, null, null, node.getLevel(), node.getSort(),
                    node.getLabel(), node.getRemark(), node.getTitle(), node.getTargetName(), node.getPeriodLabel(),
                    node.getPlannedBudget(), node.getExpectedRecovery(), node.getStatus());
        }
        BigDecimal purchasePrice = investMap.getOrDefault(asset.getId(), BigDecimal.ZERO);
        Sale sale = saleMap.get(asset.getId());
        BigDecimal salePrice = sale == null ? BigDecimal.ZERO : Optional.ofNullable(sale.getSalePrice()).orElse(BigDecimal.ZERO);
        boolean sold = sale != null || "已出售".equals(asset.getStatus());
        return new UpgradeGraphNodeDTO(
                node.getId(),
                asset.getId(),
                asset.getName(),
                asset.getStatus(),
                purchasePrice,
                salePrice,
                sold,
                asset.getPurchaseDate(),
                storagePathHelper.toFullUrl(asset.getCoverImageUrl()),
                node.getLevel(),
                node.getSort(),
                node.getLabel(),
                node.getRemark(),
                node.getTitle(),
                node.getTargetName(),
                node.getPeriodLabel(),
                node.getPlannedBudget(),
                node.getExpectedRecovery(),
                node.getStatus()
        );
    }

    private BigDecimal calculateStepCost(EquipUpgradeLink link,
                                         Map<Long, EquipUpgradeNode> nodeMap,
                                         Map<Long, BigDecimal> investMap,
                                         Map<Long, Sale> saleMap,
                                         Map<Long, DeviceAsset> assetMap) {
        EquipUpgradeNode fromNode = nodeMap.get(link.getFromNodeId());
        EquipUpgradeNode toNode = nodeMap.get(link.getToNodeId());
        if (fromNode == null || toNode == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "升级关系关联的节点已失效");
        }
        DeviceAsset fromAsset = assetMap.get(fromNode.getAssetId());
        if (fromAsset == null || toNode.getAssetId() == null) {
            // 纯计划节点没有实际成交数据，使用计划预算/回收值提供可解释的预估差额。
            return Optional.ofNullable(toNode.getPlannedBudget()).orElse(BigDecimal.ZERO)
                    .subtract(Optional.ofNullable(fromNode.getExpectedRecovery()).orElse(BigDecimal.ZERO))
                    .setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal targetInvest = investMap.getOrDefault(toNode.getAssetId(), BigDecimal.ZERO);
        Sale sale = saleMap.get(fromAsset.getId());
        boolean sold = sale != null || "已出售".equals(fromAsset.getStatus());
        BigDecimal soldPrice = sale == null ? BigDecimal.ZERO : Optional.ofNullable(sale.getSalePrice()).orElse(BigDecimal.ZERO);
        BigDecimal cost = sold ? targetInvest.subtract(soldPrice) : targetInvest;
        return cost.setScale(2, RoundingMode.HALF_UP);
    }

    private String resolveAssetName(DeviceAsset asset) {
        return asset == null ? null : asset.getName();
    }
}
