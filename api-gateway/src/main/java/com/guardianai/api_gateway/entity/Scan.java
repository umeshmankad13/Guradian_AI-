package com.guardianai.api_gateway.entity;

import java.math.BigDecimal;
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
@Table (name = "scans")
@Getter @Setter @NoArgsConstructor 
public class Scan {

    @Id 
    @GeneratedValue (strategy = GenerationType.UUID)
    @Column (name = "scan_id")
    private UUID id;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "url_id")
    private Url url;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "domain_id")
    private Domain domain;

    @Column (nullable = false)
    private String status = "PENDING";

    @Column (name = "risk_score")
    private Integer riskScore;

    @Column (name = "risk_level")
    private String riskLevel;

    @Column (columnDefinition = "TEXT")
    private String summary;

    @Column (precision = 3, scale = 2)
    private BigDecimal confidence;

    @Column (name = " created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column (name = "completed_at")
    private LocalDateTime completedAt;

    @PrePersist 
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
}
