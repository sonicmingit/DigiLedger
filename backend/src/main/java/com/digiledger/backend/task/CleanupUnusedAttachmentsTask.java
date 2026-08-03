package com.digiledger.backend.task;

import com.digiledger.backend.config.StorageProperties;
import com.digiledger.backend.mapper.FileAttachmentMapper;
import com.digiledger.backend.model.entity.FileAttachment;
import com.digiledger.backend.model.dto.attachment.UnusedAttachmentResponse;
import com.digiledger.backend.util.StoragePathHelper;
import io.minio.*;
import io.minio.messages.Item;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 定期清理未使用的附件文件任务
 */
@Component
public class CleanupUnusedAttachmentsTask {

    private static final Logger logger = LoggerFactory.getLogger(CleanupUnusedAttachmentsTask.class);

    private final FileAttachmentMapper fileAttachmentMapper;
    private final MinioClient minioClient;
    private final StorageProperties storageProperties;
    private final StoragePathHelper storagePathHelper;

    public CleanupUnusedAttachmentsTask(FileAttachmentMapper fileAttachmentMapper,
                                       MinioClient minioClient,
                                       StorageProperties storageProperties,
                                       StoragePathHelper storagePathHelper) {
        this.fileAttachmentMapper = fileAttachmentMapper;
        this.minioClient = minioClient;
        this.storageProperties = storageProperties;
        this.storagePathHelper = storagePathHelper;
    }

