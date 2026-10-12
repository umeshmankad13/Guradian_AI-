package com.guardianai.api_gateway.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_settings")
@Getter @Setter @NoArgsConstructor
public class UserSettings {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "setting_id")
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    @Column(name = "scan_on_browse", nullable = false)
    private boolean scanOnBrowse = true;

    @Column(name = "show_notifications", nullable = false)
    private boolean showNotifications = true;

    @Column(name = "risk_threshold", nullable = false)
    private int riskThreshold = 50;

    @Column(nullable = false)
    private String theme = "light";

    @Column(nullable = false)
    private String language = "en";

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = createdAt;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}