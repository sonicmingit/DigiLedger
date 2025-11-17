package com.digiledger.backend.model.dto.asset;

import com.digiledger.backend.model.cover.CoverCandidate;

import java.util.Collections;
import java.util.Map;

/**
 * 封面候选图
 */
public record CoverSuggestionDTO(
        String thumbUrl,
        String sourceUrl,
        String source,
        String title,
        Map<String, Object> extra
) {

    public static CoverSuggestionDTO fromCandidate(CoverCandidate candidate) {
        Map<String, Object> extra = candidate.extra() == null ? Map.of() : candidate.extra();
        return new CoverSuggestionDTO(
                candidate.thumbnailUrl(),
                candidate.originalUrl(),
                candidate.source(),
                candidate.title(),
                Collections.unmodifiableMap(extra)
        );
    }
}
