package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.service.FileService;
import com.digiledger.backend.util.StoragePathHelper;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 文件上传接口。
 */
@RestController
@RequestMapping("/api/files")
public class FileController {

    private final FileService fileService;
    private final StoragePathHelper storagePathHelper;

    public FileController(FileService fileService, StoragePathHelper storagePathHelper) {
        this.fileService = fileService;
        this.storagePathHelper = storagePathHelper;
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
}
