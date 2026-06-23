package com.zyy.file.util;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.core.util.URLUtil;
import com.zyy.file.config.MinioAvailability;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.StatObjectArgs;
import java.io.ByteArrayInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.TimeUnit;

/**
 * ==========================================================
 * MinIO File Storage Utility
 * Provides unified file storage operations with MinIO/local fallback
 * ==========================================================
 *
 * Storage Strategy:
 * 1. Spring auto-injects MinioClient from MinioConfig
 * 2. If minioClient != null && availability.available = true, use MinIO
 * 3. If MinIO unavailable, fallback to LocalFileStorageStrategy
 *
 * Usage:
 * ```java
 * @Autowired
 * private MinioUtil minioUtil;
 *
 * // Upload file
 * String url = minioUtil.uploadFile(file);  // Auto-detect storage
 * minioUtil.deleteFile(url);                 // Delete file
 * ```
 *
 * @author System Architect
 */
@Component
public class MinioUtil {

    private static final Logger log = LoggerFactory.getLogger(MinioUtil.class);

    // ==================== Dependencies ====================

    @Autowired(required = false)
    private MinioClient minioClient;

    @Autowired
    private MinioAvailability availability;

    @Autowired(required = false)
    private com.zyy.file.config.LocalFileStorageStrategy localStorage;

