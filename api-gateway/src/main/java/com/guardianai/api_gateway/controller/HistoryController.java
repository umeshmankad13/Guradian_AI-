package com.guardianai.api_gateway.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.guardianai.api_gateway.dto.HistoryItem;
import com.guardianai.api_gateway.dto.PageResponse;
import com.guardianai.api_gateway.service.ScanService;

@RestController 
@RequestMapping("/v1/history")
public class HistoryController {
    private final ScanService scanService;

    public HistoryController(ScanService scanService) {
        this.scanService = scanService;
    }

    @GetMapping 
    public PageResponse<HistoryItem> history(@AuthenticationPrincipal UUID userId,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam (defaultValue = "20") int size
    ) {
        return scanService.getHistory(userId, page, size);
    }
}
