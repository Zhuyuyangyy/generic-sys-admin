package com.zyy.file.config;





import cn.hutool.core.io.FileUtil;


import cn.hutool.core.util.IdUtil;


import cn.hutool.core.util.StrUtil;


import org.slf4j.Logger;


import org.slf4j.LoggerFactory;


import org.springframework.beans.factory.annotation.Value;


import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;


import org.springframework.context.annotation.Configuration;


import org.springframework.web.multipart.MultipartFile;





import jakarta.annotation.PostConstruct;


import java.io.File;


import java.nio.file.Files;


import java.nio.file.Path;


import java.nio.file.Paths;


import java.nio.file.StandardCopyOption;





/**


 * MinIO fallback: local filesystem storage strategy.


 * Triggered when MinIO is unavailable at startup.


 *


 * Storage path: {user.home}/.generic-sys-admin/uploads/


 * Windows example: C:/Users/username/.generic-sys-admin/uploads/


 * Linux/Mac: ~/.generic-sys-admin/uploads/


 *


 * Access URL: /local-files/{year}/{month}/{uuid}.{ext}


 *


 * @author Alice


 */


@Configuration


@ConditionalOnProperty(name = "storage.fallback-to-local", havingValue = "true")


public class LocalFileStorageStrategy {





    private static final Logger log = LoggerFactory.getLogger(LocalFileStorageStrategy.class);





    /**
     * 本地存储根目录。默认取 user.home。
     *
     * 原来写 #{user.home} —— @Value 在 bean 属性上解析该 SpEL 会抛
     * EL1008E "Property or field 'user' cannot be found on
     * BeanExpressionContext"，导致整个容器启动失败（Boot 3.2 起更严格）。
     * 现在用 ${user.home} 属性占位符，Spring 会解析它；仍为空时由
     * PostConstruct 兜底。
     */
    @Value("${storage.local.base-dir:${user.home}}")


    private String baseDir;

    @jakarta.annotation.PostConstruct
    private void ensureBaseDir() {
        if (baseDir == null || baseDir.isBlank()) {
            baseDir = System.getProperty("user.home", System.getProperty("java.io.tmpdir"));
        }
    }





    @Value("${storage.local.sub-dir:.generic-sys-admin/uploads}")


    private String subDir;





    @Value("${storage.local.max-file-size:10485760}")


    private long maxFileSize;





    private Path uploadBasePath;


    private boolean initialized = false;





    @PostConstruct


    public void init() {


        try {


            uploadBasePath = Paths.get(baseDir, subDir);


            Files.createDirectories(uploadBasePath);


            log.info("[LocalFileStorage] Init OK | Path: {}", uploadBasePath.toAbsolutePath());


            log.warn("[LocalFileStorage] MinIO unavailable - using LOCAL FILESYSTEM fallback");


            initialized = true;


        } catch (Exception e) {


            log.error("[LocalFileStorage] Init FAILED: {}", e.getMessage());


            initialized = false;


        }


    }





    /**


     * Upload MultipartFile to local filesystem.


     * @param file Spring MultipartFile


     * @param customFileName null = auto-generate UUID filename


     * @return access path like /local-files/2026/04/{uuid}.{ext}


     */


    public String uploadFile(MultipartFile file, String customFileName) {


        ensureInitialized();


        if (file == null || file.isEmpty()) {


            throw new IllegalArgumentException("File cannot be empty");


        }


        if (file.getSize() > maxFileSize) {


            throw new IllegalArgumentException("File size exceeds limit: " + formatSize(maxFileSize));


        }





        String ext = getExtension(file.getOriginalFilename());


        String finalName = StrUtil.isNotBlank(customFileName)


            ? customFileName


            : IdUtil.fastSimpleUUID() + ext;





        String yearMonth = cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyy/MM");


        Path targetDir = uploadBasePath.resolve(yearMonth);





        try {


            Files.createDirectories(targetDir);


            Path targetPath = targetDir.resolve(finalName);


            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);


            String accessPath = "/local-files/" + yearMonth + "/" + finalName;


            log.info("[LocalFileStorage] Upload OK | Path: {}", accessPath);


            return accessPath;


        } catch (Exception e) {


            throw new RuntimeException("Upload failed: " + e.getMessage(), e);


        }


    }





    /**


     * Upload byte array (used by EasyExcel export etc.)


     */


    public String uploadBytes(byte[] data, String fileName, String contentType) {


        ensureInitialized();


        if (data == null || data.length == 0) {


            throw new IllegalArgumentException("Data cannot be empty");


        }


        try {


            String yearMonth = cn.hutool.core.date.DateUtil.format(new java.util.Date(), "yyyy/MM");


            Path targetDir = uploadBasePath.resolve(yearMonth);


            Files.createDirectories(targetDir);


            Path targetPath = targetDir.resolve(fileName);


            Files.write(targetPath, data);


            String accessPath = "/local-files/" + yearMonth + "/" + fileName;


            log.info("[LocalFileStorage] Bytes upload OK: {}", accessPath);


            return accessPath;


        } catch (Exception e) {


            throw new RuntimeException("Bytes upload failed: " + e.getMessage(), e);


        }


    }





    /**


     * Delete local file by access path.


     */


    public void deleteFile(String accessPath) {


        ensureInitialized();


        if (StrUtil.isBlank(accessPath)) return;


        try {


            String relativePath = accessPath.replace("/local-files/", "");


            Path filePath = uploadBasePath.resolve(relativePath);


            if (Files.exists(filePath)) {


                Files.delete(filePath);


                log.info("[LocalFileStorage] Deleted: {}", filePath);


            }


        } catch (Exception e) {


            log.error("[LocalFileStorage] Delete failed: {}", e.getMessage());


        }


    }





    public String getAbsolutePath(String accessPath) {


        if (StrUtil.isBlank(accessPath)) return null;


        String relativePath = accessPath.replace("/local-files/", "");


        return uploadBasePath.resolve(relativePath).toAbsolutePath().toString();


    }





    public boolean exists(String accessPath) {


        if (StrUtil.isBlank(accessPath)) return false;


        String relativePath = accessPath.replace("/local-files/", "");


        return Files.exists(uploadBasePath.resolve(relativePath));


    }





    private void ensureInitialized() {


        if (!initialized) {


            throw new IllegalStateException("LocalStorage not initialized - check directory permissions");


        }


    }





    private String getExtension(String filename) {


        if (StrUtil.isBlank(filename) || !filename.contains(".")) return "";


        return filename.substring(filename.lastIndexOf(".")).toLowerCase();


    }





    private String formatSize(long bytes) {


        if (bytes < 1024) return bytes + " B";


        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);


        return String.format("%.1f MB", bytes / (1024.0 * 1024.0));


    }


}


