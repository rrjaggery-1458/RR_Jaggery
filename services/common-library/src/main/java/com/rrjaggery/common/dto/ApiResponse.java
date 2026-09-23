package com.rrjaggery.common.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {

    private final boolean success;
    private final Instant timestamp;
    private final String message;
    private final T data;
    private final ErrorResponse error;

    public ApiResponse(boolean success, String message, T data, ErrorResponse error) {
        this.success = success;
        this.timestamp = Instant.now();
        this.message = message;
        this.data = data;
        this.error = error;
    }

    /** Single-data factory — no message. */
    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, null, data, null);
    }

    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(true, null, data, null);
    }

    public static <T> ApiResponse<T> success(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }

    /** Single-data factory with a human-readable message. */
    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }

    /** 201 Created factory with a human-readable message. */
    public static <T> ApiResponse<T> created(T data, String message) {
        return new ApiResponse<>(true, message, data, null);
    }

    public static <T> ApiResponse<T> error(ErrorResponse error) {
        return new ApiResponse<>(false, null, null, error);
    }

    public static <T> ApiResponse<T> error(String message, String code) {
        return new ApiResponse<>(false, null, null, new ErrorResponse(code, message));
    }

    public boolean isSuccess() {
        return success;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public String getMessage() {
        return message;
    }

    public T getData() {
        return data;
    }

    public ErrorResponse getError() {
        return error;
    }
}
