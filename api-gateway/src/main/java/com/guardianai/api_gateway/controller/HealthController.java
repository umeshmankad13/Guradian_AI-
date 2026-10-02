package com.guardianai.api_gateway.controller;

import java.time.Instant;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guardianai.api_gateway.dto.HealthResponse;

@RestController 
@RequestMapping("/v1")
public class HealthController {
    
    @GetMapping("/health")
    public HealthResponse health()
    {
        return new HealthResponse(
        "UP",
        "guardianai-api-gateway",
        "0.0.1",
        Instant.now()
    );
    }
}
