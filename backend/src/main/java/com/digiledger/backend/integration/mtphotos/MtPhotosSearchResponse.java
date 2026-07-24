package com.digiledger.backend.integration.mtphotos;

import java.util.List;

public record MtPhotosSearchResponse(
        String mode,
        Integer totalCount,
        int page,
        int pageSize,
        int totalPages,
        List<MtPhotosSearchItem> items
) {
}
