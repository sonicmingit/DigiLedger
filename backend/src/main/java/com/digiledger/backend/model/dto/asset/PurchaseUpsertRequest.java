package com.digiledger.backend.model.dto.asset;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PurchaseUpsertRequest extends PurchaseRequest {

    @NotNull(message = "资产ID不能为空")
    private Long assetId;
}
