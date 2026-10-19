package com.guardianai.api_gateway.controller;

import com.guardianai.api_gateway.dto.ProfileResponse;
import com.guardianai.api_gateway.dto.SettingsResponse;
import com.guardianai.api_gateway.dto.UpdateProfileRequest;
import com.guardianai.api_gateway.dto.UpdateSettingsRequest;
import com.guardianai.api_gateway.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/users/me")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ProfileResponse getProfile(@AuthenticationPrincipal UUID userId) {
        return userService.getProfile(userId);
    }

    @PutMapping
    public ProfileResponse updateProfile(@AuthenticationPrincipal UUID userId,
                                         @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId, request);
    }

    @GetMapping("/settings")
    public SettingsResponse getSettings(@AuthenticationPrincipal UUID userId) {
        return userService.getSettings(userId);
    }

    @PutMapping("/settings")
    public SettingsResponse updateSettings(@AuthenticationPrincipal UUID userId,
                                           @Valid @RequestBody UpdateSettingsRequest request) {
        return userService.updateSettings(userId, request);
    }
}