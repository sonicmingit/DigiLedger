package com.digiledger.backend.integration.cover;

import com.digiledger.backend.integration.mtphotos.MtPhotosService;
import com.digiledger.backend.integration.mtphotos.MtPhotosThumbnail;
import com.digiledger.backend.service.impl.AssetCoverService;
import com.digiledger.backend.util.InMemoryMultipartFile;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 将统一搜图结果转换成可写入本地对象存储的文件。
 * MT Photos 返回的是本系统鉴权代理地址，不能交给通用 HTTP 下载器回环访问。
 */
@Service
public class ExternalImageImportService {

    private static final Pattern MT_PHOTOS_THUMBNAIL_PATH = Pattern.compile(
            "^(?:https?://[^/]+)?/api/external-api-configs/MT_PHOTOS/thumbnail/(\\d+)(?:[?#].*)?$",
            Pattern.CASE_INSENSITIVE
    );

    private final MtPhotosService mtPhotosService;
    private final AssetCoverService assetCoverService;

    public ExternalImageImportService(MtPhotosService mtPhotosService, AssetCoverService assetCoverService) {
        this.mtPhotosService = mtPhotosService;
        this.assetCoverService = assetCoverService;
    }

    public MultipartFile download(String sourceUrl) {
        Matcher matcher = MT_PHOTOS_THUMBNAIL_PATH.matcher(sourceUrl.trim());
        if (!matcher.matches()) {
            return assetCoverService.downloadRemoteImage(sourceUrl);
        }

        long fileId = Long.parseLong(matcher.group(1));
        MtPhotosThumbnail thumbnail = mtPhotosService.getThumbnail(fileId);
        return new InMemoryMultipartFile(
                "file",
                "mt-photos-" + fileId + extensionOf(thumbnail.contentType()),
                thumbnail.contentType(),
                thumbnail.content()
        );
    }

    private String extensionOf(String contentType) {
        if (contentType == null) return ".jpg";
        return switch (contentType.toLowerCase()) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            case "image/gif" -> ".gif";
            default -> ".jpg";
        };
    }
}
