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
@Table(name = "scan_results")
@Getter @Setter @NoArgsConstructor 
public class ScanResult {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "result_id")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "scan_id")
    private Scan scan;

    @Column (name = "analysis_type", nullable = false)
    private String analysisType;

    @Column (name = "risk_score")
    private Integer riskScore;

    @Column (name = "risk_level")
    private String riskLevel;

    @Column (columnDefinition = "TEXT")
    private String explanation;

    @Column (columnDefinition = "TEXT")
    private String recommendation;

    @Column (name = "model_used")
    private String modelUsed;

    @Column (name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist 
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
