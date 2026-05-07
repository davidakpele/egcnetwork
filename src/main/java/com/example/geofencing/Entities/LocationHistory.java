package com.example.geofencing.Entities;

import java.time.Instant;

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
@Table(name = "location_history")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LocationHistory {
 
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
 
    @Column(columnDefinition = "GEOMETRY(Point, 4326)", nullable = false)
    private Point location;
 
    private Double accuracy;
    private Double speed;
    private Double bearing;
    private Double altitude;
 
    @Column(name = "device_id")
    private String deviceId;
 
    @Column(name = "session_id")
    private String sessionId;
 
    @Column(name = "recorded_at", nullable = false)
    @Builder.Default
    private Instant recordedAt = Instant.now();
}