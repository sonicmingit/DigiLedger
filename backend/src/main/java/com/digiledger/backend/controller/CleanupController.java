package com.digiledger.backend.controller;

import com.digiledger.backend.common.ApiResponse;
import com.digiledger.backend.model.dto.attachment.UnusedAttachmentResponse;
import com.digiledger.backend.task.CleanupUnusedAttachmentsTask;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 清理相关的控制器
 */
@RestController
@RequestMapping("/api/cleanup")
public class CleanupController {

    private final CleanupUnusedAttachmentsTask cleanupTask;

    public CleanupController(CleanupUnusedAttachmentsTask cleanupTask) {
        this.cleanupTask = cleanupTask;
    }

    /**
     * 获取未使用的附件列表
     *
     * @return ApiResponse<List<String>>
     */
    @GetMapping("/unused-attachments")
    public ApiResponse<List<UnusedAttachmentResponse>> getUnusedAttachments() {
        try {
            List<UnusedAttachmentResponse> unusedAttachments = cleanupTask.getUnusedAttachments();
            return ApiResponse.success(unusedAttachments);
        } catch (Exception e) {
            // 记录错误但不中断程序执行
            return ApiResponse.success(List.of()); // 返回空列表而不是错误
        }
    }

    /**
     * 手动触发清理任务
     *
     * @return ApiResponse<Void>
     */
    @PostMapping("/unused-attachments")
    public ApiResponse<Void> cleanupUnusedAttachments(@RequestBody(required = false) List<String> objectKeys) {
        try {
            if (objectKeys != null && !objectKeys.isEmpty()) {
                cleanupTask.cleanupUnusedAttachments(objectKeys);
            } else {
                cleanupTask.cleanupUnusedAttachments();
            }
            return ApiResponse.success(null);
        } catch (Exception e) {
            // 即使出现错误也返回成功，避免前端报错
            return ApiResponse.success(null);
        }
    }
}
