package com.digiledger.backend.service;

import com.digiledger.backend.model.dto.upgrade.*;

import java.util.List;

/**
 * 装备升级路线图服务。
 */
public interface EquipUpgradeService {

    List<EquipUpgradeRouteDTO> listRoutes();

    Long createRoute(EquipUpgradeRouteRequest request);

    void updateRoute(Long id, EquipUpgradeRouteRequest request);

    void deleteRoute(Long id);

    Long addNode(Long routeId, EquipUpgradeNodeRequest request);
    void updateNode(Long routeId, Long nodeId, EquipUpgradeNodeRequest request);

    void deleteNode(Long routeId, Long nodeId);

    Long addLink(Long routeId, EquipUpgradeLinkRequest request);

    void deleteLink(Long routeId, Long linkId);

    UpgradeRouteGraphDTO getRouteGraph(Long routeId);
}
