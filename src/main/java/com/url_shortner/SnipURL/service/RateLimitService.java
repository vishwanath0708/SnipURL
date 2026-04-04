package com.url_shortner.SnipURL.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class RateLimitService {

    private final RedisTemplate<String, Long> redisTemplate;

    public boolean isAllowed(String key, int limit, int durationSeconds) {
        Long currentCount = redisTemplate.opsForValue().increment(key);

        if (currentCount != null && currentCount == 1) {
            redisTemplate.expire(key, Duration.ofSeconds(durationSeconds));
        }

        if (currentCount != null && currentCount > limit) {
            log.warn("Rate limit exceeded for key: {}", key);
            return false;
        }

        return true;
    }

    public long getRemaining(String key, int limit) {
        try {
            Long current = redisTemplate.opsForValue().get(key);
            if (current == null) return limit;
            return Math.max(0, limit - current);
        } catch (Exception e) {
            return limit;
        }
    }

    public long getResetTime(String key) {
        try {
            Long ttl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl != null ? Math.max(0, ttl) : 0;
        } catch (Exception e) {
            return 0;
        }
    }
}