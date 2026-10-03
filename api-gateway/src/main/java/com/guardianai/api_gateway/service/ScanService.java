package com.guardianai.api_gateway.service;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.guardianai.api_gateway.dto.ScanRequest;
import com.guardianai.api_gateway.dto.ScanResponse;

@Service 
public class ScanService {
    public ScanResponse analyze(ScanRequest request)
    {
        String domain = extractDomain(request.url());

        //TODO: replace with the Analysis Orchestrator call
        return new ScanResponse(UUID.randomUUID(),
                domain,
                "LOW",
                82,
                0.90,
                List.of("Mock result: real analysis not connected yet"),
                "This is a placeholder verdict for " + domain + ".",
                List.of("No action needed"),
                Instant.now());
    }

    private String extractDomain(String url){
        try {
            String host = URI.create(url).getHost();
            return host != null ? host : "unknown";
        } catch (IllegalArgumentException e) {
            return "unknown";
        }
    }
}
