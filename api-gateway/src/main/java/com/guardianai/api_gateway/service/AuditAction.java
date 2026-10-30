package com.guardianai.api_gateway.service;

public enum AuditAction {
    REGISTER,
    LOGIN_SUCCESS,
    LOGIN_FAILED,
    SCAN_CREATED,
    SCAN_ACCESS_DENIED,
    DEVICE_REGISTERED,
    DEVICE_REVOKED,
    PROFILE_UPDATED,
    SETTINGS_UPDATED,
    RATE_LIMITED
}