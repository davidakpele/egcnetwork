package com.example.geofencing.Entities;

import java.time.LocalDateTime;
import java.util.UUID;

import com.example.geofencing.Enums.TrackingStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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
@Table(name = "tracking_sessions")
@Getter 
@Setter 
@NoArgsConstructor 
@AllArgsConstructor 
@Builder
public class TrackingSession {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
 
    @Column(name = "device_id", length = 100)
    private String deviceId;
 
    @Column(name = "started_at", nullable = false)
    @Builder.Default
    private LocalDateTime startedAt = LocalDateTime.now();
 
    @Column(name = "ended_at")
    private LocalDateTime endedAt;
 
    @Column(name = "total_distance_meters")
    @Builder.Default
    private Double totalDistanceMeters = 0.0;
 
    @Column(name = "update_count")
    @Builder.Default
    private Integer updateCount = 0;
 
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private TrackingStatus status = TrackingStatus.ACTIVE;
}