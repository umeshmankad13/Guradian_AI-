package com.guardianai.api_gateway.dto;

import java.time.Instant;
import java.util.UUID;

public record DeviceResponse(
    UUID id,
    String browser,
    String extVersion,
    boolean revoked,
    Instant createdAt,
    Instant lastSeenAt
) {
    
}
