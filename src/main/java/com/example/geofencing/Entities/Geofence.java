package com.example.geofencing.Entities;


import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;

import com.example.geofencing.Enums.GeofenceType;

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
@Table(name = "geofences")
@SQLDelete(sql = "UPDATE geofences SET deleted_at = NOW() WHERE id = ?")
@SQLRestriction("deleted_at IS NULL")
@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Geofence {
 
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
 
    @Column(nullable = false, length = 100)
    private String name;
 
    @Column(columnDefinition = "TEXT")
    private String description;
 
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;
 
    @Enumerated(EnumType.STRING)
    @Column(name = "geofence_type", nullable = false)
    @Builder.Default
    private GeofenceType geofenceType = GeofenceType.POLYGON;
 
    @Column(columnDefinition = "GEOMETRY(GEOMETRY, 4326)", nullable = false)
    private Geometry geometry;
 
    @Column(name = "center_point", columnDefinition = "GEOMETRY(POINT, 4326)")
    private Point centerPoint;
 
    @Column(name = "radius_meters")
    private Double radiusMeters;
 
    @Column(length = 7)
    @Builder.Default
    private String color = "#FF5733";
 
    @Column(name = "is_active")
    @Builder.Default
    private Boolean isActive = true;
 
    @Column(name = "is_global")
    @Builder.Default
    private Boolean isGlobal = false;
 
    @Column(name = "enter_alert")
    @Builder.Default
    private Boolean enterAlert = true;
 
    @Column(name = "exit_alert")
    @Builder.Default
    private Boolean exitAlert = true;
 
    @Column(name = "dwell_alert")
    @Builder.Default
    private Boolean dwellAlert = false;
 
    @Column(name = "dwell_time_seconds")
    @Builder.Default
    private Integer dwellTimeSeconds = 300;
 
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> metadata;
 
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
 
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}