    // ==================== Configuration ====================

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.bucket-name}")
    private String bucketName;

    @Value("${minio.public-url:#{null}}")
    private String publicUrl;

    @Value("${storage.provider:auto}")
    private String storageProvider;  // auto / minio / local

    @Value("${storage.local.base-dir:#{systemProperties['user.home']}}")
    private String localBaseDir;

    @Value("${storage.local.sub-dir:.generic-sys-admin/uploads}")
    private String localSubDir;

    // ==================== Public API ====================

    /**
     * Upload file with auto-detected storage mode.
     *
     * Storage selection:
     * 1. storageProvider = minio: Force use MinIO
     * 2. storageProvider = local: Force use local storage
     * 3. storageProvider = auto: Check availability
     */
    public String uploadFile(MultipartFile file) {
        return uploadFile(file, null);
    }

    public String uploadFile(MultipartFile file, String originalFilename) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("[MinIO] File cannot be null or empty");
        }

        String storageMode = resolveStorageMode();

        if ("local".equals(storageMode)) {
            log.debug("[MinIO] Using local storage for file upload");
            return uploadLocally(file, originalFilename);
        } else {
            log.debug("[MinIO] Using MinIO for file upload");
            return uploadToMinio(file, originalFilename);
        }
    }

    public String uploadBytes(byte[] data, String filename, String contentType) {
        String storageMode = resolveStorageMode();

        if ("local".equals(storageMode)) {
            return uploadBytesLocally(data, filename);
        } else {
            return uploadBytesToMinio(data, filename, contentType);
        }
    }

    /**
     * Delete file by URL.
     *
     * URL patterns:
     * - /local-files/: Local storage file
     * - minio/bucketName: MinIO storage file
     */
    public void deleteFile(String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) {
            log.warn("[MinIO] File URL is blank, skipping delete");
            return;
        }

        if (fileUrl.contains("/local-files/")) {
            deleteLocalFile(fileUrl);
        } else {
            deleteMinioFile(fileUrl);
        }
    }

    // ==================== URL Building ====================

    public String buildFileUrl(String objectName) {
        if (StrUtil.isNotBlank(publicUrl)) {
            String base = publicUrl.endsWith("/") ? publicUrl.substring(0, publicUrl.length() - 1) : publicUrl;
            return base + "/" + objectName;
        } else {
            String base = (endpoint != null && !endpoint.isBlank()) ? endpoint : "http://localhost:9000";
            return base + "/" + bucketName + "/" + objectName;
        }
    }

    public String getSignedUrl(String objectName, long expire, TimeUnit unit) {
        if (minioClient == null || !availability.isAvailable()) {
            log.warn("[MinIO] MinIO unavailable, cannot generate signed URL");
            return null;
        }

        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .expiry((int) unit.toSeconds(expire), unit)
                    .build()
            );
        } catch (Exception e) {
            log.error("[MinIO] Failed to generate signed URL: {}", e.getMessage());
            return null;
        }
    }

    public boolean exists(String objectName) {
        if (minioClient == null || !availability.isAvailable()) {
            return existsLocally(objectName);
        }

        try {
            minioClient.statObject(
                StatObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== Storage Mode Resolution ====================

    private String resolveStorageMode() {
        if ("minio".equalsIgnoreCase(storageProvider)) {
            return "minio";
        }
        if ("local".equalsIgnoreCase(storageProvider)) {
            return "local";
        }
        // auto mode: check MinIO availability
        return (minioClient != null && availability.isAvailable()) ? "minio" : "local";
    }

    // ==================== MinIO Operations ====================

    private String uploadToMinio(MultipartFile file, String originalFilename) {
        if (minioClient == null) {
            throw new IllegalStateException("[MinIO] MinIO Client is null, check MinioConfig");
        }

        String sourceName = file.getOriginalFilename();
        String ext = getFileExtension(sourceName);
        String finalFileName = cn.hutool.core.util.IdUtil.fastSimpleUUID() + ext;

        try {
            String contentType = file.getContentType();
            if (StrUtil.isBlank(contentType) || "application/octet-stream".equals(contentType)) {
                contentType = guessContentType(ext);
            }

            ByteArrayInputStream bais = new ByteArrayInputStream(file.getBytes());

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(finalFileName)
                    .stream(bais, file.getSize(), -1)
                    .contentType(contentType)
                    .build()
            );

            IoUtil.close(bais);

            String fileUrl = buildFileUrl(finalFileName);
            log.info("[MinIO] File uploaded to MinIO | URL: {}", fileUrl);

            return fileUrl;

        } catch (Exception e) {
            log.error("[MinIO] MinIO upload failed, falling back to local: {}", e.getMessage());
            return uploadLocally(file, originalFilename);
        }
    }

    private String uploadBytesToMinio(byte[] data, String filename, String contentType) {
        if (minioClient == null) {
            return uploadBytesLocally(data, filename);
        }

        try {
            ByteArrayInputStream bais = new ByteArrayInputStream(data);

            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(bucketName)
                    .object(filename)
                    .stream(bais, data.length, -1)
                    .contentType(contentType)
                    .build()
            );

            IoUtil.close(bais);

            String fileUrl = buildFileUrl(filename);
            log.info("[MinIO] Bytes uploaded to MinIO | Filename: {}", filename);

            return fileUrl;

        } catch (Exception e) {
            log.warn("[MinIO] MinIO bytes upload failed, falling back to local: {}", e.getMessage());
            return uploadBytesLocally(data, filename);
        }
    }

    private void deleteMinioFile(String fileUrl) {
        if (minioClient == null) {
            log.warn("[MinIO] MinIO client null, deleting local instead");
            deleteLocalFile(fileUrl);
            return;
        }

        try {
            String objectName = extractObjectName(fileUrl);
            if (objectName == null) return;

            String urlBucketName = extractBucketName(fileUrl);
            if (!bucketName.equals(urlBucketName)) {
                throw new SecurityException("Cannot delete file from different bucket");
            }

            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(bucketName)
                    .object(objectName)
                    .build()
            );

            log.info("[MinIO] File deleted from MinIO | Object: {}", objectName);

        } catch (SecurityException se) {
            throw se;
        } catch (Exception e) {
            log.error("[MinIO] Failed to delete MinIO file: {}", e.getMessage());
        }
    }

    // ==================== Local Storage Operations ====================

    private Path getLocalUploadPath() {
        return Paths.get(localBaseDir, localSubDir);
    }

    private String uploadLocally(MultipartFile file, String originalFilename) {
        String sourceName = file.getOriginalFilename();
        String ext = getFileExtension(sourceName);
        String finalFileName = cn.hutool.core.util.IdUtil.fastSimpleUUID() + ext;

        try {
            String yearMonth = cn.hutool.core.date.DateUtil.format(
                new java.util.Date(), "yyyy/MM");
            Path targetDir = getLocalUploadPath().resolve(yearMonth);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(finalFileName);
            Files.copy(file.getInputStream(), targetPath);

            String accessPath = "/local-files/" + yearMonth + "/" + finalFileName;
            log.info("[MinIO] File saved locally | Path: {}", targetPath);
            log.info("[MinIO] Access URL: {}", accessPath);

            return accessPath;

        } catch (Exception e) {
            log.error("[MinIO] Local upload failed: {}", e.getMessage());
            throw new RuntimeException("[MinIO] Local upload failed: " + e.getMessage(), e);
        }
    }

    private String uploadBytesLocally(byte[] data, String filename) {
        try {
            String yearMonth = cn.hutool.core.date.DateUtil.format(
                new java.util.Date(), "yyyy/MM");
            Path targetDir = getLocalUploadPath().resolve(yearMonth);
            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(filename);
            Files.write(targetPath, data);

            String accessPath = "/local-files/" + yearMonth + "/" + filename;
            log.info("[MinIO] Bytes saved locally | Path: {}", targetPath);

            return accessPath;

        } catch (Exception e) {
            throw new RuntimeException("[MinIO] Local bytes upload failed: " + e.getMessage(), e);
        }
    }

    private void deleteLocalFile(String fileUrl) {
        if (StrUtil.isBlank(fileUrl) || !fileUrl.contains("/local-files/")) {
            return;
        }

        try {
            String relativePath = fileUrl.replace("/local-files/", "");
            Path filePath = getLocalUploadPath().resolve(relativePath);

            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("[MinIO] Local file deleted | Path: {}", filePath);
            } else {
                log.warn("[MinIO] Local file not found: {}", filePath);
            }
        } catch (Exception e) {
            log.error("[MinIO] Failed to delete local file: {}", e.getMessage());
        }
    }

    private boolean existsLocally(String objectName) {
        try {
            Path filePath = getLocalUploadPath().resolve(objectName);
            return Files.exists(filePath);
        } catch (Exception e) {
            return false;
        }
    }

    // ==================== Utility Methods ====================

    private String extractObjectName(String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) return null;

        try {
            String decoded = URLUtil.decode(fileUrl, StandardCharsets.UTF_8);
            int bucketIndex = decoded.indexOf("/" + bucketName + "/");
            if (bucketIndex == -1) {
                int lastSlash = decoded.lastIndexOf('/');
                if (lastSlash > decoded.indexOf("://") + 3) {
                    String possibleBucket = decoded.substring(lastSlash);
                    return possibleBucket.startsWith("/") ? possibleBucket.substring(1) : possibleBucket;
                }
                return null;
            }
            return decoded.substring(bucketIndex + bucketName.length() + 1);
        } catch (Exception e) {
            return null;
        }
    }

    private String extractBucketName(String fileUrl) {
        if (StrUtil.isBlank(fileUrl)) return null;
        try {
            String decoded = URLUtil.decode(fileUrl, StandardCharsets.UTF_8);
            int bucketIndex = decoded.indexOf("/" + bucketName + "/");
            if (bucketIndex != -1) return bucketName;
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    private String getFileExtension(String filename) {
        if (StrUtil.isBlank(filename) || !filename.contains(".")) return "";
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }

    private String guessContentType(String extension) {
        if (StrUtil.isBlank(extension)) return "application/octet-stream";
        return switch (extension.toLowerCase()) {
            case ".jpg", ".jpeg" -> "image/jpeg";
            case ".png" -> "image/png";
            case ".gif" -> "image/gif";
            case ".bmp" -> "image/bmp";
            case ".webp" -> "image/webp";
            case ".svg" -> "image/svg+xml";
            case ".pdf" -> "application/pdf";
            case ".doc", ".docx" -> "application/msword";
            case ".xls", ".xlsx" -> "application/vnd.ms-excel";
            case ".ppt", ".pptx" -> "application/vnd.ms-powerpoint";
            case ".txt" -> "text/plain";
            case ".html", ".htm" -> "text/html";
            case ".css" -> "text/css";
            case ".js" -> "application/javascript";
            case ".json" -> "application/json";
            case ".xml" -> "application/xml";
            case ".zip" -> "application/zip";
            case ".mp3" -> "audio/mpeg";
            case ".wav" -> "audio/wav";
            case ".mp4" -> "video/mp4";
            default -> "application/octet-stream";
        };
    }
}
