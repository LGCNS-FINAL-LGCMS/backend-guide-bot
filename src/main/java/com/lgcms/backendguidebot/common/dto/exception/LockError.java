package com.lgcms.backendguidebot.common.dto.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum LockError implements ErrorCodeInterface{
    LOCK_ALREADY_HELD("LCKE-01", "락이 이미 점유중입니다", HttpStatus.INTERNAL_SERVER_ERROR),
    LOCK_INTERRUPTED("LCKE-02", "락 처리중 인터럽트 발생", HttpStatus.INTERNAL_SERVER_ERROR),
    ;


    private final String status;
    private final String message;
    private final HttpStatus httpStatus;

    @Override
    public ErrorCode getErrorCode() {
        return ErrorCode.builder()
                .status(status)
                .message(message)
                .httpStatus(httpStatus)
                .build();
    }
}