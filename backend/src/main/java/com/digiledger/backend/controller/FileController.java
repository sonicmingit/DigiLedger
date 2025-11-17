package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.FileAttachmentMapper;
import com.digiledger.backend.model.dto.asset.CoverApplyResponse;
import com.digiledger.backend.model.dto.asset.RemoveBgRequest;
import com.digiledger.backend.model.dto.asset.RemoveBgResponse;
import com.digiledger.backend.model.dto.attachment.AttachmentResponse;
import com.digiledger.backend.service.AttachmentService;
import com.digiledger.backend.service.FileService;
import com.digiledger.backend.service.impl.AssetCoverService;
import com.digiledger.backend.service.impl.BackgroundRemovalService;
import com.digiledger.backend.util.InMemoryMultipartFile;
import com.digiledger.backend.util.StoragePathHelper;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.StringJoiner;

/**
 * 文件上传相关接口
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final StoragePathHelper storagePathHelper;
    private final AssetCoverService assetCoverService;
    private final FileAttachmentMapper fileAttachmentMapper;
    private final AttachmentService attachmentService;
    private final BackgroundRemovalService backgroundRemovalService;


    public FileController(FileService fileService,
                          StoragePathHelper storagePathHelper,
                          AssetCoverService assetCoverService,
                          FileAttachmentMapper fileAttachmentMapper,
                          AttachmentService attachmentService,
                          BackgroundRemovalService backgroundRemovalService) {
        this.fileService = fileService;
        this.storagePathHelper = storagePathHelper;
        this.assetCoverService = assetCoverService;
        this.fileAttachmentMapper = fileAttachmentMapper;
        this.attachmentService = attachmentService;
        this.backgroundRemovalService = backgroundRemovalService;
    }

    @PostMapping("/upload")
    public ApiResponse<Map<String, String>> upload(@RequestParam(name = "file") MultipartFile file) {
        String objectKey = fileService.upload(file);
        String publicUrl = storagePathHelper.toFullUrl(objectKey);
        return ApiResponse.success(Map.of(
                "objectKey", objectKey,
                "url", publicUrl != null ? publicUrl : ""
        ));
    }

    @PostMapping("/remove-bg")
    public ApiResponse<RemoveBgResponse> removeBackground(@RequestBody @Valid RemoveBgRequest request) {
        String objectKey = resolveObjectKey(request);
        CoverApplyResponse response = assetCoverService.removeBackground(request.getAssetId(), objectKey);
        return ApiResponse.success(new RemoveBgResponse(response.attachmentId(), response.url()));
    }

    @PostMapping("/remove-bg-preview")
    public ApiResponse<RemoveBgResponse> previewRemoveBackground(@RequestBody @Valid RemoveBgRequest request) {
        String objectKey = resolveObjectKey(request);
        byte[] origin = fileService.download(objectKey);
        byte[] result = backgroundRemovalService.removeBackground(request.getAssetId(), origin, "no-bg-preview.png");
        String extra = buildExtra("remove-bg-preview", objectKey);
        AttachmentResponse attachment = attachmentService.upload(
                new InMemoryMultipartFile("file", "no-bg-preview.png", "image/png", result),
                "ASSET_COVER",
                request.getAssetId(),
                extra
        );
        String url = storagePathHelper.toFullUrl(attachment.objectKey());
        return ApiResponse.success(new RemoveBgResponse(attachment.id(), url));
    }

    private String resolveObjectKey(RemoveBgRequest request) {
        if (request.getAttachmentId() != null) {
            var attachment = fileAttachmentMapper.findById(request.getAttachmentId());
            if (attachment == null) {
                throw new BizException(ErrorCode.ATTACHMENT_NOT_FOUND, "附件不存在");
            }
            return attachment.getObjectKey();
        }
        String coverUrl = request.getCoverUrl();
        if (coverUrl != null && !coverUrl.isBlank()) {
            String objectKey = storagePathHelper.toObjectKey(coverUrl);
            if (objectKey == null || objectKey.isBlank()) {
                throw new BizException(ErrorCode.VALIDATION_ERROR, "封面地址无效");
            }
            return objectKey;
        }
        throw new BizException(ErrorCode.VALIDATION_ERROR, "请提供封面附件或封面地址");
    }

    private String buildExtra(String type, String objectKey) {
        return assetCoverService.toExtra(Map.of("type", type, "objectKey", objectKey));
    }

}
