package com.digiledger.backend.model.dto.asset;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssetStatusUpdateRequest {

    @NotBlank(message = "状态不能为空")
    private String status;
}
