package com.digiledger.backend.model.cover;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 封面候选信息
 */
public record CoverCandidate(
        String thumbnailUrl,
        String originalUrl,
        String source,
        String title,
        Map<String, Object> extra
) {

    public CoverCandidate {
        Map<String, Object> safeExtra = extra == null ? Map.of() : new LinkedHashMap<>(extra);
        safeExtra.values().removeIf(value -> value == null || (value instanceof String s && s.isBlank()));
        this.extra = Collections.unmodifiableMap(safeExtra);
    }
}
