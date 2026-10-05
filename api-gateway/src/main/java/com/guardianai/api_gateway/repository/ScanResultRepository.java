package com.guardianai.api_gateway.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.guardianai.api_gateway.entity.ScanResult;

public interface ScanResultRepository extends JpaRepository<ScanResult, UUID> {
    Optional<ScanResult> findFirstByScanId(UUID scanId);
}
