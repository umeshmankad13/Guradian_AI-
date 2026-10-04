package com.guardianai.api_gateway.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.guardianai.api_gateway.entity.Domain;

public interface DomainRepository extends JpaRepository<Domain, UUID> {
    Optional<Domain> findByDomain(String domain);

    
} 
