package com.zyy.exception;

/**
 * Resource Not Found Exception
 */
public class ResourceNotFoundException extends BusinessException {
    public ResourceNotFoundException(String message) {
        super(404, message);
    }
}
