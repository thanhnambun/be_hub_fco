package com.fco.platform.auth.application;

public interface TokenBlacklistService {
    void blacklistAllUserTokens(Long userId);
}
