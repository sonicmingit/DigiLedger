package com.digiledger.backend.model.dto.asset;

import java.util.List;

/** 物品中心的分页查询结果。 */
public record AssetPageDTO(List<AssetSummaryDTO> records, long total, int page, int pageSize) { }
