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
@Table (name = "scan_indicators")
@Getter @Setter @NoArgsConstructor 
public class ScanIndicator {
    
    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "indicator_id")
    private UUID id;

    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "scan_id")
    private Scan scan;

    @Column (name = "indicator_type", nullable = false)
    private String indicatorType;

    @Column (name = "indicator_name", nullable = false)
    private String indicatorName;

    @Column (columnDefinition = "TEXT")
    private String value;

    @Column (name = "is_malicious", nullable = false)
    private boolean malicious = false;

    @Column (name = "confidence_score")
    private Integer confidenceScore;

    @Column (name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist 
    void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
