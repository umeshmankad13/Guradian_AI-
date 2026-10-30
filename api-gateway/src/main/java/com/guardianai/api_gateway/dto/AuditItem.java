package com.guardianai.api_gateway.dto;

import java.time.Instant;

public record AuditItem(String action, String resource, Instant createdAt) {}