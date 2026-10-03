package com.guardianai.api_gateway.dto;

import java.time.Instant;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ScanRequest (
    
        @NotBlank(message="url is required")
        @Size(max = 2048, message = "url is too long")
        @Pattern(regexp = "^https?://.+", message = "url must start with http:// or https://")
        String url,

        Instant timestamp,

        @Valid TlsInfo tls,

        @Size(max = 20, message = "too many redirects")
        List<String> redirects,
    
        @Valid PageSignals page
    ){
        public record TlsInfo(boolean valid, String issuer, Instant expires) {}

        public record PageSignals(
            //@Min(0) count can't be negative
            @Min(0) int forms,
            @Min(0) int passwordFields,
            @Min(0) int externalScripts,
            @Min(0) int iframes,
            @Min(0) int externalLinks
        ) {}
    }

