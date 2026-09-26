package com.zyy.file.config;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.SetBucketPolicyArgs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * ==========================================================
 * MinIO Object Storage Configuration
 * Initializes MinIO client and bucket on application startup
 * ==========================================================
 *
 * @author System Architect
 */
@Configuration
public class MinioConfig {

    private static final Logger log = LoggerFactory.getLogger(MinioConfig.class);

    // ==================== Configuration Properties ====================

    @Value("${minio.endpoint}")
    private String endpoint;

    @Value("${minio.access-key}")
    private String accessKey;

    @Value("${minio.secret-key}")
    private String secretKey;

    @Value("${minio.bucket-name}")
    private String bucketName;

    // ==================== Availability Checker ====================

    @Autowired
    private MinioAvailability availability;

    // ==================== Bean Definition ====================

    @Bean
    public MinioClient minioClient() {
        // endpoint 可能是刻意不可达的（测试、未部署 MinIO 的开发机）。
        // MinioClient.builder().endpoint() 对畸形/不可路由地址直接抛异常，
        // 会中断整个 ApplicationContext。MinioUtil 以 @Autowired(required=false)
        // 注入本 bean，并按 availability 降级到本地存储，因此返回 null
        // 就是既有的"存储不可用"信号。
        if (endpoint == null || endpoint.isBlank()) {
            log.warn("[MinIO] 未配置 endpoint，降级为本地存储");
            availability.setAvailable(false, "No endpoint configured");
            return null;
        }
        try {
            log.info("[MinIO] Initializing MinIO client | Endpoint: {}", endpoint);

            MinioClient client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

            log.info("[MinIO] MinioClient bean created successfully");
            return client;
        } catch (Exception e) {
            log.warn("[MinIO] endpoint 无效 [{}]，降级为本地存储: {}", endpoint, e.getMessage());
            availability.setAvailable(false, e.getMessage());
            return null;
        }
    }

    // ==================== Bucket Initialization ====================

    @PostConstruct
    public void initBucket() {
        try {
            log.info("[MinIO] ========== Bucket Initialization ==========");
            log.info("[MinIO] Target Bucket: {}", bucketName);

            // Create a separate client instance for bucket init (avoids circular reference)
            MinioClient initClient = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();

            boolean exists = initClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(bucketName)
                    .build()
            );

            if (exists) {
                log.info("[MinIO] Bucket [{}] already exists, skipping creation", bucketName);
            } else {
                log.info("[MinIO] Creating new Bucket [{}]...", bucketName);
                initClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(bucketName)
                        .build()
                );
                log.info("[MinIO] Bucket [{}] created successfully", bucketName);
            }

            applyPublicReadPolicy(initClient);

            // Mark MinIO as available
            availability.setAvailable(true, "MinIO service is healthy");
            log.info("[MinIO] ========== Bucket Init Complete ==========");

        } catch (Exception e) {
            // Mark MinIO as unavailable
            availability.setAvailable(false, e.getMessage());
            log.error("[MinIO] Bucket initialization failed | Error: {}", e.getMessage());
            log.warn("[MinIO] Will use fallback storage (local filesystem)");
            log.warn("[MinIO] To fix: Run 'docker-compose -f doc/startup/minio-docker-compose.yml up -d'");
        }
    }

    private void applyPublicReadPolicy(MinioClient client) throws Exception {
        String policyJson = String.format("""
            {
              "Version": "2012-10-17",
              "Statement": [
                {
                  "Sid": "PublicReadGetObject",
                  "Effect": "Allow",
                  "Principal": { "AWS": ["*"] },
                  "Action": ["s3:GetObject", "s3:GetObjectVersion"],
                  "Resource": ["arn:aws:s3:::%s/*"]
                },
                {
                  "Sid": "AllowListBucket",
                  "Effect": "Allow",
                  "Principal": { "AWS": ["*"] },
                  "Action": ["s3:ListBucket"],
                  "Resource": ["arn:aws:s3:::%s"]
                }
              ]
            }
            """, bucketName, bucketName);

        log.debug("[MinIO] Applying public read policy...");

        client.setBucketPolicy(
            SetBucketPolicyArgs.builder()
                .bucket(bucketName)
                .config(policyJson)
                .build()
        );

        log.info("[MinIO] Public read policy applied successfully");
    }
}
