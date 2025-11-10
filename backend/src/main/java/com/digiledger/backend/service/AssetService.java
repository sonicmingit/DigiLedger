package com.digiledger.backend.service;

import com.digiledger.backend.model.dto.asset.AssetCreateRequest;
import com.digiledger.backend.model.dto.asset.AssetDetailDTO;
import com.digiledger.backend.model.dto.asset.AssetSellRequest;
import com.digiledger.backend.model.dto.asset.AssetSummaryDTO;
import com.digiledger.backend.model.dto.asset.SaleDTO;

import java.util.List;

public interface AssetService {

    List<AssetSummaryDTO> listAssets(String status, String keyword);

    AssetDetailDTO getAssetDetail(Long id);

    Long createAsset(AssetCreateRequest request);

    void updateAsset(Long id, AssetCreateRequest request);

    void deleteAsset(Long id);

    SaleDTO sellAsset(Long id, AssetSellRequest request);
}
