package com.guardianai.api_gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @NotBlank (message = "name is required")
    @Size (max = 100, message = "name is too long")
    String name
) {
    
}
