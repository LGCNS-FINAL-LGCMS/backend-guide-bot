package com.lgcms.backendguidebot.common.dto.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum DataError implements ErrorCodeInterface {
    DATA_NOT_FOUND("DATAE-01", "FAQ데이터가 없습니다.", HttpStatus.NOT_FOUND),
    DATA_FAIL_EMBEDDING("DATAE-02","데이터 임베딩을 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR),
    DATA_FAIL_SAVE("DATAE-03","벡터db에 저장 실패했습니다.", HttpStatus.INTERNAL_SERVER_ERROR);


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