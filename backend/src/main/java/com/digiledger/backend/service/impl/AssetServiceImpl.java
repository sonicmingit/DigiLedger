package com.digiledger.backend.service.impl;

import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.mapper.PurchaseMapper;
import com.digiledger.backend.mapper.SaleMapper;
import com.digiledger.backend.mapper.SystemSettingMapper;
import com.digiledger.backend.model.dto.AssetSummaryDTO;
import com.digiledger.backend.model.entity.DeviceAsset;
import com.digiledger.backend.model.entity.Sale;
import com.digiledger.backend.model.entity.SystemSetting;
import com.digiledger.backend.service.AssetService;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AssetServiceImpl implements AssetService {

    private final AssetMapper assetMapper;
    private final PurchaseMapper purchaseMapper;
    private final SaleMapper saleMapper;
    private final SystemSettingMapper systemSettingMapper;

    public AssetServiceImpl(AssetMapper assetMapper,
                            PurchaseMapper purchaseMapper,
                            SaleMapper saleMapper,
                            SystemSettingMapper systemSettingMapper) {
        this.assetMapper = assetMapper;
        this.purchaseMapper = purchaseMapper;
        this.saleMapper = saleMapper;
        this.systemSettingMapper = systemSettingMapper;
    }

    @Override
    public List<AssetSummaryDTO> listAssets() {
        SystemSetting setting = Optional.ofNullable(systemSettingMapper.findLatest())
                .orElseGet(() -> {
                    SystemSetting def = new SystemSetting();
                    def.setAnnualRate(0.5);
                    def.setCurrency("CNY");
                    return def;
                });

        return assetMapper.findAll().stream()
                .map(asset -> buildSummary(asset, setting.getAnnualRate()))
                .collect(Collectors.toList());
    }

    @Override
    public DeviceAsset getAsset(Long id) {
        return assetMapper.findById(id);
    }

    private AssetSummaryDTO buildSummary(DeviceAsset asset, double annualRate) {
        BigDecimal totalInvest = Optional.ofNullable(purchaseMapper.sumInvestByAsset(asset.getId()))
                .orElse(BigDecimal.ZERO);

        long useDays = calculateUseDays(asset);
        BigDecimal useDaysDecimal = BigDecimal.valueOf(Math.max(useDays, 1));
        BigDecimal avgCostPerDay = totalInvest.divide(useDaysDecimal, 2, RoundingMode.HALF_UP);

        BigDecimal accumulatedDepreciation = totalInvest
                .multiply(BigDecimal.valueOf(annualRate))
                .multiply(BigDecimal.valueOf(useDays).divide(BigDecimal.valueOf(365), 6, RoundingMode.HALF_UP));
        if (accumulatedDepreciation.compareTo(totalInvest) > 0) {
            accumulatedDepreciation = totalInvest;
        }
        BigDecimal bookValue = totalInvest.subtract(accumulatedDepreciation);
        if (bookValue.compareTo(BigDecimal.ZERO) < 0) {
            bookValue = BigDecimal.ZERO;
        }

        return new AssetSummaryDTO(
                asset.getId(),
                asset.getName(),
                asset.getCategory(),
                asset.getStatus(),
                totalInvest.setScale(2, RoundingMode.HALF_UP),
                avgCostPerDay,
                accumulatedDepreciation.setScale(2, RoundingMode.HALF_UP),
                bookValue.setScale(2, RoundingMode.HALF_UP),
                asset.getEnabledDate()
        );
    }

    private long calculateUseDays(DeviceAsset asset) {
        LocalDate enabledDate = Optional.ofNullable(asset.getEnabledDate())
                .orElse(Optional.ofNullable(asset.getPurchaseDate()).orElse(LocalDate.now()));
        String status = Optional.ofNullable(asset.getStatus()).orElse("IN_USE");
        LocalDate endDate = switch (status) {
            case "SOLD" -> Optional.ofNullable(asset.getSaleDate()).orElseGet(() -> {
                Sale sale = saleMapper.findLatestByAsset(asset.getId());
                return sale != null ? sale.getSaleDate() : LocalDate.now();
            });
            case "RETIRED" -> Optional.ofNullable(asset.getRetiredDate()).orElse(LocalDate.now());
            default -> LocalDate.now();
        };

        long diff = ChronoUnit.DAYS.between(enabledDate, endDate);
        if (diff <= 0) {
            return 1;
        }
        return diff;
    }
}
