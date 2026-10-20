package com.guardianai.api_gateway.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.guardianai.api_gateway.dto.DeviceResponse;
import com.guardianai.api_gateway.dto.RegisterDeviceRequest;
import com.guardianai.api_gateway.service.DeviceService;

import jakarta.validation.Valid;

@RestController 
@RequestMapping ("/v1/devices")
public class DeviceController {
    
    private final DeviceService deviceService;

    public DeviceController(DeviceService deviceService) {
        this.deviceService = deviceService;
    }

    @PostMapping 
    public DeviceResponse register(@AuthenticationPrincipal UUID userId, @Valid @RequestBody RegisterDeviceRequest request) {

        return deviceService.register(userId, request);
    }


    @GetMapping 
    public List<DeviceResponse> list (@AuthenticationPrincipal UUID userId) {
        return deviceService.list(userId);
    }

    @DeleteMapping("/{id}") 
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void revoke(@AuthenticationPrincipal UUID userId, @PathVariable  UUID id) {
        deviceService.revoke(userId, id);
    }
}
