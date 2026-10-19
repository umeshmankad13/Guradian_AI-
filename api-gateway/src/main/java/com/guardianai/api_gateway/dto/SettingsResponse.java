package com.guardianai.api_gateway.dto;

public record SettingsResponse(
    boolean scanOnBrowse,
    boolean showNotifications,
    int riskThreshold,
    String theme,
    String language
) {
    
}