    /**
     * todo 定时删除
     * 每天凌晨2点执行清理任务
     * 可以通过调整cron表达式来改变执行频率
     */
    //@Scheduled(cron = "0 0 2 * * ?")
    public void cleanupUnusedAttachments() {
        String bucketName = storageProperties.getBucket();
        if (!StringUtils.hasText(bucketName)) {
            logger.warn("存储桶名称未配置，跳过清理任务");
            return;
        }

        logger.info("开始执行未使用附件文件清理任务");

        try {
            ensureBucket();
            logger.debug("MinIO连接可用，已验证存储桶：{}", bucketName);
        } catch (Exception connectionException) {
            logger.error("MinIO连接测试失败，请检查配置和网络连接", connectionException);
            return;
        }

        try {
            // 获取所有未被软删除的附件记录
            Set<String> activeObjectKeys = getActiveObjectKeys();
            logger.info("系统中共有 {} 个活跃附件", activeObjectKeys.size());

            // 遍历MinIO中的所有对象
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .recursive(true)
                    .build()
            );

            int cleanedCount = 0;
            for (Result<Item> result : results) {
                try {
                    Item item = result.get();
                    String objectKey = item.objectName();

                    // 如果该对象不在活跃附件列表中，则删除它
                    if (!activeObjectKeys.contains(objectKey)) {
                        try {
                            minioClient.removeObject(
                                RemoveObjectArgs.builder()
                                    .bucket(bucketName)
                                    .object(objectKey)
                                    .build()
                            );
                            logger.info("已删除未使用的文件: {}", objectKey);
                            cleanedCount++;
                        } catch (Exception removeException) {
                            logger.error("删除MinIO对象失败，对象键: {}", objectKey, removeException);
                        }
                    }
                } catch (Exception e) {
                    logger.error("处理MinIO对象时发生错误", e);
                }
            }

            logger.info("清理任务完成，共删除 {} 个未使用的文件", cleanedCount);
        } catch (Exception e) {
            logger.error("执行清理任务时发生错误", e);
        }
    }

    /**
     * 获取将要被清理的未使用文件列表（不实际执行删除）
     * @return 将要被清理的文件列表，包含对象键与完整访问URL
     */
    public List<UnusedAttachmentResponse> getUnusedAttachments() {
        List<UnusedAttachmentResponse> unusedFiles = new ArrayList<>();
        String bucketName = storageProperties.getBucket();

        if (!StringUtils.hasText(bucketName)) {
            logger.warn("存储桶名称未配置，无法获取未使用附件列表");
            return unusedFiles;
        }

        try {
            ensureBucket();
            logger.debug("MinIO连接可用，已验证存储桶：{}", bucketName);
        } catch (Exception connectionException) {
            logger.error("MinIO连接测试失败，请检查配置和网络连接", connectionException);
            return unusedFiles;
        }

        try {
            // 获取所有未被软删除的附件记录
            Set<String> activeObjectKeys = getActiveObjectKeys();
            logger.info("系统中共有 {} 个活跃附件", activeObjectKeys.size());

            // 遍历MinIO中的所有对象
            Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                    .bucket(bucketName)
                    .recursive(true)
                    .build()
            );

            for (Result<Item> result : results) {
                try {
                    Item item = result.get();
                    String objectKey = item.objectName();

                    // 如果该对象不在活跃附件列表中，则加入待清理列表
                    if (!activeObjectKeys.contains(objectKey)) {
                        String fullUrl = storagePathHelper.toBrowserUrl(objectKey);
                        unusedFiles.add(new UnusedAttachmentResponse(
                            objectKey,
                            fullUrl
                        ));
                    }
                } catch (Exception e) {
                    logger.error("处理MinIO对象时发生错误", e);
                }
            }
        } catch (Exception e) {
            logger.error("获取未使用附件列表时发生错误: {}", e.getMessage());
            // 不抛出异常，而是返回空列表
        }
        
        return unusedFiles;
    }

    /**
     * 根据前端选择清理指定的未使用文件
     * @param objectKeys 待清理的对象键列表
     */
    public void cleanupUnusedAttachments(List<String> objectKeys) {
        if (objectKeys == null || objectKeys.isEmpty()) {
            cleanupUnusedAttachments();
            return;
        }

        String bucketName = storageProperties.getBucket();
        if (!StringUtils.hasText(bucketName)) {
            logger.warn("存储桶名称未配置，跳过清理任务");
            return;
        }

        try {
            ensureBucket();
            logger.debug("MinIO连接可用，已验证存储桶：{}", bucketName);
        } catch (Exception connectionException) {
            logger.error("MinIO连接测试失败，请检查配置和网络连接", connectionException);
            return;
        }

        try {
            Set<String> activeObjectKeys = getActiveObjectKeys();
            int cleanedCount = 0;
            for (String objectKey : objectKeys) {
                if (activeObjectKeys.contains(objectKey)) {
                    logger.warn("跳过仍被引用的文件，不予删除：{}", objectKey);
                    continue;
                }
                try {
                    minioClient.removeObject(
                        RemoveObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectKey)
                            .build()
                    );
                    cleanedCount++;
                    logger.info("已按选择删除未使用的文件: {}", objectKey);
                } catch (Exception removeException) {
                    logger.error("删除MinIO对象失败，对象键: {}", objectKey, removeException);
                }
            }
            logger.info("选中清理任务完成，共删除 {} 个未使用的文件", cleanedCount);
        } catch (Exception e) {
            logger.error("执行选中清理任务时发生错误", e);
        }
    }

    /**
     * 获取所有未被软删除的附件记录
     */
    private List<FileAttachment> getAllActiveAttachments() {
        return fileAttachmentMapper.findAllActive();
    }

    private Set<String> getActiveObjectKeys() {
        List<FileAttachment> activeAttachments = getAllActiveAttachments();
        Set<String> activeObjectKeys = new HashSet<>();

        for (FileAttachment attachment : activeAttachments) {
            activeObjectKeys.add(attachment.getObjectKey());
        }
        return activeObjectKeys;
    }

    private void ensureBucket() throws Exception {
        BucketExistsArgs existsArgs = BucketExistsArgs.builder()
            .bucket(storageProperties.getBucket())
            .build();
        if (!minioClient.bucketExists(existsArgs)) {
            MakeBucketArgs.Builder builder = MakeBucketArgs.builder()
                .bucket(storageProperties.getBucket());
            if (StringUtils.hasText(storageProperties.getRegion())) {
                builder.region(storageProperties.getRegion());
            }
            minioClient.makeBucket(builder.build());
        }
    }
}
