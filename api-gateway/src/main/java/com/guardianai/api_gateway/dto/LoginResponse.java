package com.guardianai.api_gateway.dto;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresInSeconds,
    UserResponse user
) {
    
}
