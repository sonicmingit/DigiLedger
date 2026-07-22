package com.digiledger.backend.model.dto.upgrade;

import java.util.List;
import java.math.BigDecimal;

/**
 * 升级路线图整体返回体。
 */
public record UpgradeRouteGraphDTO(
        Long routeId,
        String routeName,
        String remark,
        Integer planYear,
        BigDecimal annualBudget,
        List<UpgradeGraphNodeDTO> nodes,
        List<UpgradeGraphLinkDTO> links
) {
}
