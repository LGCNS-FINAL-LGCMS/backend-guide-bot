package com.lgcms.backendguidebot.common.dto;


public record BaseResponse<T>(
        String status,
        String message,
        T data
) {
    public static <T> BaseResponse<T> ok(T result) {
        return new BaseResponse<>("OK", "호출에 성공했습니다", result);
    }

    public static <T> BaseResponse<T> onFailure(String status, String message, T data) {
        return new BaseResponse<>(status, message, data);

    }
}