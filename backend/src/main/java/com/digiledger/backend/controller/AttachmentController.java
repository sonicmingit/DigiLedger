package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.model.dto.attachment.AttachmentResponse;
import com.digiledger.backend.service.AttachmentService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 附件统一管理接口。
 */
@RestController
@RequestMapping("/api/attachments")
public class AttachmentController {

    private final AttachmentService attachmentService;

    public AttachmentController(AttachmentService attachmentService) {
        this.attachmentService = attachmentService;
    }

    @PostMapping("/upload")
    public ApiResponse<AttachmentResponse> upload(@RequestPart("file") MultipartFile file,
                                                  @RequestParam(value = "biz_type", required = false) String bizType,
                                                  @RequestParam(value = "biz_id", required = false) Long bizId,
                                                  @RequestParam(value = "extra", required = false) String extra) {
        return ApiResponse.success(attachmentService.upload(file, bizType, bizId, extra));
    }

    @GetMapping
    public ApiResponse<List<AttachmentResponse>> list(@RequestParam("biz_type") String bizType,
                                                      @RequestParam("biz_id") Long bizId) {
        return ApiResponse.success(attachmentService.listByBiz(bizType, bizId));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        attachmentService.delete(id);
        return ApiResponse.success();
    }
}
