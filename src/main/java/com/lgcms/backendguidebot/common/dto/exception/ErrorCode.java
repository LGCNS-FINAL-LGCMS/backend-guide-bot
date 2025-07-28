package com.lgcms.backendguidebot.common.dto.exception;

import lombok.Builder;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@Builder
public class ErrorCode {
    private final String status;
    private final String message;
    private final HttpStatus httpStatus;
}