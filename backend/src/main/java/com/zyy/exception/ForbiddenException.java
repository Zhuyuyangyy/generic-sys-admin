package com.zyy.exception;

/**
 * Forbidden Exception
 */
public class ForbiddenException extends BusinessException {
    public ForbiddenException(String message) {
        super(403, message);
    }
}
