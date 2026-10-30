package com.guardianai.api_gateway.service;

import com.guardianai.api_gateway.entity.AuditLog;
import com.guardianai.api_gateway.repository.AuditLogRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuditServiceTest {

    private static final String KEY = "audit-test-key-audit-test-key-123456";

    private AuditLogRepository repo;
    private PlatformTransactionManager tm;
    private AuditService service;

    @BeforeEach
    void setUp() {
        repo = mock(AuditLogRepository.class);
        tm = mock(PlatformTransactionManager.class);
        when(tm.getTransaction(any())).thenReturn(new SimpleTransactionStatus());
        service = new AuditService(repo, tm, KEY);
    }

    @Test
    void recordStoresActionUserResourceAndAHashedIp() {
        UUID userId = UUID.randomUUID();

        service.record(AuditAction.LOGIN_FAILED, userId, "res-1", "203.0.113.7");

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repo).save(captor.capture());
        AuditLog saved = captor.getValue();
        assertEquals("LOGIN_FAILED", saved.getAction());
        assertEquals(userId, saved.getUserId());
        assertEquals("res-1", saved.getResource());
        assertNotEquals("203.0.113.7", saved.getIpHash());
        assertTrue(saved.getIpHash().matches("[0-9a-f]{64}"));
    }

    @Test
    void sameIpGivesSameHashAndDifferentIpsDiffer() {
        assertEquals(service.hashIp("203.0.113.7"), service.hashIp("203.0.113.7"));
        assertNotEquals(service.hashIp("203.0.113.7"), service.hashIp("203.0.113.8"));
    }

    @Test
    void theSecretKeyChangesTheHash() {
        AuditService other = new AuditService(repo, tm, "another-key-another-key-another-1234");
        assertNotEquals(service.hashIp("203.0.113.7"), other.hashIp("203.0.113.7"));
    }

    @Test
    void missingIpMeansNoHash() {
        service.record(AuditAction.LOGIN_FAILED, null, null, null);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(repo).save(captor.capture());
        assertNull(captor.getValue().getIpHash());
    }

    @Test
    void aFailingDatabaseNeverBreaksTheCaller() {
        when(repo.save(any())).thenThrow(new RuntimeException("db down"));

        assertDoesNotThrow(() -> service.record(AuditAction.SCAN_CREATED, UUID.randomUUID(), "x", "1.2.3.4"));
    }

    @Test
    void recordAfterCommitWritesImmediatelyWhenThereIsNoTransaction() {
        service.recordAfterCommit(AuditAction.REGISTER, UUID.randomUUID(), null);

        verify(repo).save(any(AuditLog.class));
    }

    @Test
    void aShortKeyIsRejectedAtStartup() {
        assertThrows(IllegalStateException.class, () -> new AuditService(repo, tm, "too-short"));
    }
}