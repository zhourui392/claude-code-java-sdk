package com.anthropic.claude.exceptions;

public class ClaudeCodeException extends RuntimeException {
    private final String errorCode;

    public ClaudeCodeException(String message) {
        super(message);
        this.errorCode = "UNKNOWN_ERROR";
    }

    public ClaudeCodeException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "UNKNOWN_ERROR";
    }

    public ClaudeCodeException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ClaudeCodeException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}