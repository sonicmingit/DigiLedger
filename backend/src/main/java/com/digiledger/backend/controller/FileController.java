package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.FileAttachmentMapper;
import com.digiledger.backend.integration.cover.ExternalImageImportService;
import com.digiledger.backend.model.dto.asset.CoverApplyResponse;
import com.digiledger.backend.model.dto.asset.RemoveBgRequest;
import com.digiledger.backend.model.dto.asset.RemoveBgResponse;
import com.digiledger.backend.model.dto.attachment.AttachmentResponse;
import com.digiledger.backend.integration.removebg.BackgroundRemovalService;
import com.digiledger.backend.service.AttachmentService;
import com.digiledger.backend.service.FileService;
import com.digiledger.backend.service.impl.AssetCoverService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

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
    private final ExternalImageImportService externalImageImportService;


    public FileController(FileService fileService,
                          StoragePathHelper storagePathHelper,
                          AssetCoverService assetCoverService,
                          FileAttachmentMapper fileAttachmentMapper,
                          AttachmentService attachmentService,
                          BackgroundRemovalService backgroundRemovalService,
                          ExternalImageImportService externalImageImportService) {
        this.fileService = fileService;
        this.storagePathHelper = storagePathHelper;
        this.assetCoverService = assetCoverService;
        this.fileAttachmentMapper = fileAttachmentMapper;
        this.attachmentService = attachmentService;
        this.backgroundRemovalService = backgroundRemovalService;
        this.externalImageImportService = externalImageImportService;
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

    /** 将外接搜图的远程图片保存到本地存储，供新增物品和抠图预览复用。 */
    @PostMapping("/import-remote-image")
    public ApiResponse<Map<String, String>> importRemoteImage(@RequestBody Map<String, String> request) {
        String sourceUrl = request == null ? null : request.get("sourceUrl");
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "图片地址不能为空");
        }
        String objectKey = fileService.upload(externalImageImportService.download(sourceUrl));
        String publicUrl = storagePathHelper.toFullUrl(objectKey);
        return ApiResponse.success(Map.of(
                "objectKey", objectKey,
                "url", publicUrl != null ? publicUrl : ""
        ));
    }

    @PostMapping("/remove-bg")
    public ApiResponse<RemoveBgResponse> removeBackground(@RequestBody @Valid RemoveBgRequest request) {
        if (request.getAssetId() == null) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "资产编号不能为空");
        }
        String objectKey = resolveObjectKey(request);
        CoverApplyResponse response = assetCoverService.removeBackground(request.getAssetId(), objectKey);
        return ApiResponse.success(new RemoveBgResponse(response.attachmentId(), response.url()));
    }

    /**
     * 新增或编辑页使用的临时抠图预览。图片不落库，只有用户确认后前端才上传为新封面。
     */
    @PostMapping("/remove-bg-preview-binary")
    public ResponseEntity<byte[]> previewRemoveBackgroundBinary(@RequestBody @Valid RemoveBgRequest request) {
        String objectKey = resolveObjectKey(request);
        byte[] origin = fileService.download(objectKey);
        byte[] result = backgroundRemovalService.removeBackground(request.getAssetId(), origin, "no-bg-preview.png");
        return ResponseEntity.ok().contentType(resolveImageMediaType(result)).body(result);
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
                throw new BizException(ErrorCode.VALIDATION_ERROR, "当前封面不是本地上传图片，请先上传图片后再抠图");
            }
            return objectKey;
        }
        throw new BizException(ErrorCode.VALIDATION_ERROR, "请提供封面附件或封面地址");
    }

    private String buildExtra(String type, String objectKey) {
        return assetCoverService.toExtra(Map.of("type", type, "objectKey", objectKey));
    }

    private MediaType resolveImageMediaType(byte[] content) {
        if (content == null || content.length < 12) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "抠图服务未返回有效图片");
        }
        if ((content[0] & 0xFF) == 0x89 && content[1] == 'P' && content[2] == 'N' && content[3] == 'G') {
            return MediaType.IMAGE_PNG;
        }
        if ((content[0] & 0xFF) == 0xFF && (content[1] & 0xFF) == 0xD8 && (content[2] & 0xFF) == 0xFF) {
            return MediaType.IMAGE_JPEG;
        }
        if (content[0] == 'R' && content[1] == 'I' && content[2] == 'F' && content[3] == 'F'
                && content[8] == 'W' && content[9] == 'E' && content[10] == 'B' && content[11] == 'P') {
            return MediaType.parseMediaType("image/webp");
        }
        throw new BizException(ErrorCode.INTERNAL_ERROR, "抠图服务未返回有效图片");
    }

}
