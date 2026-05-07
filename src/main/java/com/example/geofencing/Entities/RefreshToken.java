package com.example.geofencing.Entities;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
 
@Entity
@Table(name = "refresh_tokens")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RefreshToken {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
 
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String token;
 
    @Column(name = "device_id")
    private String deviceId;
 
    @Column(name = "ip_address")
    private String ipAddress;
 
    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;
 
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;
 
    @Column(nullable = false)
    @Builder.Default
    private boolean revoked = false;
 
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
 
    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
 
    public boolean isValid() {
        return !revoked && !isExpired();
    }
}