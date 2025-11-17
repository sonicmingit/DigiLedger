package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.AssetMapper;
import com.digiledger.backend.model.dto.attachment.AttachmentResponse;
import com.digiledger.backend.model.dto.asset.CoverApplyResponse;
import com.digiledger.backend.service.AttachmentService;
import com.digiledger.backend.service.FileService;
import com.digiledger.backend.util.StoragePathHelper;
import jakarta.validation.constraints.NotNull;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Paths;

/**
 * 物品封面相关服务
 */
@Service
public class AssetCoverService {

    private final AssetMapper assetMapper;
    private final AttachmentService attachmentService;
    private final FileService fileService;
    private final BackgroundRemovalService backgroundRemovalService;
    private final StoragePathHelper storagePathHelper;

    public AssetCoverService(AssetMapper assetMapper,
                             AttachmentService attachmentService,
                             FileService fileService,
                             BackgroundRemovalService backgroundRemovalService,
                             StoragePathHelper storagePathHelper) {
        this.assetMapper = assetMapper;
        this.attachmentService = attachmentService;
        this.fileService = fileService;
        this.backgroundRemovalService = backgroundRemovalService;
        this.storagePathHelper = storagePathHelper;
    }

    public CoverApplyResponse setCoverFromUrl(Long assetId, @NotNull String sourceUrl) {
        validateAsset(assetId);
        MultipartFile file = downloadRemoteImage(sourceUrl);
        AttachmentResponse attachment = attachmentService.upload(file, "ASSET_COVER", assetId, sourceUrl);
        return updateCover(assetId, attachment);
    }

    public CoverApplyResponse removeBackground(Long assetId, String objectKey) {
        validateAsset(assetId);
        if (!StringUtils.isNotBlank(objectKey)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "当前封面地址不合法");
        }
        byte[] origin = fileService.download(objectKey);
        byte[] result = backgroundRemovalService.removeBackground(assetId, origin, "no-bg.png");
        AttachmentResponse attachment = attachmentService.upload(
                new MockMultipartFile("file", "no-bg.png", "image/png", result),
                "ASSET_COVER",
                assetId,
                "remove-bg"
        );
        return updateCover(assetId, attachment);
    }

    public CoverApplyResponse replaceCover(Long assetId, AttachmentResponse attachment) {
        validateAsset(assetId);
        return updateCover(assetId, attachment);
    }

    private CoverApplyResponse updateCover(Long assetId, AttachmentResponse attachment) {
        assetMapper.updateCoverImage(assetId, attachment.objectKey());
        String url = storagePathHelper.toFullUrl(attachment.objectKey());
        return new CoverApplyResponse(attachment.id(), url, attachment.objectKey());
    }

    private void validateAsset(Long assetId) {
        if (assetId == null || assetMapper.findById(assetId) == null) {
            throw new BizException(ErrorCode.ASSET_NOT_FOUND, "资产不存在");
        }
    }

    private MultipartFile downloadRemoteImage(String sourceUrl) {
        try {
            URL url = new URL(sourceUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setConnectTimeout(6_000);
            connection.setReadTimeout(10_000);
            try (InputStream inputStream = connection.getInputStream();
                 ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
                byte[] chunk = new byte[8192];
                int read;
                while ((read = inputStream.read(chunk)) != -1) {
                    buffer.write(chunk, 0, read);
                }
                String contentType = connection.getContentType();
                if (!StringUtils.isNotBlank(contentType)) {
                    contentType = "image/jpeg";
                }
                String fileName = extractFileName(url);
                return new MockMultipartFile("file", fileName, contentType, buffer.toByteArray());
            }
        } catch (Exception ex) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "下载远程封面失败: " + ex.getMessage());
        }
    }

    private String extractFileName(URL url) {
        String path = url.getPath();
        if (StringUtils.isNotBlank(path)) {
            String fileName = Paths.get(path).getFileName().toString();
            if (StringUtils.isNotBlank(fileName)) {
                return fileName;
            }
        }
        return "cover-image";
    }
}
