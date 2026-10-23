package com.guardianai.api_gateway.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RateLimitServiceTest {

    private static final Duration WINDOW = Duration.ofSeconds(60);

    private StringRedisTemplate redis;
    private ValueOperations<String, String> ops;
    private RateLimitService service;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        service = new RateLimitService(redis);
    }

    @Test
    void firstRequestStartsTheWindowAndIsAllowed() {
        when(ops.increment("rl:k")).thenReturn(1L);
        when(redis.getExpire("rl:k")).thenReturn(60L);

        RateLimitResult r = service.check("k", 5, WINDOW);

        assertTrue(r.allowed());
        assertEquals(4, r.remaining());
        verify(redis).expire("rl:k", WINDOW);
    }

    @Test
    void requestAtTheLimitIsStillAllowed() {
        when(ops.increment("rl:k")).thenReturn(5L);
        when(redis.getExpire("rl:k")).thenReturn(30L);

        RateLimitResult r = service.check("k", 5, WINDOW);

        assertTrue(r.allowed());
        assertEquals(0, r.remaining());
    }

    @Test
    void requestOverTheLimitIsBlockedWithRetryAfter() {
        when(ops.increment("rl:k")).thenReturn(6L);
        when(redis.getExpire("rl:k")).thenReturn(42L);

        RateLimitResult r = service.check("k", 5, WINDOW);

        assertFalse(r.allowed());
        assertEquals(0, r.remaining());
        assertEquals(42, r.retryAfterSeconds());
    }

    @Test
    void keyWithoutExpiryIsRepaired() {
        when(ops.increment("rl:k")).thenReturn(3L);
        when(redis.getExpire("rl:k")).thenReturn(-1L);

        service.check("k", 5, WINDOW);

        verify(redis).expire("rl:k", WINDOW);
    }

    @Test
    void whenRedisIsDownTheRequestIsAllowed() {
        when(ops.increment("rl:k")).thenThrow(new RedisConnectionFailureException("down"));

        RateLimitResult r = service.check("k", 5, WINDOW);

        assertTrue(r.allowed());
    }
}