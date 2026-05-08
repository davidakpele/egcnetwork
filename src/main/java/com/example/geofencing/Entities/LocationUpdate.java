package com.example.geofencing.Entities;

import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.locationtech.jts.geom.Point;

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
@Table(name = "location_updates")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class LocationUpdate {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
 
    @Column(columnDefinition = "GEOMETRY(POINT, 4326)", nullable = false)
    private Point location;
 
    @Column(nullable = false)
    private Double latitude;
 
    @Column(nullable = false)
    private Double longitude;
 
    private Double altitude;
    private Double accuracy;
    private Double speed;
    private Double heading;
 
    @Column(name = "battery_level")
    private Integer batteryLevel;
 
    @Column(name = "device_id", length = 100)
    private String deviceId;
 
    @Column(length = 50)
    private String provider;
 
    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
 
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}