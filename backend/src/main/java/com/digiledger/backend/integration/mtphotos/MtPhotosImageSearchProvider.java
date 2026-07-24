package com.digiledger.backend.integration.mtphotos;

import com.digiledger.backend.integration.cover.ImageSearchProvider;
import com.digiledger.backend.integration.externalapi.service.ExternalApiConfigServiceImpl;
import com.digiledger.backend.model.cover.CoverCandidate;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/** 将 MT Photos 作为统一的封面搜图提供方，缩略图继续由本服务后端代理。 */
@Component
@Order(1)
public class MtPhotosImageSearchProvider implements ImageSearchProvider {
    private final MtPhotosService mtPhotosService;

    public MtPhotosImageSearchProvider(MtPhotosService mtPhotosService) {
        this.mtPhotosService = mtPhotosService;
    }

    @Override
    public String getName() {
        return ExternalApiConfigServiceImpl.MT_PHOTOS;
    }

    @Override
    public String getDisplayName() {
        return "MT Photos 图库";
    }

    @Override
    public String getDescription() {
        return "在已接入的私有图库中按文件名、元数据或文本信息检索照片。";
    }

    @Override
    public List<CoverCandidate> search(String query, int limit) {
        MtPhotosSearchResponse response = mtPhotosService.testSearch(
                new MtPhotosSearchRequest(query, "CLIP", 1));
        return response.items().stream()
                .limit(Math.max(1, limit))
                .map(item -> new CoverCandidate(
                        item.thumbnailUrl(),
                        item.thumbnailUrl(),
                        getName(),
                        item.fileName() == null ? "MT Photos 文件 #" + item.id() : item.fileName(),
                        Map.of("sourceLabel", getDisplayName(), "fileId", item.id())
                ))
                .toList();
    }
}
