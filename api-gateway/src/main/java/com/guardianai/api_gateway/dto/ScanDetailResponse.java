package com.guardianai.api_gateway.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ScanDetailResponse(
        UUID scanId,
        String url,
        String domain,
        String status,
        Integer securityScore,
        String riskLevel,
        BigDecimal confidence,
        String summary,
        List<String> recommendations,
        List<ScanDetailResponse.IndicatorDto> indicators,
        List<ScanDetailResponse.ElementDto> pageElements,
        Instant createdAt,
        Instant completedAt
) {
    public record IndicatorDto(String type, String name, String value, boolean malicious) {}

    public record ElementDto(String type, String value, boolean suspicious, String reason) {}
}