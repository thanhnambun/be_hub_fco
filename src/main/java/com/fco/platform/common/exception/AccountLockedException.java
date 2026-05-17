package com.fco.platform.common.exception;

public class AccountLockedException extends BusinessException {

    public AccountLockedException(String message) {
        super(message, ErrorCode.AUTH_LOCKED);
    }

    public AccountLockedException() {
        super(ErrorCode.AUTH_LOCKED);
    }
}
