package com.fco.platform.common.exception;

public class TokenBlacklistedException extends BusinessException {

    public TokenBlacklistedException(String message) {
        super(message, ErrorCode.AUTH_TOKEN_BLACKLISTED);
    }

    public TokenBlacklistedException() {
        super(ErrorCode.AUTH_TOKEN_BLACKLISTED);
    }
}
