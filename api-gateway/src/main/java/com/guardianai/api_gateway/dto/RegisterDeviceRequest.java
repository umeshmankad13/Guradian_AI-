package com.guardianai.api_gateway.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterDeviceRequest(
    @NotBlank (message = "browser is required")
    @Size (max = 50, message = "browser is too long")
    String browser,

    @NotBlank (message = "extVersion is required")
    @Size (max = 20, message = "extVersion is too long")
    String extVersion
) {
    
}
