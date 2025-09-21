package com.anthropic.claude.exceptions;

public class ProcessExecutionException extends ClaudeCodeException {
    private final int exitCode;

    public ProcessExecutionException(String message, int exitCode) {
        super("PROCESS_EXECUTION_ERROR", message);
        this.exitCode = exitCode;
    }

    public ProcessExecutionException(String message, int exitCode, Throwable cause) {
        super("PROCESS_EXECUTION_ERROR", message, cause);
        this.exitCode = exitCode;
    }

    public int getExitCode() {
        return exitCode;
    }
}