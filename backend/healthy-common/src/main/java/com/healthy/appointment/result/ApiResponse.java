package com.healthy.appointment.result;

public record ApiResponse<T>(int code, String message, T data) {
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(0, "success", data);
    }

    public static ApiResponse<Void> success() {
        return new ApiResponse<>(0, "success", null);
    }

    public static ApiResponse<Void> failure(com.healthy.appointment.enumeration.ErrorCode errorCode) {
        return failure(errorCode, errorCode.message());
    }

    public static ApiResponse<Void> failure(com.healthy.appointment.enumeration.ErrorCode errorCode, String message) {
        return new ApiResponse<>(errorCode.code(), message, null);
    }
}
