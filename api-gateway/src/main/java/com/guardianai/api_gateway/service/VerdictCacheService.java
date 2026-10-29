package com.guardianai.api_gateway.service;

import com.guardianai.api_gateway.dto.ScanRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.util.HexFormat;
import java.util.Optional;

@Service
public class VerdictCacheService {

    private static final Logger log = LoggerFactory.getLogger(VerdictCacheService.class);
    private static final String PREFIX = "vc:v1:";   // bump v1 if AnalysisOutcome changes shape

    private final StringRedisTemplate redis;
    private final JsonMapper json;
    private final Duration lowTtl;
    private final Duration mediumTtl;
    private final Duration highTtl;

    public VerdictCacheService(StringRedisTemplate redis,
                               JsonMapper json,
                               @Value("${guardian.cache.low-ttl-minutes:60}") long lowMinutes,
                               @Value("${guardian.cache.medium-ttl-minutes:30}") long mediumMinutes,
                               @Value("${guardian.cache.high-ttl-minutes:360}") long highMinutes) {
        this.redis = redis;
        this.json = json;
        this.lowTtl = Duration.ofMinutes(lowMinutes);
        this.mediumTtl = Duration.ofMinutes(mediumMinutes);
        this.highTtl = Duration.ofMinutes(highMinutes);
    }

    /** Hash of every input that affects the verdict. The timestamp is left out on purpose. */
    public String fingerprint(ScanRequest r) {
        StringBuilder sb = new StringBuilder(UrlUtils.normalize(r.url())).append('\n');

        if (r.tls() == null) {
            sb.append("tls=none");
        } else {
            sb.append("tls=").append(r.tls().valid()).append('|')
              .append(r.tls().issuer()).append('|').append(r.tls().expires());
        }
        sb.append('\n');

        sb.append("redirects=").append(r.redirects() == null ? "none" : String.join(",", r.redirects()))
          .append('\n');

        if (r.page() == null) {
            sb.append("page=none");
        } else {
            sb.append("page=").append(r.page().forms()).append(',')
              .append(r.page().passwordFields()).append(',')
              .append(r.page().externalScripts()).append(',')
              .append(r.page().iframes()).append(',')
              .append(r.page().externalLinks());
        }
        return sha256Hex(sb.toString());
    }

    public Optional<AnalysisOutcome> get(String fingerprint) {
        try {
            String value = redis.opsForValue().get(PREFIX + fingerprint);
            if (value == null) {
                return Optional.empty();
            }
            return Optional.of(json.readValue(value, AnalysisOutcome.class));
        } catch (RuntimeException e) {
            // Redis down, timeout or unreadable JSON: act as a cache miss. The key holds a hash, but stay quiet anyway
            log.warn("Verdict cache read failed, treating as miss ({})", e.getClass().getSimpleName());
            return Optional.empty();
        }
    }

    public void put(String fingerprint, AnalysisOutcome outcome) {
        try {
            redis.opsForValue().set(PREFIX + fingerprint, json.writeValueAsString(outcome), ttlFor(outcome.level()));
        } catch (RuntimeException e) {
            log.warn("Verdict cache write failed, continuing without cache ({})", e.getClass().getSimpleName());
        }
    }

    private Duration ttlFor(String level) {
        return switch (level) {
            case "LOW" -> lowTtl;
            case "MEDIUM" -> mediumTtl;
            default -> highTtl;
        };
    }

    private static String sha256Hex(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 is not available", e);
        }
    }
}