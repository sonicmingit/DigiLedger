package com.digiledger.backend.mapper;

import com.digiledger.backend.model.entity.FileAttachment;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 附件 Mapper，负责数据库访问。
 */
@Mapper
public interface FileAttachmentMapper {

    int insert(FileAttachment attachment);

    FileAttachment findById(@Param("id") Long id);

    List<FileAttachment> findByBiz(@Param("bizType") String bizType, @Param("bizId") Long bizId);

    int softDelete(@Param("id") Long id);

    int updateBizInfo(@Param("id") Long id,
                      @Param("bizType") String bizType,
                      @Param("bizId") Long bizId);
}
