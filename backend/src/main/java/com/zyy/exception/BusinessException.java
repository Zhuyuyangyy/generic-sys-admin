package com.zyy.exception;

/**
 * Business Exception
 */
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(String message) {
        this(4004, message);
    }

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
