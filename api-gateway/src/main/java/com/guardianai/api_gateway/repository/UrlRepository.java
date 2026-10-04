package com.guardianai.api_gateway.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.guardianai.api_gateway.entity.Url;

public interface UrlRepository extends JpaRepository<Url, UUID> {
    Optional<Url> findByUrl(String url);
    
}
