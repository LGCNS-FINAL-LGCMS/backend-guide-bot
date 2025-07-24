package com.lgcms.backendguidebot.common.dto.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum QnaError implements ErrorCodeInterface {
    QNA_NOT_FOUND("QNAE-01", "질문이 입력되지 않았습니다.", HttpStatus.NOT_FOUND),
    QNA_SERVER_ERROR("QNAE-02","답변생성에 문제가 생겼습니다.", HttpStatus.INTERNAL_SERVER_ERROR);


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
