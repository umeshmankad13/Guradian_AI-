package com.guardianai.api_gateway.controller;

import com.guardianai.api_gateway.dto.LoginRequest;
import com.guardianai.api_gateway.dto.LoginResponse;
import com.guardianai.api_gateway.dto.RegisterRequest;
import com.guardianai.api_gateway.dto.UserResponse;
import com.guardianai.api_gateway.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login (@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}