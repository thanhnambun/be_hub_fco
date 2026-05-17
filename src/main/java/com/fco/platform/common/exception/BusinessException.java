package com.fco.platform.common.exception;

import java.util.Collections;
import java.util.Map;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;
    private final Map<String, Object> extensions;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
        this.extensions = Collections.emptyMap();
    }

    public BusinessException(String message, ErrorCode errorCode) {
        super(message);
        this.errorCode = errorCode;
        this.extensions = Collections.emptyMap();
    }

    public BusinessException(String message, ErrorCode errorCode, Map<String, Object> extensions) {
        super(message);
        this.errorCode = errorCode;
        this.extensions = extensions == null ? Collections.emptyMap() : Map.copyOf(extensions);
    }
}
