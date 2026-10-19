package com.guardianai.api_gateway.repository;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.guardianai.api_gateway.entity.Scan;

public interface ScanRepository extends JpaRepository <Scan, UUID> {
    //we used entity graph so that while fetching the scan we will fetch teh url and domain which solves the N + 1 problem
    @EntityGraph(attributePaths = {"url", "domain"})
    Page<Scan> findByUserId(UUID userId, Pageable pageable);
}
