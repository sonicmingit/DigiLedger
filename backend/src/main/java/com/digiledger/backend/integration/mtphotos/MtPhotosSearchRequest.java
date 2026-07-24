package com.digiledger.backend.integration.mtphotos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record MtPhotosSearchRequest(
        @NotBlank(message = "请输入搜索词") @Size(max = 200, message = "搜索词不能超过 200 个字符") String query,
        String mode,
        Integer page
) {
}
