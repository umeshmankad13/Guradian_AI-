package com.guardianai.api_gateway.dto;

import java.util.UUID;

public record UserResponse(
        UUID id,
        String name,
        String email,
        String role
) {}