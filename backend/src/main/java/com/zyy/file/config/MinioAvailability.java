package com.zyy.file.config;

import org.springframework.stereotype.Component;

/**
 * MinIO availability status holder.
 * 
 * This class is auto-created by MinioConfig to track MinIO connection status.
 * MinioConfig creates the MinioClient Bean and sets availability status.
 * MinioUtil uses this Bean to check if MinIO is available.
 *
 * @author System Architect
 */
@Component
public class MinioAvailability {
    private boolean available = false;
    private String reason = "Not initialized";

    public boolean isAvailable() { return available; }
    public String getReason() { return reason; }

    public void setAvailable(boolean available, String reason) {
        this.available = available;
        this.reason = reason;
    }
}
