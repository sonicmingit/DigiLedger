package com.digiledger.backend.model.dto.upgrade;

import java.util.List;

/**
 * 升级路线图整体返回体。
 */
public record UpgradeRouteGraphDTO(
        Long routeId,
        String routeName,
        String remark,
        List<UpgradeGraphNodeDTO> nodes,
        List<UpgradeGraphLinkDTO> links
) {
}
