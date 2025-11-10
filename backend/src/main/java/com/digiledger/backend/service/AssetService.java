package com.digiledger.backend.service;

import com.digiledger.backend.model.dto.AssetSummaryDTO;
import com.digiledger.backend.model.entity.DeviceAsset;

import java.util.List;

public interface AssetService {

    List<AssetSummaryDTO> listAssets();

    DeviceAsset getAsset(Long id);
}
