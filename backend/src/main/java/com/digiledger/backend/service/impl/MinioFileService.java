package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.config.StorageProperties;
import com.digiledger.backend.config.UploadProperties;
import com.digiledger.backend.service.FileService;
import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.UUID;

/**
 * 基于 MinIO 的上传实现。
 */
@Service
public class MinioFileService implements FileService {

    private final MinioClient minioClient;
    private final StorageProperties storageProperties;
    private final UploadProperties uploadProperties;

    public MinioFileService(MinioClient minioClient,
                             StorageProperties storageProperties,
                             UploadProperties uploadProperties) {
        this.minioClient = minioClient;
        this.storageProperties = storageProperties;
        this.uploadProperties = uploadProperties;
    }

    @Override
    public String upload(MultipartFile file) {
        validateFile(file);
        String objectName = buildObjectName(file.getOriginalFilename());
        try (InputStream inputStream = file.getInputStream()) {
            ensureBucket();
            minioClient.putObject(PutObjectArgs.builder()
                    .bucket(storageProperties.getBucket())
                    .object(objectName)
                    .contentType(file.getContentType())
                    .stream(inputStream, file.getSize(), -1)
                    .build());
        } catch (Exception e) {
            throw new BizException(ErrorCode.INTERNAL_ERROR, "上传失败: " + e.getMessage());
        }
        return buildFileUrl(objectName);
    }

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "文件不能为空");
        }
        long maxBytes = uploadProperties.getMaxSizeMb() * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BizException(ErrorCode.FILE_TOO_LARGE, "文件超过" + uploadProperties.getMaxSizeMb() + "MB 限制");
        }
        if (uploadProperties.getAllowedContentTypes() != null
                && !uploadProperties.getAllowedContentTypes().isEmpty()
                && !uploadProperties.getAllowedContentTypes().contains(file.getContentType())) {
            throw new BizException(ErrorCode.UNSUPPORTED_MEDIA_TYPE, "仅支持上传图片类型：" + uploadProperties.getAllowedContentTypes());
        }
    }

    private void ensureBucket() throws Exception {
        BucketExistsArgs existsArgs = BucketExistsArgs.builder()
                .bucket(storageProperties.getBucket())
                .build();
        if (!minioClient.bucketExists(existsArgs)) {
            MakeBucketArgs.Builder builder = MakeBucketArgs.builder()
                    .bucket(storageProperties.getBucket());
            if (storageProperties.getRegion() != null && !storageProperties.getRegion().isBlank()) {
                builder.region(storageProperties.getRegion());
            }
            minioClient.makeBucket(builder.build());
        }
    }

    private String buildObjectName(String originalFilename) {
        String suffix = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            suffix = originalFilename.substring(originalFilename.lastIndexOf('.'));
        }
        return "uploads/" + LocalDate.now() + "/" + UUID.randomUUID() + suffix;
    }

    private String buildFileUrl(String objectName) {
        String baseUrl = storageProperties.getBaseUrl();
        if (baseUrl == null || baseUrl.isBlank()) {
            baseUrl = storageProperties.getEndpoint();
        }
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/";
        }
        return baseUrl + storageProperties.getBucket() + "/" + objectName;
    }
}
