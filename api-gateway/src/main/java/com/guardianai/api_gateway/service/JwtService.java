package com.guardianai.api_gateway.service;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;


@Service 
public class JwtService {
    
    private final SecretKey key;
    private final long expiryMillis;

    public JwtService(@Value("${guardian.jwt.secret}") String secret,
    @Value("${guardian.jwt.expiry-minutes}") long expiryMinutes){
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expiryMillis = expiryMinutes * 60_000;
    }

    public String generateToken(UUID userId, String email, String role){
        long now = System.currentTimeMillis();
        return Jwts.builder()
            .subject(userId.toString())
            .claim("email",email)
            .claim("role",role)
            .issuedAt(new Date(now))
            .expiration(new Date(now + expiryMillis))
            .signWith(key)
            .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .getPayload();

    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch(JwtException | IllegalArgumentException e){
            return false;
        }
    }

    public UUID extractUserId(String token) {
        return UUID.fromString(parse(token).getSubject());
    }

    public String extractRole(String token) {
        return parse(token).get("role",String.class);
    }

    public long getExpirySeconds() {
        return expiryMillis / 1000;
    }
}
