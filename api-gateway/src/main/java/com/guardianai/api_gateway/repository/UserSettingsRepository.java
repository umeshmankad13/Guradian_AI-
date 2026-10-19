package com.guardianai.api_gateway.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.guardianai.api_gateway.entity.UserSettings;

public interface UserSettingsRepository extends JpaRepository<UserSettings, UUID>{
    
    Optional<UserSettings> findByUserId(UUID userId);
}
