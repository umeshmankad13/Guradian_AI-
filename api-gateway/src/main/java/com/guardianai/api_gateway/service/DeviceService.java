package com.guardianai.api_gateway.service;

import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.guardianai.api_gateway.dto.DeviceResponse;
import com.guardianai.api_gateway.dto.RegisterDeviceRequest;
import com.guardianai.api_gateway.entity.Device;
import com.guardianai.api_gateway.repository.DeviceRepository;
import com.guardianai.api_gateway.repository.UserRepository;

import jakarta.transaction.Transactional;

@Service 
public class DeviceService {
    
    private static final int MAX_ACTIVE_DEVICES = 10;

    private final DeviceRepository deviceRepository;

    private final UserRepository userRepository;

    public DeviceService (DeviceRepository deviceRepository, UserRepository userRepository) {
        this.deviceRepository= deviceRepository;
        this.userRepository = userRepository;
    }

    @Transactional 
    public DeviceResponse register (UUID userId, RegisterDeviceRequest request) {
        if(deviceRepository.countByUserIdAndRevokedFalse(userId) >= MAX_ACTIVE_DEVICES) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Device limit reached . Revoke an old device first.");
        }

        Device device = new Device();
        device.setUser(userRepository.getReferenceById(userId));
        device.setBrowser(request.browser().trim());
        device.setExtVersion(request.extVersion().trim());
        return toResponse(deviceRepository.save(device));
    }

    @Transactional 
    public List<DeviceResponse> list(UUID userId) {
        return deviceRepository.findByUserIdOrderByCreatedAtDesc(userId).stream().map(this::toResponse).toList();
    }

    @Transactional 
    public void revoke(UUID userId, UUID deviceId) {
        //another user's device looks exactly like a missing one
        Device device = deviceRepository.findByIdAndUserId(deviceId, userId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Device not found"));
        device.setRevoked(true); //soft delete: scan history keeps its device link
    }

        private DeviceResponse toResponse(Device d) {
            return new DeviceResponse(d.getId(), d.getBrowser(), d.getExtVersion(), d.isRevoked(), d.getCreatedAt().toInstant(ZoneOffset.UTC),
            d.getLastSeenAt().toInstant(ZoneOffset.UTC));
        }
    


}
