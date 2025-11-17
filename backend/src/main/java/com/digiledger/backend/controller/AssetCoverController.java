package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.model.cover.CoverCandidate;
import com.digiledger.backend.model.dto.asset.CoverApplyResponse;
import com.digiledger.backend.model.dto.asset.CoverFromUrlRequest;
import com.digiledger.backend.model.dto.asset.CoverSuggestionDTO;
import com.digiledger.backend.service.impl.AssetCoverService;
import com.digiledger.backend.service.impl.CoverSuggestionService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 封面相关接口
 */
@RestController
@RequestMapping("/api/assets/{assetId}/cover")
public class AssetCoverController {

    private final CoverSuggestionService suggestionService;
    private final AssetCoverService coverService;

    public AssetCoverController(CoverSuggestionService suggestionService,
                                AssetCoverService coverService) {
        this.suggestionService = suggestionService;
        this.coverService = coverService;
    }

    @GetMapping("/suggestions")
    public ApiResponse<List<CoverSuggestionDTO>> suggestions(@PathVariable("assetId") Long assetId,
                                                             @RequestParam(name = "query", required = false) String query) {
        List<CoverCandidate> candidates = suggestionService.getCoverCandidatesForAsset(assetId, query);
        return ApiResponse.success(candidates.stream().map(CoverSuggestionDTO::fromCandidate).toList());
    }

    @PostMapping("/from-url")
    public ApiResponse<CoverApplyResponse> fromUrl(@PathVariable("assetId") Long assetId,
                                                   @RequestBody @Valid CoverFromUrlRequest request) {
        return ApiResponse.success(coverService.setCoverFromUrl(assetId, request.getSourceUrl()));
    }
}
