package com.fde.sentinelflow.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import java.time.Duration;

@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private static final int MAX_REQUESTS_PER_MINUTE = 100;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public boolean isAllowed(String tenantId) {
        String key = "rate_limit:" + tenantId;
        Long currentRequests = redisTemplate.opsForValue().increment(key);

        if (currentRequests != null && currentRequests == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(1));
        }

        return currentRequests != null && currentRequests <= MAX_REQUESTS_PER_MINUTE;
    }
}
