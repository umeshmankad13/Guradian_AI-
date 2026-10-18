package com.guardianai.api_gateway.dto;

import java.time.Instant;
import java.util.UUID;

public record HistoryItem(
    UUID scanId,
    String url,
    String domain,
    String status,
    Integer securityScore,
    String riskLevel,
    Instant createdAt
) {
    
}
