package com.guardianai.api_gateway.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table(name = "domains")
@Getter @Setter @NoArgsConstructor 
public class Domain {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name ="domain_id")
    private UUID id;

    @Column (nullable = false, unique = true)
    private String domain;

    @Column (name ="registrar")
    private String register;

    @Column(name="is_suspicious", nullable = false)
    private boolean suspicious = false;

    @Column (name="created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column (name="updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist //just before the entity is persisted, set the createdAt and updatedAt fields to the current time
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate //just before the entity is updated, set the updatedAt field to the current time
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

}
