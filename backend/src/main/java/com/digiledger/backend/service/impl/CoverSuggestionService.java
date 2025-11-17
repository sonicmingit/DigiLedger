package com.digiledger.backend.service.impl;

import com.digiledger.backend.model.dto.asset.CoverSuggestionDTO;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 封面图智能推荐
 */
@Service
public class CoverSuggestionService {



    public List<CoverSuggestionDTO> suggest(Long assetId, String query) {
        String keyword = buildKeyword(assetId, query);
        Set<String> used = new HashSet<>();
        List<CoverSuggestionDTO> suggestions = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            String seed = keyword + " seed " + i;
            String encoded = urlEncode(seed);
            if (used.contains(encoded)) {
                encoded = encoded + "-" + i;
            }
            used.add(encoded);
            // todo 修改图片引擎
            String thumb = String.format("https://picsum.photos/seed/%s/420/320", encoded);
            String source = String.format("https://picsum.photos/seed/%s/1600/1200", encoded);
            suggestions.add(new CoverSuggestionDTO(thumb, source));
        }
        return suggestions;
    }

    private String buildKeyword(Long assetId, String query) {
        List<String> parts = new ArrayList<>();
        if (StringUtils.hasText(query)) {
            parts.add(query.trim());
        }
        if (assetId != null) {
            parts.add("asset-" + assetId);
        }
        if (parts.isEmpty()) {
            parts.add("物品封面");
        }
        return String.join(" ", parts);
    }

    private String urlEncode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8).replace("+", "%20");
    }
}
