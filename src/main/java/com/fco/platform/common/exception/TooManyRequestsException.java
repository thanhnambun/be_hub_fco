package com.fco.platform.common.exception;

public class TooManyRequestsException extends BusinessException {

    public TooManyRequestsException(String message) {
        super(message, ErrorCode.AUTH_RATE_LIMITED);
    }

    public TooManyRequestsException() {
        super(ErrorCode.AUTH_RATE_LIMITED);
    }
}
