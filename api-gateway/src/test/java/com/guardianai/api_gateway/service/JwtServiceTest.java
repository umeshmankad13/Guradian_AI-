package com.guardianai.api_gateway.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET = "test-secret-key-test-secret-key-12345";
    private final JwtService jwt = new JwtService(SECRET, 15);

    @Test
    void validTokenIsAcceptedAndContainsUserIdAndRole() {
        UUID id = UUID.randomUUID();
        String token = jwt.generateToken(id, "a@b.com", "USER");

        assertTrue(jwt.isValid(token));
        assertEquals(id, jwt.extractUserId(token));
        assertEquals("USER", jwt.extractRole(token));
    }

    @Test
    void tamperedTokenIsRejected() {
        String token = jwt.generateToken(UUID.randomUUID(), "a@b.com", "USER");
        String tampered = token.substring(0, token.length() - 5) + "XXXXX";

        assertFalse(jwt.isValid(tampered));
    }

    @Test
    void expiredTokenIsRejected() {
        JwtService expired = new JwtService(SECRET, -1);
        String token = expired.generateToken(UUID.randomUUID(), "a@b.com", "USER");

        assertFalse(jwt.isValid(token));
    }

    @Test
    void tokenSignedWithAnotherSecretIsRejected() {
        JwtService other = new JwtService("another-secret-another-secret-123456", 15);
        String token = other.generateToken(UUID.randomUUID(), "a@b.com", "USER");

        assertFalse(jwt.isValid(token));
    }

    @Test
    void garbageIsRejected() {
        assertFalse(jwt.isValid("not.a.token"));
        assertFalse(jwt.isValid(""));
    }
}