package com.ra.base_spring_boot.exception;

public class AccountLockedException extends BusinessException {
    public AccountLockedException(String message) {
        super(message, ErrorCode.AUTH_LOCKED);
    }
    
    public AccountLockedException() {
        super(ErrorCode.AUTH_LOCKED);
    }
}
