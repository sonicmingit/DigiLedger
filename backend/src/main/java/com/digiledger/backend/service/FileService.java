package com.digiledger.backend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务。
 */
public interface FileService {

    /**
     * 上传文件并返回对象存储中的 objectKey。
     *
     * @param file 上传的文件
     * @return 对象在存储中的 objectKey
     */
    String upload(MultipartFile file);
}
