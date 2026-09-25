package com.zyy.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.zyy.common.BaseController;
import com.zyy.common.Result;
import com.zyy.util.MinioUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

/**
 * File management REST controller.
 * <p>
 * Provides centralized file upload/download operations via MinIO object storage.
 * Supports single and batch upload, type validation, and automatic cleanup.
 * <p>
 * Storage strategy:
 * <ul>
 *   <li>Images: jpg, jpeg, png, gif, bmp, webp (max 10MB)</li>
 *   <li>Documents: pdf, doc, docx, xls, xlsx, ppt, pptx, txt (max 10MB)</li>
 * </ul>
 *
 * @author System Architect
 * @version 1.0.0
 */
@Slf4j
@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
@Tag(name = "File Management", description = "MinIO object storage operations for file upload/download")
@SecurityRequirement(name = "BearerAuth")
public class FileController {

    private final MinioUtil minioUtil;

    /**
     * Permitted image file extensions for upload validation.
     */
    private static final List<String> ALLOWED_IMAGE_TYPES = Arrays.asList(
        "jpg", "jpeg", "png", "gif", "bmp", "webp"
    );

    /**
     * Permitted document file extensions for upload validation.
     */
    private static final List<String> ALLOWED_DOC_TYPES = Arrays.asList(
        "pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt"
    );

    /**
     * Maximum permitted file size (10 MB).
     */
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    /**
     * Upload a single file to MinIO storage.
     *
     * @param file Multipart file attachment
     * @return Result containing the file URL and metadata
     */
    @PostMapping("/upload")
    @PreAuthorize("@ss.hasAuthority('file:upload')")
    @Operation(
        summary = "Upload file",
        description = "Upload a single file to MinIO. Returns the accessible URL.",
        responses = {
            @ApiResponse(
                responseCode = "200",
                description = "Upload successful",
                content = @Content(schema = @Schema(example = """
                    {
                      "code": 200,
                      "message": "Upload successful",
                      "data": {
                        "url": "http://localhost:9000/generic-sys-admin/f7c3a8b1.jpg",
                        "filename": "f7c3a8b1.jpg",
                        "originalName": "avatar.jpg",
                        "size": 102400,
                        "contentType": "image/jpeg"
                      }
                    }
                    """))
            )
        }
    )
    public Result<?> upload(
        @Parameter(description = "File to upload", required = true)
        @RequestParam("file") MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            return Result.fail(400, "Empty file not allowed");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return Result.fail(400, "File size exceeds 10MB limit: " + formatFileSize(file.getSize()));
        }

        String originalName = file.getOriginalFilename();
        String ext = getFileExtension(originalName).toLowerCase();

        if (!isAllowedType(ext)) {
            return Result.fail(400, "File type not allowed: ." + ext + " . Allowed: " + getAllowedExtensions());
        }

        log.info("[FileController] Upload | filename: {} | size: {}",
            originalName, formatFileSize(file.getSize()));

