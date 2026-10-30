package com.guardianai.api_gateway.controller;

import com.guardianai.api_gateway.dto.AuditItem;
import com.guardianai.api_gateway.dto.PageResponse;
import com.guardianai.api_gateway.service.AuditService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/v1/audit")
public class AuditController {

    private final AuditService auditService;

    public AuditController(AuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/me")
    public PageResponse<AuditItem> myActivity(@AuthenticationPrincipal UUID userId,
                                              @RequestParam(defaultValue = "0") int page,
                                              @RequestParam(defaultValue = "20") int size) {
        return auditService.myActivity(userId, page, size);
    }
}