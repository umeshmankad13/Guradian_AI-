package com.guardianai.api_gateway.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record UpdateSettingsRequest(
    @NotNull (message = "scanOnBrowse is required")
    Boolean scanOnBrowse,

    @NotNull (message = "showNotifications is required")
    Boolean showNotifications,

    @NotNull (message = "riskThreshold is required")
    @Min (value = 0, message = "riskThreshold must be 0 to 100")
    @Max (value = 100, message = "riskThreshold must be 0 to 100")
    Integer riskThreshold,

    @NotNull(message = "theme is required")
        @Pattern(regexp = "^(light|dark|system)$", message = "theme must be light, dark or system")
        String theme,

    @NotNull(message = "language is required")
        @Pattern(regexp = "^[a-z]{2}(-[A-Z]{2})?$", message = "language must look like 'en' or 'en-IN'")
        String language
) {
    
}