        try {
            String fileUrl = minioUtil.uploadFile(file);

            JSONObject result = JSONUtil.createObj()
                .set("url", fileUrl)
                .set("filename", extractFilename(fileUrl))
                .set("originalName", originalName)
                .set("size", file.getSize())
                .set("contentType", file.getContentType());

            log.info("[FileController] Upload success | url: {}", fileUrl);
            triggerSuccessBroadcast(originalName);
            return Result.ok(result, "Upload successful");

        } catch (Exception e) {
            log.error("[FileController] Upload failed | error: {}", e.getMessage());
            return Result.fail(500, "Upload failed: " + e.getMessage());
        }
    }

    /**
     * Upload multiple files in a single request.
     *
     * @param files Array of multipart files (max 20)
     * @return Result containing success/failure counts and file metadata
     */
    @PostMapping("/upload/batch")
    @PreAuthorize("@ss.hasAuthority('file:upload')")
    @Operation(
        summary = "Batch upload files",
        description = "Upload multiple files in one request. Maximum 20 files per batch."
    )
    public Result<?> uploadBatch(
        @Parameter(description = "Files to upload (max 20)", required = true)
        @RequestParam("files") MultipartFile[] files
    ) {
        if (files == null || files.length == 0) {
            return Result.fail(400, "No files provided");
        }

        if (files.length > 20) {
            return Result.fail(400, "Batch size cannot exceed 20 files");
        }

        log.info("[FileController] Batch upload | count: {}", files.length);

        JSONObject result = JSONUtil.createObj();
        int successCount = 0;
        int failCount = 0;

        for (int i = 0; i < files.length; i++) {
            MultipartFile file = files[i];
            try {
                if (file != null && !file.isEmpty()) {
                    String ext = getFileExtension(file.getOriginalFilename()).toLowerCase();
                    if (!isAllowedType(ext) || file.getSize() > MAX_FILE_SIZE) {
                        failCount++;
                        continue;
                    }
                    String fileUrl = minioUtil.uploadFile(file);
                    result.set("file_" + i, JSONUtil.createObj()
                        .set("url", fileUrl)
                        .set("filename", extractFilename(fileUrl))
                        .set("originalName", file.getOriginalFilename())
                        .set("size", file.getSize()));
                    successCount++;
                }
            } catch (Exception e) {
                failCount++;
                log.warn("[FileController] Batch item failed | index: {} | error: {}", i, e.getMessage());
            }
        }

        result.set("successCount", successCount);
        result.set("failCount", failCount);

        String msg = String.format("Batch complete: %d succeeded, %d failed", successCount, failCount);
        log.info("[FileController] {}", msg);

        return Result.ok(result, msg);
    }

    /**
     * Delete a file from MinIO storage by URL.
     *
     * @param fileUrl Full URL of the file to delete
     * @return Operation result
     */
    @DeleteMapping
    @PreAuthorize("@ss.hasAuthority('file:del')")
    @Operation(
        summary = "Delete file",
        description = "Delete a file from MinIO storage by its URL"
    )
    public Result<?> delete(
        @Parameter(description = "Full URL of the file to delete", required = true)
        @RequestParam("url") String fileUrl
    ) {
        if (fileUrl == null || fileUrl.trim().isEmpty()) {
            return Result.fail(400, "File URL cannot be empty");
        }

        try {
            minioUtil.deleteFile(fileUrl);
            log.info("[FileController] Delete success | url: {}", fileUrl);
            return Result.ok(null, "File deleted");
        } catch (SecurityException se) {
            return Result.fail(403, "Delete not permitted: bucket access denied");
        } catch (Exception e) {
            log.error("[FileController] Delete failed | error: {}", e.getMessage());
            return Result.fail(500, "Delete failed: " + e.getMessage());
        }
    }

    /**
     * Batch delete multiple files.
     *
     * @param body JSON body containing array of file URLs
     * @return Operation result with success/failure counts
     */
    @DeleteMapping("/batch")
    @PreAuthorize("@ss.hasAuthority('file:del')")
    @Operation(
        summary = "Batch delete files",
        description = "Delete multiple files from MinIO storage"
    )
    public Result<?> deleteBatch(@RequestBody JSONObject body) {
        var urls = body.getJSONArray("urls");
        if (urls == null || urls.isEmpty()) {
            return Result.fail(400, "File URL list is empty or malformed");
        }

        int successCount = 0;
        int failCount = 0;

        for (Object url : urls) {
            try {
                minioUtil.deleteFile(url.toString());
                successCount++;
            } catch (Exception e) {
                failCount++;
                log.warn("[FileController] Batch delete failed | url: {} | error: {}", url, e.getMessage());
            }
        }

        String msg = String.format("Batch delete complete: %d succeeded, %d failed", successCount, failCount);
        log.info("[FileController] {}", msg);

        return failCount == 0 ? Result.ok(null, msg) : Result.fail(msg);
    }

    // ==================== Private Helper Methods ====================

    private boolean isAllowedType(String ext) {
        return ALLOWED_IMAGE_TYPES.contains(ext) || ALLOWED_DOC_TYPES.contains(ext);
    }

    private String getAllowedExtensions() {
        return "." + String.join(", .", ALLOWED_IMAGE_TYPES) + ", ." + String.join(", .", ALLOWED_DOC_TYPES);
    }

    private String extractFilename(String url) {
        if (url == null || url.isEmpty()) return "";
        int lastSlash = url.lastIndexOf('/');
        return lastSlash == -1 ? url : url.substring(lastSlash + 1);
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }

    /**
     * Trigger TTS broadcast on successful upload.
     * Placeholder for event-driven architecture integration.
     */
    private void triggerSuccessBroadcast(String filename) {
        try {
            log.debug("[FileController] TTS broadcast placeholder for: {}", filename);
        } catch (Exception e) {
            log.warn("[FileController] TTS broadcast skipped: {}", e.getMessage());
        }
    }
}
