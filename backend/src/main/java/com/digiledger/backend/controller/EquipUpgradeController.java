package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.model.dto.upgrade.EquipUpgradeLinkRequest;
import com.digiledger.backend.model.dto.upgrade.EquipUpgradeNodeRequest;
import com.digiledger.backend.model.dto.upgrade.EquipUpgradeRouteDTO;
import com.digiledger.backend.model.dto.upgrade.EquipUpgradeRouteRequest;
import com.digiledger.backend.model.dto.upgrade.UpgradeRouteGraphDTO;
import com.digiledger.backend.service.EquipUpgradeService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 装备升级路线图接口。
 */
@RestController
@RequestMapping("/api/upgrade-routes")
public class EquipUpgradeController {

    private final EquipUpgradeService equipUpgradeService;

    public EquipUpgradeController(EquipUpgradeService equipUpgradeService) {
        this.equipUpgradeService = equipUpgradeService;
    }

    /**
     * 升级路线列表。
     */
    @GetMapping
    public ApiResponse<List<EquipUpgradeRouteDTO>> listRoutes() {
        return ApiResponse.success(equipUpgradeService.listRoutes());
    }

    /**
     * 新建升级路线。
     */
    @PostMapping
    public ApiResponse<Long> createRoute(@RequestBody @Valid EquipUpgradeRouteRequest request) {
        return ApiResponse.success(equipUpgradeService.createRoute(request));
    }

    /**
     * 更新升级路线。
     */
    @PutMapping("/{id}")
    public ApiResponse<Void> updateRoute(@PathVariable("id") @NotNull @Min(1) Long id,
                                         @RequestBody @Valid EquipUpgradeRouteRequest request) {
        equipUpgradeService.updateRoute(id, request);
        return ApiResponse.success();
    }

    /**
     * 删除升级路线（逻辑删除）。
     */
    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteRoute(@PathVariable("id") @NotNull @Min(1) Long id) {
        equipUpgradeService.deleteRoute(id);
        return ApiResponse.success();
    }

    /**
     * 查询整条路线的图结构。
     */
    @GetMapping("/{routeId}/graph")
    public ApiResponse<UpgradeRouteGraphDTO> getGraph(@PathVariable("routeId") @NotNull @Min(1) Long routeId) {
        return ApiResponse.success(equipUpgradeService.getRouteGraph(routeId));
    }

    /**
     * 在路线中新增节点。
     */
    @PostMapping("/{routeId}/nodes")
    public ApiResponse<Long> addNode(@PathVariable("routeId") @NotNull @Min(1) Long routeId,
                                     @RequestBody @Valid EquipUpgradeNodeRequest request) {
        return ApiResponse.success(equipUpgradeService.addNode(routeId, request));
    }

    /** 完整更新节点；保留 POST 新增语义，避免旧客户端受影响。 */
    @PutMapping("/{routeId}/nodes/{nodeId}")
    public ApiResponse<Void> updateNode(@PathVariable("routeId") @Min(1) Long routeId,
                                        @PathVariable("nodeId") @Min(1) Long nodeId,
                                        @RequestBody @Valid EquipUpgradeNodeRequest request) {
        equipUpgradeService.updateNode(routeId, nodeId, request);
        return ApiResponse.success();
    }

    /**
     * 删除节点及关联关系。
     */
    @DeleteMapping("/{routeId}/nodes/{nodeId}")
    public ApiResponse<Void> deleteNode(@PathVariable("routeId") @NotNull @Min(1) Long routeId,
                                        @PathVariable("nodeId") @NotNull @Min(1) Long nodeId) {
        equipUpgradeService.deleteNode(routeId, nodeId);
        return ApiResponse.success();
    }

    /**
     * 新增升级关系。
     */
    @PostMapping("/{routeId}/links")
    public ApiResponse<Long> addLink(@PathVariable("routeId") @NotNull @Min(1) Long routeId,
                                     @RequestBody @Valid EquipUpgradeLinkRequest request) {
        return ApiResponse.success(equipUpgradeService.addLink(routeId, request));
    }

    /**
     * 删除升级关系。
     */
    @DeleteMapping("/{routeId}/links/{linkId}")
    public ApiResponse<Void> deleteLink(@PathVariable("routeId") @NotNull @Min(1) Long routeId,
                                        @PathVariable("linkId") @NotNull @Min(1) Long linkId) {
        equipUpgradeService.deleteLink(routeId, linkId);
        return ApiResponse.success();
    }
}
