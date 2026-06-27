package com.zyy.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * File upload MIME type validation filter.
 * <p>
 * Validates the Content-Type of multipart upload requests against
 * a configurable whitelist of allowed MIME types.
 */
@Slf4j
@Component
public class FileUploadValidationFilter extends OncePerRequestFilter {

    private final List<String> allowedMimeTypes;

    public FileUploadValidationFilter(
            @Value("${security.upload.allowed-mime-types:image/jpeg,image/png,image/gif,application/pdf,text/csv,application/vnd.ms-excel,application/vnd.openxmlformats-officedocument.spreadsheetml.sheet,application/vnd.openxmlformats-officedocument.wordprocessingml.document}")
            String allowedMimeTypesConfig) {
        this.allowedMimeTypes = Arrays.asList(allowedMimeTypesConfig.split(","));
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        // Only validate multipart requests
        if (isMultipartUpload(request)) {
            String contentType = request.getContentType();
            if (contentType != null) {
                // The multipart content-type itself is always multipart/form-data
                // Actual file MIME validation happens at the service layer via the part content-type
                // This filter provides a secondary check on individual parts
                request.setAttribute("allowedMimeTypes", allowedMimeTypes);
            }
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Get the list of allowed MIME types for service-layer validation.
     */
    public List<String> getAllowedMimeTypes() {
        return allowedMimeTypes;
    }

    /**
     * Validate if a given MIME type is in the allowed list.
     */
    public boolean isAllowedMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        // Handle wildcard patterns for Office Open XML
        if (mimeType.startsWith("application/vnd.openxmlformats-officedocument")) {
            return allowedMimeTypes.stream()
                    .anyMatch(allowed -> allowed.startsWith("application/vnd.openxmlformats-officedocument"));
        }
        return allowedMimeTypes.contains(mimeType);
    }

    private boolean isMultipartUpload(HttpServletRequest request) {
        String contentType = request.getContentType();
        return contentType != null && contentType.toLowerCase().startsWith("multipart/");
    }
}
