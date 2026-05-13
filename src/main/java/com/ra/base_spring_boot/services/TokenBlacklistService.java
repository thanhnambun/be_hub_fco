package com.ra.base_spring_boot.services;

public interface TokenBlacklistService {
    void blacklistAllUserTokens(Long userId);
}
