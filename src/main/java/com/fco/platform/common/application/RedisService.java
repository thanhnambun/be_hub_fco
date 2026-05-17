package com.fco.platform.common.application;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class RedisService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final StringRedisTemplate stringRedisTemplate;

    /** Counter keys — plain string INCR/EXPIRE, không dùng JSON serializer. */
    public Long incrementCounter(String key) {
        return stringRedisTemplate.opsForValue().increment(key);
    }

    public void expireCounter(String key, Duration ttl) {
        stringRedisTemplate.expire(key, ttl);
    }

    public String getCounter(String key) {
        return stringRedisTemplate.opsForValue().get(key);
    }

    public boolean deleteCounter(String key) {
        return Boolean.TRUE.equals(stringRedisTemplate.delete(key));
    }

    public void set(String key, String value, Duration ttl) {
        redisTemplate.opsForValue().set(key, value, ttl);
    }

    public void set(String key, String value) {
        redisTemplate.opsForValue().set(key, value);
    }

    public String get(String key) {
        Object value = redisTemplate.opsForValue().get(key);
        return value != null ? value.toString() : null;
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key);
    }

    public Long increment(String key, long delta) {
        return redisTemplate.opsForValue().increment(key, delta);
    }

    public boolean delete(String key) {
        return Boolean.TRUE.equals(redisTemplate.delete(key));
    }

    public boolean exists(String key) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public Duration getTtl(String key) {
        Long seconds = redisTemplate.getExpire(key);
        if (seconds == null || seconds < 0) {
            return Duration.ZERO;
        }
        return Duration.ofSeconds(seconds);
    }
}
