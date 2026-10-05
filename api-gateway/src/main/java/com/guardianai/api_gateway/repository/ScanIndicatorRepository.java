package com.guardianai.api_gateway.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.guardianai.api_gateway.entity.ScanIndicator;

public interface ScanIndicatorRepository extends JpaRepository<ScanIndicator, UUID>{
    List <ScanIndicator> findByScan(UUID scanId);
    
}
