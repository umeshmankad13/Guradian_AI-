package com.guardianai.api_gateway.dto;

import java.time.Instant;
import java.util.UUID;

public record ProfileResponse(
    UUID id,
    String name,
    String email,
    String role,
    Instant createdAt
) {
    
}
