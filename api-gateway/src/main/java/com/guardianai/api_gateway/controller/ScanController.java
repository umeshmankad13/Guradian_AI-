package com.guardianai.api_gateway.controller;

import java.util.UUID;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.guardianai.api_gateway.dto.ScanDetailResponse;
import com.guardianai.api_gateway.dto.ScanRequest;
import com.guardianai.api_gateway.dto.ScanResponse;
import com.guardianai.api_gateway.service.ScanService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping("/v1/scans")
public class ScanController {
    
    private final ScanService scanService;

    public ScanController(ScanService scanService) {
        this.scanService = scanService;
    }

    //Mappint the Dto from the controller to the Service Layer
    @PostMapping 
    public ScanResponse createScan(@Valid @RequestBody ScanRequest request) {
        return scanService.analyze(request);
    }

    @GetMapping("/{id}")
    public ScanDetailResponse getScan(@PathVariable UUID id){
        return scanService.getScan(id);
    }
}
