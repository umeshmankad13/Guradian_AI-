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
@Table (name = "page_elements")
@Getter @Setter @NoArgsConstructor 
public class PageElement {

    @Id 
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column (name = "element_id")
    private UUID id;


    @ManyToOne (fetch = FetchType.LAZY, optional = false)
    @JoinColumn (name = "scan_id")
    private Scan scan;

    @Column (name = "element_type", nullable = false)
    private String elementType;
    
    @Column (name = "element_value", columnDefinition = "TEXT")
    private String elementValue;

    @Column (name = "is_suspicious", nullable = false)
    private boolean suspicious = false;

    @Column (columnDefinition = "TEXT")
    private String reason;

    @Column (name = "created_at" , nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist 
    void onCreate() {
        createdAt = LocalDateTime.now();
    }

}
