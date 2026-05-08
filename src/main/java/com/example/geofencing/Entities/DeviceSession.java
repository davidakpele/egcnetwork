package com.example.geofencing.Entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

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
@Table(name = "device_sessions")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class DeviceSession {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
 
    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;
 
    @Column(name = "device_name", length = 100)
    private String deviceName;
 
    @Column(name = "device_type", length = 50)
    private String deviceType;
 
    @Column(name = "os_version", length = 50)
    private String osVersion;
 
    @Column(name = "app_version", length = 20)
    private String appVersion;
 
    @Column(name = "push_token", length = 500)
    private String pushToken;
 
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
 
    @Column(name = "last_seen")
    private LocalDateTime lastSeen;
 
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}