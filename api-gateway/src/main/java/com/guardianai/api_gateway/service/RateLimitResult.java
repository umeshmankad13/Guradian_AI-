package com.guardianai.api_gateway.service;

public record RateLimitResult(boolean allowed, int limit,long remaining, long retryAfterSeconds) {
    
}
