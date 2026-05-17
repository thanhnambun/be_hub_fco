package com.fco.platform.auth.application;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenBlacklistServiceImpl implements TokenBlacklistService {

    private final RedisTemplate<String, Object> redisTemplate;
    
    // We assume tokens are valid for some time, so we just set a key for the user indicating all tokens before this time are invalid.
    // An alternative is to store individual tokens if they are logged out.
    // Since the prompt asks to blacklist *all* user tokens, we can set a timestamp of the logout/lock event.
    // The filter would then check if the token was issued before this timestamp.
    // However, if we just want a simple blacklist implementation as requested:
    
    private static final String BLACKLIST_USER_PREFIX = "blacklist:user:";

    @Override
    public void blacklistAllUserTokens(Long userId) {
        String key = BLACKLIST_USER_PREFIX + userId;
        // Setting the current time in milliseconds. Any token issued before this time is considered invalid.
        // We can keep it for the max duration of a token (e.g., 7 days if refresh token is 7 days, or 15 mins for access token)
        // Let's set it to 7 days (7 * 24 * 60 * 60 seconds)
        redisTemplate.opsForValue().set(key, System.currentTimeMillis(), 7, TimeUnit.DAYS);
        log.info("Blacklisted all tokens for user id: {}", userId);
    }
}
