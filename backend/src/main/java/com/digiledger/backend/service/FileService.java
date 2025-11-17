package com.digiledger.backend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务抽象
 */
public interface FileService {

    /**
     * 上传文件并返回存储路径 key
     */
    String upload(MultipartFile file);

    /**
     * 删除已有的存储文件
     */
    void delete(String objectKey);

    /**
     * 读取已经上传的文件
     */
    byte[] download(String objectKey);
}
