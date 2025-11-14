package com.digiledger.backend.service;

import com.digiledger.backend.model.dto.attachment.AttachmentResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 附件统一管理服务。
 */
public interface AttachmentService {

    /**
     * 上传附件并入库。
     *
     * @param file 上传文件
     * @param bizType 业务类型，可为空
     * @param bizId 业务主键，可为空
     * @param extra 附加 JSON 字符串
     * @return 附件响应
     */
    AttachmentResponse upload(MultipartFile file, String bizType, Long bizId, String extra);

    /**
     * 根据业务维度查询附件列表。
     *
     * @param bizType 业务类型
     * @param bizId 业务主键
     * @return 附件列表
     */
    List<AttachmentResponse> listByBiz(String bizType, Long bizId);

    /**
     * 删除附件，同时清理对象存储。
     *
     * @param id 附件主键
     */
    void delete(Long id);
}
