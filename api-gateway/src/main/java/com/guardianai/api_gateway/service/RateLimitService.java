package com.guardianai.api_gateway.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RateLimitService {

    private static final Logger log = LoggerFactory.getLogger(RateLimitService.class);
    private static final String PREFIX = "rl:";

    private final StringRedisTemplate redis;

    public RateLimitService(StringRedisTemplate redis) {
        this.redis = redis;
    }

    public RateLimitResult check(String key, int limit, Duration window) {
        String redisKey = PREFIX + key;
        try {
            // INCR is atomic: two simultaneous requests can never get the same number
            Long count = redis.opsForValue().increment(redisKey);
            if (count == null) {
                return failOpen(limit);
            }

            if (count == 1L) {
                redis.expire(redisKey, window);          // the first request starts the window
            }

            Long ttl = redis.getExpire(redisKey);        // seconds left; -1 means "no expiry"
            if (ttl == null || ttl < 0) {
                redis.expire(redisKey, window);          // repair a key that would never expire
                ttl = window.toSeconds();
            }

            boolean allowed = count <= limit;
            long remaining = Math.max(limit - count, 0);
            return new RateLimitResult(allowed, limit, remaining, ttl);

        } catch (DataAccessException e) {
            // never log the key: it contains an IP address or a user ID
            log.warn("Rate limiter unavailable, allowing request ({})", e.getClass().getSimpleName());
            return failOpen(limit);
        }
    }

    private RateLimitResult failOpen(int limit) {
        return new RateLimitResult(true, limit, limit, 0);
    }
}