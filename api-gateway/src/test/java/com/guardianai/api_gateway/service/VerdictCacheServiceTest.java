package com.guardianai.api_gateway.service;

import com.guardianai.api_gateway.dto.ScanRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class VerdictCacheServiceTest {

    private ValueOperations<String, String> ops;
    private VerdictCacheService cache;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ops = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(ops);
        cache = new VerdictCacheService(redis, JsonMapper.builder().build(), 60, 30, 360);
    }

    private ScanRequest request(String url, int scripts) {
        return new ScanRequest(url, Instant.now(),
                new ScanRequest.TlsInfo(true, "CA", null),
                List.of("a"),
                new ScanRequest.PageSignals(1, 1, scripts, 0, 5));
    }

    private AnalysisOutcome outcome(String level) {
        return new AnalysisOutcome(82, level, 0.6, List.of("f"), "e", List.of("r"),
                List.of(new AnalysisOutcome.IndicatorData("URL", "uses_https", "true", false)),
                List.of(new AnalysisOutcome.ElementData("PASSWORD_FIELD", "1", false, null)));
    }

    @Test
    void timestampHostCaseAndFragmentDoNotChangeTheFingerprint() {
        String a = cache.fingerprint(request("https://Example.com/login#top", 4));
        String b = cache.fingerprint(request("https://example.com/login", 4));
        assertEquals(a, b);
    }

    @Test
    void differentSignalsOrPathsChangeTheFingerprint() {
        String base = cache.fingerprint(request("https://example.com/login", 4));
        assertNotEquals(base, cache.fingerprint(request("https://example.com/login", 15)));
        assertNotEquals(base, cache.fingerprint(request("https://example.com/other", 4)));
    }

    @Test
    void fingerprintIsAHashThatHidesTheUrl() {
        String fp = cache.fingerprint(request("https://example.com/secret-token-123", 4));
        assertTrue(fp.matches("[0-9a-f]{64}"));
        assertFalse(fp.contains("example"));
    }

    @Test
    void outcomeSurvivesAStoreAndReadRoundTrip() {
        ArgumentCaptor<String> stored = ArgumentCaptor.forClass(String.class);
        cache.put("fp", outcome("LOW"));
        verify(ops).set(eq("vc:v1:fp"), stored.capture(), any(Duration.class));

        when(ops.get("vc:v1:fp")).thenReturn(stored.getValue());
        assertEquals(Optional.of(outcome("LOW")), cache.get("fp"));
    }

    @Test
    void ttlDependsOnTheRiskLevel() {
        cache.put("a", outcome("LOW"));
        cache.put("b", outcome("MEDIUM"));
        cache.put("c", outcome("HIGH"));

        verify(ops).set(eq("vc:v1:a"), anyString(), eq(Duration.ofMinutes(60)));
        verify(ops).set(eq("vc:v1:b"), anyString(), eq(Duration.ofMinutes(30)));
        verify(ops).set(eq("vc:v1:c"), anyString(), eq(Duration.ofMinutes(360)));
    }

    @Test
    void missAndUnreadableValueAreBothTreatedAsMiss() {
        when(ops.get("vc:v1:none")).thenReturn(null);
        when(ops.get("vc:v1:bad")).thenReturn("{not json");

        assertTrue(cache.get("none").isEmpty());
        assertTrue(cache.get("bad").isEmpty());
    }

    @Test
    void redisDownDoesNotBreakReadsOrWrites() {
        when(ops.get(anyString())).thenThrow(new RedisConnectionFailureException("down"));
        doThrow(new RedisConnectionFailureException("down"))
                .when(ops).set(anyString(), anyString(), any(Duration.class));

        assertTrue(cache.get("x").isEmpty());
        assertDoesNotThrow(() -> cache.put("x", outcome("LOW")));
    }
}