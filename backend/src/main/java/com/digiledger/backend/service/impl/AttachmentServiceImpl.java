package com.digiledger.backend.service.impl;

import com.digiledger.backend.common.BizException;
import com.digiledger.backend.common.ErrorCode;
import com.digiledger.backend.mapper.FileAttachmentMapper;
import com.digiledger.backend.model.dto.attachment.AttachmentResponse;
import com.digiledger.backend.model.entity.FileAttachment;
import com.digiledger.backend.service.AttachmentService;
import com.digiledger.backend.service.FileService;
import com.digiledger.backend.util.StoragePathHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 附件统一管理服务实现。
 */
@Service
public class AttachmentServiceImpl implements AttachmentService {

    private static final Logger log = LoggerFactory.getLogger(AttachmentServiceImpl.class);

    private final FileAttachmentMapper fileAttachmentMapper;
    private final FileService fileService;
    private final StoragePathHelper storagePathHelper;

    public AttachmentServiceImpl(FileAttachmentMapper fileAttachmentMapper,
                                 FileService fileService,
                                 StoragePathHelper storagePathHelper) {
        this.fileAttachmentMapper = fileAttachmentMapper;
        this.fileService = fileService;
        this.storagePathHelper = storagePathHelper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AttachmentResponse upload(MultipartFile file, String bizType, Long bizId, String extra) {
        String objectKey = fileService.upload(file);
        FileAttachment attachment = new FileAttachment();
        attachment.setBizType(normalizeBizType(bizType));
        attachment.setBizId(bizId);
        attachment.setObjectKey(objectKey);
        attachment.setFileName(file.getOriginalFilename());
        attachment.setFileType(file.getContentType());
        attachment.setFileSize(file.getSize());
        attachment.setExtra(extra);
        attachment.setDeleted(Boolean.FALSE);
        LocalDateTime now = LocalDateTime.now();
        attachment.setCreatedAt(now);
        attachment.setUpdatedAt(now);
        fileAttachmentMapper.insert(attachment);
        return toResponse(attachment);
    }

    @Override
    public List<AttachmentResponse> listByBiz(String bizType, Long bizId) {
        if (!StringUtils.hasText(bizType) || Objects.isNull(bizId)) {
            throw new BizException(ErrorCode.VALIDATION_ERROR, "biz_type 与 biz_id 不能为空");
        }
        List<FileAttachment> attachments = fileAttachmentMapper.findByBiz(normalizeBizType(bizType), bizId);
        return attachments.stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        FileAttachment attachment = fileAttachmentMapper.findById(id);
        if (attachment == null || Boolean.TRUE.equals(attachment.getDeleted())) {
            throw new BizException(ErrorCode.ATTACHMENT_NOT_FOUND, "附件不存在或已删除");
        }
        try {
            fileService.delete(attachment.getObjectKey());
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("删除对象存储文件失败，objectKey={}，原因={}", attachment.getObjectKey(), ex.getMessage());
            throw new BizException(ErrorCode.INTERNAL_ERROR, "删除附件失败: " + ex.getMessage());
        }
        int rows = fileAttachmentMapper.softDelete(id);
        if (rows == 0) {
            throw new BizException(ErrorCode.ATTACHMENT_NOT_FOUND, "附件不存在或已删除");
        }
    }

    private String normalizeBizType(String bizType) {
        if (!StringUtils.hasText(bizType)) {
            return null;
        }
        return bizType.trim().toUpperCase();
    }

    private AttachmentResponse toResponse(FileAttachment attachment) {
        return new AttachmentResponse(
                attachment.getId(),
                attachment.getBizType(),
                attachment.getBizId(),
                attachment.getObjectKey(),
                storagePathHelper.toFullUrl(attachment.getObjectKey()),
                attachment.getFileName(),
                attachment.getFileType(),
                attachment.getFileSize(),
                attachment.getExtra(),
                attachment.getCreatedAt()
        );
    }
}
