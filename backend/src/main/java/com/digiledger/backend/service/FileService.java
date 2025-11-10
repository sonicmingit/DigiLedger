package com.digiledger.backend.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传服务。
 */
public interface FileService {

    /**
     * 上传文件并返回可访问 URL。
     *
     * @param file 上传的文件
     * @return 可访问的公网地址
     */
    String upload(MultipartFile file);
}
