package com.guardianai.api_gateway.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ScanResponse(
    UUID scanId,
    String domain,
    String riskLevel,
    int securityScore,
    double confidence,
    List<String> findings,
    String explanation,
    List<String> recommendations,
    Instant analyzedAt
) {
    
}
