package com.guardianai.api_gateway.service;

import com.guardianai.api_gateway.dto.AuditItem;
import com.guardianai.api_gateway.dto.PageResponse;
import com.guardianai.api_gateway.entity.AuditLog;
import com.guardianai.api_gateway.repository.AuditLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.time.ZoneOffset;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final AuditLogRepository repository;
    private final TransactionTemplate ownTransaction;
    private final SecretKeySpec ipKey;

    public AuditService(AuditLogRepository repository,
                        PlatformTransactionManager transactionManager,
                        @Value("${guardian.audit.ip-hash-key}") String key) {
        if (key == null || key.length() < 32) {
            throw new IllegalStateException("guardian.audit.ip-hash-key must be at least 32 characters");
        }
        this.repository = repository;
        this.ownTransaction = new TransactionTemplate(transactionManager);
        this.ownTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.ipKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** Writes now, in its own transaction. Use it for outcomes that are already final (failures, denials, logins). */
    public void record(AuditAction action, UUID userId, String resource) {
        write(action, userId, resource, currentIp());
    }

    public void record(AuditAction action, UUID userId, String resource, String ip) {
        write(action, userId, resource, ip);
    }

    /** Writes after the surrounding transaction commits. Use it for changes that must really have happened. */
    public void recordAfterCommit(AuditAction action, UUID userId, String resource) {
        String ip = currentIp();      // read it now: the request is bound to this thread only
        if (TransactionSynchronizationManager.isActualTransactionActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    write(action, userId, resource, ip);
                }
            });
        } else {
            write(action, userId, resource, ip);
        }
    }

    @Transactional(readOnly = true)
    public PageResponse<AuditItem> myActivity(UUID userId, int page, int size) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), 50);

        Page<AuditLog> result = repository.findByUserId(
                userId, PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt")));

        List<AuditItem> items = result.getContent().stream()
                .map(a -> new AuditItem(a.getAction(), a.getResource(),
                        a.getCreatedAt().toInstant(ZoneOffset.UTC)))
                .toList();

        return new PageResponse<>(items, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages());
    }

    String hashIp(String ip) {
        if (ip == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");     // Mac objects are not thread-safe, so make one per call
            mac.init(ipKey);
            return HexFormat.of().formatHex(mac.doFinal(ip.getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("HmacSHA256 is not available", e);
        }
    }

    private void write(AuditAction action, UUID userId, String resource, String ip) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action.name());
            entry.setUserId(userId);
            entry.setResource(resource);
            entry.setIpHash(hashIp(ip));
            ownTransaction.executeWithoutResult(status -> repository.save(entry));
        } catch (RuntimeException e) {
            // an audit problem must never break the user's request, and the message must not carry data
            log.warn("Audit write failed for {} ({})", action, e.getClass().getSimpleName());
        }
    }

    private String currentIp() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest().getRemoteAddr();   // same source as the rate limiter, and not forgeable
        }
        return null;
    }
}