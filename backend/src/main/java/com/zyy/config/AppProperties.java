package com.zyy.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Centralized Application Configuration Properties.
 * <p>
 * This component provides type-safe access to all externalized configuration properties
 * through a hierarchical property source mechanism. All sensitive credentials are
 * externalized via environment variables with sensible defaults for development.
 * <p>
 * Configuration hierarchy:
 * <ul>
 *   <li>Security: JWT secret, token expiration, BCrypt cost factor</li>
 *   <li>Storage: MinIO endpoint, credentials, bucket configuration</li>
 *   <li>AI Services: Minimax API credentials and endpoints</li>
 *   <li>Application: Demo mode, feature toggles</li>
 * </ul>
 *
 * @author System Architect
 * @version 1.0.0
 * @since Spring Boot 3.2.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "sys.config")
public class AppProperties {

    /** Security configuration properties */
    private SecurityProperties security = new SecurityProperties();

    /** Storage configuration properties (MinIO) */
    private StorageProperties storage = new StorageProperties();

    /** AI service configuration properties */
    private AiProperties ai = new AiProperties();

    /** Application-level configuration */
    private AppConfig app = new AppConfig();

    /**
     * Security-related configuration properties.
     * Manages JWT authentication parameters and password encoding settings.
     */
    @Data
    public static class SecurityProperties {
        /** JWT signing secret key (minimum 256 bits for HS256). MUST be set via JWT_SECRET env var in production. */
        private String jwtSecret;

        /** JWT token validity duration in seconds (default: 2 hours) */
        private long jwtExpiration = 7200;

        /** JWT refresh token validity duration in seconds (default: 7 days) */
        private long jwtRefreshExpiration = 604800;

        /** BCrypt password encoding cost factor (default: 10) */
        private int bcryptCostFactor = 10;

        /** Maximum failed login attempts before account lockout */
        private int maxFailedAttempts = 5;

        /** Account lockout duration in minutes */
        private int lockoutMinutes = 30;
    }

    /**
     * Object storage configuration properties (MinIO compatible).
     * Supports both MinIO self-hosted and S3-compatible cloud storage.
     */
    @Data
    public static class StorageProperties {
        /** S3-compatible endpoint URL */
        private String endpoint = "http://localhost:9000";

        /** Access key identifier */
        private String accessKey;

        /** Secret access key */
        private String secretKey;

        /** Target bucket name */
        private String bucketName = "generic-sys-admin";

        /** Pre-signed URL validity duration in seconds */
        private int presignedUrlExpiration = 3600;

        /** Enable path-style access (for MinIO compatibility) */
        private boolean pathStyleAccess = true;
    }

    /**
     * AI service provider configuration properties.
     * Supports multiple AI backends with automatic fallback capability.
     */
    @Data
    public static class AiProperties {
        /** Minimax TTS service configuration */
        private MinimaxProperties minimax = new MinimaxProperties();

        @Data
        public static class MinimaxProperties {
            /** API base URL */
            private String apiUrl = "https://api.minimax.chat";

            /** Application identifier */
            private String appId;

            /** API access key */
            private String apiKey;

            /** Group identifier */
            private String groupId;

            /** Voice conversion name */
            private String vcn = "female_tianmei";
        }
    }

    /**
     * Application-level configuration and feature toggles.
     */
    @Data
    public static class AppConfig {
        /** Enable demo mode with mock services */
        private boolean demoMode = false;

        /** Enable operation audit logging via AOP */
        private boolean auditEnabled = true;

        /** Enable API documentation endpoint */
        private boolean docEnabled = true;
    }
}
