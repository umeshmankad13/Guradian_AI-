package com.guardianai.api_gateway.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity 
@Table (name = "urls")
@Getter @Setter @NoArgsConstructor 
public class Url {
    
    @Id 
    @GeneratedValue (strategy = GenerationType.UUID)
    @Column(name = "url_id")
    private UUID id;

    @Column (nullable = false, unique = true, columnDefinition = "TEXT")
    private String url;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "domain_id")
    private Domain domain;

    @Column (name = "is_ip", nullable = false)
    private boolean ip = false;

    @Column (name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column (name = "last_seen_at", nullable = false)
    private LocalDateTime lastSeenAt;

    @PrePersist 
    void onCreate() {
        createdAt = LocalDateTime.now();
        lastSeenAt = createdAt;
    }
}
