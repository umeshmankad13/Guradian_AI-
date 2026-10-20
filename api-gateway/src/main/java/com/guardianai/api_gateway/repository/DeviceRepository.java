package com.guardianai.api_gateway.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.guardianai.api_gateway.entity.Device;

public interface DeviceRepository extends JpaRepository<Device, UUID> {
    List<Device> findByUserIdOrderByCreatedAtDesc(UUID userId);

    Optional<Device> findByIdAndUserId(UUID id, UUID userID);

    long countByUserIdAndRevokedFalse(UUID userId);

    
} 
