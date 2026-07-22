package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.model.dto.asset.AssetSummaryDTO;
import com.digiledger.backend.model.dto.export.DataExportDTO;
import com.digiledger.backend.service.AssetService;
import com.digiledger.backend.service.DataExportService;
import org.springframework.stereotype.Service;
import java.time.LocalDate;
import java.util.*;

@Service
/**
 * 导出服务复用资产列表的指标口径，避免 CSV/JSON 与页面展示出现两套计算规则。
 * CSV 的文本字段统一双引号转义，防止名称中的逗号或换行破坏列结构。
 */
public class DataExportServiceImpl implements DataExportService {
    private final AssetService assetService;
    public DataExportServiceImpl(AssetService assetService) { this.assetService = assetService; }
    @Override public DataExportDTO export(String format) {
        String normalized = Optional.ofNullable(format).orElse("json").trim().toLowerCase(Locale.ROOT);
        if (!Set.of("json", "csv").contains(normalized)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "format 仅支持 json 或 csv");
        }
        List<AssetSummaryDTO> assets = assetService.listAssets(null, null, null, null, null);
        String name = "digiledger-assets-" + LocalDate.now() + "." + normalized;
        if ("json".equals(normalized)) return new DataExportDTO("json", name, "application/json", assets);
        StringBuilder csv = new StringBuilder("id,name,status,categoryId,totalInvest,avgDailyCost,useDays,purchaseDate\r\n");
        for (AssetSummaryDTO a : assets) {
            csv.append(a.id()).append(',').append(escape(a.name())).append(',').append(escape(a.status())).append(',')
                    .append(a.categoryId() == null ? "" : a.categoryId()).append(',')
                    .append(a.totalInvest() == null ? "" : a.totalInvest()).append(',')
                    .append(a.avgCostPerDay() == null ? "" : a.avgCostPerDay()).append(',')
                    .append(a.useDays()).append(',').append(a.purchaseDate() == null ? "" : a.purchaseDate()).append("\r\n");
        }
        return new DataExportDTO("csv", name, "text/csv;charset=UTF-8", csv.toString());
    }
    private String escape(String value) {
        if (value == null) return "";
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
