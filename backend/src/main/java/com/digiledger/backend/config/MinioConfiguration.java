package com.digiledger.backend.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 客户端配置。
 */
@Configuration
public class MinioConfiguration {

    /**
     * 构建 MinIO 客户端，供上传服务使用。
     */
    @Bean
    public MinioClient minioClient(StorageProperties storageProperties) {
        MinioClient.Builder builder = MinioClient.builder()
                .endpoint(storageProperties.getEndpoint())
                .credentials(storageProperties.getAccessKey(), storageProperties.getSecretKey());
        if (storageProperties.getRegion() != null && !storageProperties.getRegion().isBlank()) {
            builder.region(storageProperties.getRegion());
        }
        return builder.build();
    }
}
